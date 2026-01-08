package events;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAdminEvent {
    private ApiType type;
    private int operatorId;
    private Map<String, Object> params;
    private String resp;
}