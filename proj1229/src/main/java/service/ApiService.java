package service;

import events.*;
import base.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import database.User;

import java.util.Map;

public class ApiService implements EventListener {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Subscribe
    public void onEvent(ApiEvent event) {
        try {
            RequestBody body = event.getBody();
            Map<String, Object> map = objectMapper.readValue(body.getBody(), new TypeReference<>() {
            });
            if (body.getType() == ApiType.Login) {
                LoginEvent le = new LoginEvent((String) map.get("name"), (String) map.get("pwdHash"), null);
                EventBus.publish(le);
                event.setResp(le.getResp());
                return;
            }
            if (body.getType() == ApiType.Register) {
                RegisterEvent re = new RegisterEvent((String) map.get("name"), (String) map.get("pwdHash"), null);
                EventBus.publish(re);
                event.setResp(re.getResp());
                return;
            }
            String sid = (String) map.get("sessionId");
            User user = SessionManager.getUser(sid != null ? sid : "");
            if (user == null) {
                event.setResp("{\"error\":\"Session expired\"}");
                return;
            }

            switch (body.getType()) {
                case CreateRepo:
                case GetRepo:
                case DelRepo:
                case SearchRepo:
                case UpdateRepo:
                case Fork:
                    RepoEvent re = new RepoEvent(body.getType(), user.getId(), map, null);
                    EventBus.publish(re);
                    event.setResp(re.getResp());
                    break;
                case Commit:
                case GetLatestFiles:
                case GetCommitHistory:
                    CommitEvent ce = new CommitEvent(body.getType(), user.getId(), map, null);
                    EventBus.publish(ce);
                    event.setResp(ce.getResp());
                    break;
                case UploadBlob:
                case GetBlob:
                    BlobEvent be = new BlobEvent(body.getType(), user.getId(), map, null);
                    EventBus.publish(be);
                    event.setResp(be.getResp());
                    break;
                case GetAllUser:
                case UpdateUserPermission:
                case DeleteUser:
                    UserAdminEvent uae = new UserAdminEvent(body.getType(), user.getId(), map, null);
                    EventBus.publish(uae);
                    event.setResp(uae.getResp());
                    break;
            }
        } catch (Exception e) {
            event.setResp("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}