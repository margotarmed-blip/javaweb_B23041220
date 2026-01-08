package service;

import base.EventListener;
import base.Subscribe;
import com.fasterxml.jackson.databind.ObjectMapper;
import database.Commit;
import database.DbOps;
import events.ApiType;
import events.CommitEvent;
import org.apache.commons.codec.digest.DigestUtils;

import java.util.List;

public class CommitService implements EventListener {

    @Subscribe
    public void onCommitEvent(CommitEvent ev) {
        try {
            if (ev.getType() == ApiType.Commit) {
                int repoId = ((Number) ev.getParams().get("repoId")).intValue();
                int userId = ev.getUserId();
                String message = (String) ev.getParams().get("message");
                String treeStr = (String) ev.getParams().get("tree");
                String treeId = DigestUtils.sha256Hex(treeStr);
                String commitId = DigestUtils.sha256Hex(treeId + System.currentTimeMillis());
                boolean success = DbOps.commitVersion(repoId, userId, message, treeId, treeStr, commitId);
                if (success) {
                    ev.setResp("{\"success\":true, \"commitId\":\"" + commitId + "\"}");
                } else {
                    ev.setResp("{\"error\":\"Commit failed: Permission denied or repository not found\"}");
                }
            } else if (ev.getType() == ApiType.GetLatestFiles) {
                int repoId = ((Number) ev.getParams().get("repoId")).intValue();
                int userId = ev.getUserId();
                String tree = DbOps.getLatestTree(repoId, userId);
                if (tree == null || tree.isEmpty()) {
                    ev.setResp("{\"tree\":\"\", \"message\":\"No commits found or access denied\"}");
                } else {
                    ev.setResp("{\"tree\":\"" + tree.replace("\n", "\\n") + "\"}");
                }
            } else if (ev.getType() == ApiType.GetCommitHistory) { // 新增
                int repoId = ((Number) ev.getParams().get("repoId")).intValue();
                String latestId = DbOps.getCommitId(repoId);
                if (latestId != null && !latestId.isEmpty()) {
                    List<Commit> history = DbOps.getCommitHistory(latestId);
                    ev.setResp(new ObjectMapper().writeValueAsString(history));
                } else {
                    ev.setResp("[]");
                }
            }
        } catch (Exception e) {
            ev.setResp("{\"error\":\"CommitService error: " + e.getMessage() + "\"}");
        }
    }
}