package events;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommitEvent {
    private ApiType type;
    private int userId;
    private Map<String, Object> params;
    private String resp;
}