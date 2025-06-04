package fish.plus.mirai.plugin.event;

import fish.plus.mirai.plugin.mqtt.MqttClientStart;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.MessagePostSendEvent;
import org.jetbrains.annotations.NotNull;

/**
 * 机器人主动发消息事件
 */
public class BotPostSendEventListener extends SimpleListenerHost {
    /**
     *
     * @param event
     */
    @EventHandler()
    public void onMessage(@NotNull MessagePostSendEvent event) {
//            MqttClientStart.getInstance().subscribeTopic("test/topic");
//            MqttClientStart.getInstance().publishMessage("test/topic", event.getMessage().contentToString());
    }
}
