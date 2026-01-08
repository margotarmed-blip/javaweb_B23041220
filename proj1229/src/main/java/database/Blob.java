package database;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blob {
   private String id;      // char(64)
   private byte[] content; // longblob 文件的数据
   private String file_name;// 文件名
}