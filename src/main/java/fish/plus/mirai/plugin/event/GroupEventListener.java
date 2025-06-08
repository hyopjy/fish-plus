package fish.plus.mirai.plugin.event;

import fish.plus.mirai.plugin.manager.RodeoManager;
import net.mamoe.mirai.contact.Contact;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.MessageEvent;
import org.jetbrains.annotations.NotNull;

public class GroupEventListener extends SimpleListenerHost {

    @EventHandler()
    public void onMessage(@NotNull MessageEvent event) {
        Contact subject = event.getSubject();
        Group group = null;
        if (subject instanceof Group) {
            group = (Group) subject;
        }
//        if ((code.startsWith("开启决斗") || code.startsWith("开启轮盘") || code.startsWith("开启大乱斗"))
//                && EconomyEventConfig.INSTANCE.getEconomyLongByRandomAdmin().contains(sender.getId())) {
//
//        }
        String code = event.getMessage().serializeToMiraiCode();
        if ("开始比赛".equals(code)) {
            RodeoManager.init();
//            subject.sendMessage("比赛将在一分钟后开始⚡️⚡️");
        }


    }
}
