package database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Repo {
    private int id;
    private String name;      // varchar(64)
    private int owner;        // 用户ID
    private boolean isPublic;
    private String commitId;  // char(64) 指向当前最新的commit
}