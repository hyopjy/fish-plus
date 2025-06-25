package fish.plus.mirai.plugin.commonEvent;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.AbstractEvent;
import net.mamoe.mirai.event.Event;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserWinEvent  extends AbstractEvent {
    String action;

    public UserWinEvent(String action) {
        this.action = action;
    }

}



