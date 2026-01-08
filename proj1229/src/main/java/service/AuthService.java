package service;

import base.EventListener;
import base.SessionManager;
import base.Subscribe;
import com.fasterxml.jackson.databind.ObjectMapper;
import database.DbOps;
import database.User;
import events.ApiType;
import events.LoginEvent;
import events.RegisterEvent;
import events.UserAdminEvent;

import java.sql.*;
import java.util.List;

public class AuthService implements EventListener {
    private final ObjectMapper mapper = new ObjectMapper();

    @Subscribe
    public void onRegister(RegisterEvent ev) {
        try {
            int result = DbOps.registerUser(ev.getName(), ev.getPwdHash());
            ev.setResp(result == 0 ? "{\"success\":true}" : "{\"error\":\"User already exists\"}");
        } catch (SQLException e) {
            ev.setResp("{\"error\":\"Database error: " + e.getMessage() + "\"}");
        }
    }

    @Subscribe
    public void onLogin(LoginEvent ev) {
        try {
            User user = DbOps.loginUser(ev.getName(), ev.getPwdHash());
            if (user != null) {
                String sessionId = SessionManager.createSession(user);
                ev.setResp("{\"success\":true, \"sessionId\":\"" + sessionId + "\", \"userId\":" + user.getId() + ", \"permission\":" + user.getPermission() + "}");
            } else {
                ev.setResp("{\"error\":\"Invalid name or password\"}");
            }
        } catch (SQLException e) {
            ev.setResp("{\"error\":\"Database error\"}");
        }
    }

    @Subscribe
    public void onAdminEvent(UserAdminEvent ev) {
        try {
            if (DbOps.getUserPermission(ev.getOperatorId()) <= 0) {
                ev.setResp("{\"error\":\"Permission denied.\"}");
                return;
            }
            if (ev.getType() == ApiType.GetAllUser) {
                List<User> users = DbOps.getAllUsers();
                ev.setResp(mapper.writeValueAsString(users));
            } else if (ev.getType() == ApiType.UpdateUserPermission) {
                int targetId = ((Number) ev.getParams().get("targetUserId")).intValue();
                int newPerm = ((Number) ev.getParams().get("newPermission")).intValue();
                DbOps.updateUserPermission(targetId, newPerm);
                ev.setResp("{\"success\":true}");
            } else if (ev.getType() == ApiType.DeleteUser) {
                int targetId = ((Number) ev.getParams().get("targetUserId")).intValue();
                DbOps.deleteUser(targetId);
                ev.setResp("{\"success\":true}");
            }
        } catch (Exception e) {
            ev.setResp("{\"error\":\"AdminService error: " + e.getMessage() + "\"}");
        }
    }
}