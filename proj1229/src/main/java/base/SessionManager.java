package base;
import database.User;
import java.util.UUID;

public class SessionManager {
    private static final TimeMap<String, User> sessionMap = new TimeMap<>(10 * 60 * 1000L);//过期时间10分钟

    public static String createSession(User user) {
        String sessionId = UUID.randomUUID().toString();
        sessionMap.set(sessionId, user);
        return sessionId;
    }

    public static User getUser(String sessionId) {
        return sessionMap.get(sessionId);
    }
}