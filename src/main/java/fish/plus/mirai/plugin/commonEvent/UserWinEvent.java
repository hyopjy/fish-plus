package fish.plus.mirai.plugin.commonEvent;

import net.mamoe.mirai.contact.Member;
import net.mamoe.mirai.event.Event;

public class UserWinEvent implements Event {
    private final Member winner;    // 获胜群成员
    private final String propCode;  // 道具代码
    private final int count;        // 道具数量

    public UserWinEvent(Member winner, String propCode, int count) {
        this.winner = winner;
        this.propCode = propCode;
        this.count = count;
    }

    // Getter 方法
    public Member getWinner() { return winner; }
    public String getPropCode() { return propCode; }
    public int getCount() { return count; }

    @Override
    public boolean isIntercepted() {
        return false;
    }

    @Override
    public void intercept() {

    }
}

