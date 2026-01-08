package database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.sql.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Commit {
    private String id;        // char(64)
    private String treeId;    // char(64)
    private String parentId;  // char(64)
    private int ownerId;
    private Timestamp time;
    private String message;
}