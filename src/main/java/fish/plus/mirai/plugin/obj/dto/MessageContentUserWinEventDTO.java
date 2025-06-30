package fish.plus.mirai.plugin.obj.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class MessageContentUserWinEventDTO extends MessageContentDTO{
    private static final long serialVersionUID = 6955947368326855990L;

    private List<Long> userIds;

    private String propCode;
}
