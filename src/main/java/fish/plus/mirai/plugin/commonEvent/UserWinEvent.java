package fish.plus.mirai.plugin.commonEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.AbstractEvent;
import net.mamoe.mirai.event.Event;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserWinEvent  extends AbstractEvent {
    String action;

    Long groupId;

    List<Long> userIds;

    String propCode;

    public UserWinEvent(String action, Long groupId, List<Long> userIds, String propCode) {
        this.action = action;
        this.groupId = groupId;
        this.userIds = userIds;
        this.propCode = propCode;
    }

}


