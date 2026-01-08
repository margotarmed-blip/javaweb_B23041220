package base;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Tool {
    static final String DB_URL = "jdbc:mysql://localhost:3306/code?serverTimezone=GMT%2B8&useSSL=false&allowPublicKeyRetrieval=true";
    static final String USER = "root";
    static final String PASS = "123456";
    static final String DRIVER = "com.mysql.cj.jdbc.Driver";
    static {
        try {
            Class.forName(DRIVER);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL, USER, PASS);
    }
}