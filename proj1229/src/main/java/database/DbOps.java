package database;

import base.Tool;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DbOps {
    public static List<Repo> searchRepos(String keyword, boolean isAdmin) throws SQLException {
        List<Repo> repos = new ArrayList<>();
        String sql = isAdmin
                ? "SELECT id, name, owner, is_public, commit_id FROM t_repo WHERE name LIKE ?"
                : "SELECT id, name, owner, is_public, commit_id FROM t_repo WHERE is_public = 1 AND name LIKE ?";

        try (Connection conn = Tool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                repos.add(new Repo(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getInt("owner"),
                        rs.getBoolean("is_public"),
                        rs.getString("commit_id")
                ));
            }
        }
        return repos;
    }

    public static void adminDeleteRepo(int repoId) throws SQLException {
        String sql = "DELETE FROM t_repo WHERE id = ?";
        try (Connection conn = Tool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, repoId);
            ps.executeUpdate();
        }
    }

    public static void updateRepoVisibility(int repoId, int userId, boolean isPublic) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_update_repo_visibility(?, ?, ?)}");
            cs.setInt(1, repoId);
            cs.setInt(2, userId);
            cs.setBoolean(3, isPublic);
            cs.execute();
        }
    }

    public static void updateUserPermission(int targetUserId, int newPermission) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_update_user_permission(?, ?)}");
            cs.setInt(1, targetUserId);
            cs.setInt(2, newPermission);
            cs.execute();
        }
    }

    public static int getUserPermission(int userId) throws SQLException {
        String sql = "SELECT permission FROM t_user WHERE id = ?";
        try (Connection conn = Tool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt("permission");
            }
            return 0;
        }
    }

    public static void deleteUser(int targetUserId) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_delete_user(?)}");
            cs.setInt(1, targetUserId);
            cs.execute();
        }
    }

    public static int registerUser(String name, String pwdHash) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_register_user(?, ?, ?)}");
            cs.setString(1, name);
            cs.setString(2, pwdHash);
            cs.registerOutParameter(3, Types.INTEGER);
            cs.execute();
            return cs.getInt(3);
        }
    }

    public static User loginUser(String name, String pwdHash) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_login_user(?, ?)}");
            cs.setString(1, name);
            cs.setString(2, pwdHash);
            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return new User(rs.getInt("id"), rs.getString("name"), rs.getString("pwd_hash"), rs.getInt("permission"));
            }
            return null;
        }
    }

    public static void createRepo(String name, int userId, boolean isPublic) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_create_repo(?, ?, ?)}");
            cs.setString(1, name);
            cs.setInt(2, userId);
            cs.setBoolean(3, isPublic);
            cs.execute();
        }
    }

    public static List<Repo> getRepos(int userId) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_get_repos(?)}");
            cs.setInt(1, userId);
            ResultSet rs = cs.executeQuery();
            List<Repo> repos = new ArrayList<>();
            while (rs.next()) {
                repos.add(new Repo(
                        rs.getInt("id"),
                        rs.getString("name"),
                        userId,
                        rs.getBoolean("is_public"),
                        rs.getString("commit_id")
                ));
            }
            return repos;
        }
    }

    public static void uploadBlob(String hash, String filename, byte[] content) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_upload_blob(?, ?, ?)}");
            cs.setString(1, hash);
            cs.setString(2, filename);
            cs.setBytes(3, content);
            cs.execute();
        }
    }

    public static byte[] getBlobContent(String filename, String hash) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_get_blob(?, ?)}");
            cs.setString(1, filename);
            cs.setString(2, hash);
            ResultSet rs = cs.executeQuery();
            if (rs.next()) {
                return rs.getBytes("content");
            }
            return null;
        }
    }

    public static boolean commitVersion(int repoId, int userId, String message, String treeId, String treeStr, String commitId) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_commit_version(?, ?, ?, ?, ?, ?, ?)}");
            cs.setInt(1, repoId);
            cs.setInt(2, userId);
            cs.setString(3, message);
            cs.setString(4, treeId);
            cs.setString(5, treeStr);
            cs.setString(6, commitId);
            cs.registerOutParameter(7, Types.BOOLEAN);
            cs.execute();
            return cs.getBoolean(7);
        }
    }

    public static String getLatestTree(int repoId, int userId) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_get_latest_tree(?, ?)}");
            cs.setInt(1, repoId);
            cs.setInt(2, userId);
            ResultSet rs = cs.executeQuery();
            return rs.next() ? rs.getString("tree_blob") : "";
        }
    }

    public static void deleteRepo(int repoId, int userId) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_delete_repo(?, ?)}");
            cs.setInt(1, repoId);
            cs.setInt(2, userId);
            cs.execute();
        }
    }

    public static List<User> getAllUsers() throws SQLException {
        List<User> users = new ArrayList<>();
        String sql = "SELECT id, name, pwd_hash, permission FROM t_user";
        try (Connection conn = Tool.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                users.add(new User(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("pwd_hash"),
                        rs.getInt("permission")
                ));
            }
        }
        return users;
    }

    public static void forkRepo(int repoId, int userId) throws SQLException {
        try (Connection conn = Tool.getConnection()) {
            CallableStatement cs = conn.prepareCall("{call sp_fork_repo(?, ?)}");
            cs.setInt(1, repoId);
            cs.setInt(2, userId);
            cs.execute();
        }
    }

    public static String getCommitId(int repoId) {
        try (java.sql.Connection conn = base.Tool.getConnection()) {
            var ps = conn.prepareStatement("SELECT commit_id FROM t_repo WHERE id = ?");
            ps.setInt(1, repoId);
            var rs = ps.executeQuery();
            if (rs.next()) return rs.getString("commit_id");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return "";
    }

    public static List<Commit> getCommitHistory(String latestCommitId) throws SQLException {
        List<Commit> history = new ArrayList<>();
        String currentId = latestCommitId;
        try (Connection conn = Tool.getConnection()) {
            while (currentId != null && !currentId.isEmpty()) {
                String sql = "SELECT id, tree_id, parent_id, owner_id, time, message FROM t_commit WHERE id = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, currentId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        Commit c = new Commit(
                                rs.getString("id"),
                                rs.getString("tree_id"),
                                rs.getString("parent_id"),
                                rs.getInt("owner_id"),
                                rs.getTimestamp("time"),
                                rs.getString("message")
                        );
                        history.add(c);
                        currentId = c.getParentId();
                    } else {
                        break;
                    }
                }
            }
        }
        return history;
    }
}