package service;

import base.EventListener;
import base.Subscribe;
import com.fasterxml.jackson.databind.ObjectMapper;
import database.DbOps;
import database.Repo;
import events.ApiType;
import events.RepoEvent;

import java.sql.SQLException;
import java.util.*;

public class RepoService implements EventListener {
    private final ObjectMapper mapper = new ObjectMapper();

    public boolean isAdmin(RepoEvent ev) throws SQLException {
        int currentUserPermission = DbOps.getUserPermission(ev.getUserId());
        return currentUserPermission > 0;
    }

    @Subscribe
    public void onRepoEvent(RepoEvent ev) {
        try {
            switch (ev.getType()) {
                case CreateRepo:
                    DbOps.createRepo((String) ev.getParams().get("name"), ev.getUserId(), (boolean) ev.getParams().get("isPublic"));
                    ev.setResp("{\"success\":true}");
                    break;
                case GetRepo:
                    List<Repo> repos = DbOps.getRepos(ev.getUserId());
                    ev.setResp(mapper.writeValueAsString(repos));
                    break;
                case DelRepo:
                    Object repoIdObj = ev.getParams().get("repoId");
                    if (repoIdObj != null) {
                        int repoId = ((Number) repoIdObj).intValue();
                        if (isAdmin(ev)) {
                            DbOps.adminDeleteRepo(repoId);
                        } else {
                            DbOps.deleteRepo(repoId, ev.getUserId());
                        }
                        ev.setResp("{\"success\":true}");
                    } else {
                        ev.setResp("{\"error\":\"Missing repoId\"}");
                    }
                    break;
                case Fork:
                    Object repoObj = ev.getParams().get("repoId");
                    if (repoObj != null) {
                        int repoId = ((Number) repoObj).intValue();
                        DbOps.forkRepo(repoId, ev.getUserId());
                        ev.setResp("{\"success\":true}");
                    } else {
                        ev.setResp("{\"error\":\"Missing repoId\"}");
                    }
                    break;
                case SearchRepo:
                    String keyword = (String) ev.getParams().get("keyword");
                    if (keyword == null) keyword = "";
                    List<Repo> publicRepos = DbOps.searchRepos(keyword, isAdmin(ev));
                    ev.setResp(mapper.writeValueAsString(publicRepos));
                    break;
                case UpdateRepo:
                    Object uRepoIdObj = ev.getParams().get("repoId");
                    Object uIsPublicObj = ev.getParams().get("isPublic");
                    if (uRepoIdObj != null && uIsPublicObj != null) {
                        int repoId = ((Number) uRepoIdObj).intValue();
                        boolean isPublic = (boolean) uIsPublicObj;
                        DbOps.updateRepoVisibility(repoId, ev.getUserId(), isPublic);
                        ev.setResp("{\"success\":true}");
                    } else {
                        ev.setResp("{\"error\":\"Missing parameters\"}");
                    }
                    break;
            }
        } catch (Exception e) {
            ev.setResp("{\"error\":\"RepoService error: " + e.getMessage() + "\"}");
        }
    }
}