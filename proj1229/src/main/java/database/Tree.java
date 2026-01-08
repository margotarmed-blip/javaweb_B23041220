package database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tree {
    private String id;       //char(64)
    private String treeBlob; //longtext (存储文件路径列表)
}