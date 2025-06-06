package fish.plus.mirai.plugin.event;

import cn.hutool.core.collection.CollectionUtil;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.SimpleListenerHost;
import net.mamoe.mirai.event.events.MessagePostSendEvent;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.MessageChain;
import net.mamoe.mirai.message.data.PlainText;
import net.mamoe.mirai.message.data.SingleMessage;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static fish.plus.mirai.plugin.strategy.RodeoFactory.DUEL;

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
        String code = event.getMessage().serializeToMiraiCode();

        // [轮盘]
        // [决斗]
        if(!(code.contains("获得一分") || code.contains("并爽快地输掉了这局比赛"))){
            return;
        }

        // [mirai:at:952746839] 😙了一口[mirai:at:1811756096] 的【肩膀🤷‍♀】，让对方被冲昏了3秒头脑。恭喜[mirai:at:952746839] 获得一分！
        // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
        //<target> 开了一枪🔫，枪响了，被冲昏了<mute-f>头脑，并爽快地输掉了这局比赛。

        List<String> messageList = new ArrayList<>();
        MessageChain message = event.getMessage();
        for (SingleMessage singleMessage : message) {
            if (singleMessage instanceof At) {
                At at = (At) singleMessage;
                messageList.add(at.getTarget() +"");
            }
            if(singleMessage instanceof PlainText){
                PlainText text = (PlainText)  singleMessage;
                messageList.add(text.serializeToMiraiCode());
            }
        }
        if(!(messageList.size() == 6 || messageList.size() == 2)){
            return;
        }
        List<Long> atUser = new ArrayList<>();

        if (messageList.size() == 6) {
            atUser.add(Long.parseLong(messageList.get(2)));
            atUser.add(Long.parseLong(messageList.get(4)));
        }
        if (messageList.size() == 2) {
            atUser.add(Long.parseLong(messageList.get(0)));
        }
        if(CollectionUtil.isEmpty(atUser)){
            return;
        }

        Rodeo redeo = RodeoManager.getCurrent(event.getTarget().getId(), atUser);
        if(Objects.isNull(redeo)){
            // 如果用户没有正在进行的比赛
            return;
        }
        // 如果有 则记录

        // [mirai:at:294253294] 😙了一口[mirai:at:952746839] 的【身体】，让对方被冲昏了1分40秒头脑。恭喜[mirai:at:294253294] 获得一分！
        // [mirai:at:952746839] 😙了一口[mirai:at:1811756096] 的【肩膀🤷‍♀】，让对方被冲昏了3秒头脑。恭喜[mirai:at:952746839] 获得一分！
        // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
        RodeoRecordGameInfoDto dto = new RodeoRecordGameInfoDto();
        if (DUEL.equals(redeo.getPlayingMethod())) {
            int totalDuration = 0;
            String winner = messageList.get(4);
            String loser = messageList.get(2);

            // 提取时长
            String timeStar = messageList.get(3);
            String durationStr = timeStar.replaceAll(".*?让对方被冲昏了", "").split("头脑")[0];
            String[] timeParts = durationStr.split("分|秒");
            if(durationStr.contains("分")){
                int minutes = Integer.parseInt(timeParts[0]);
                int seconds = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
                totalDuration += minutes * 60 + seconds;
            }else {
                totalDuration = Integer.parseInt(timeParts[0]);
            }

            // 输出结果（可选）
            System.out.println("赢家: " + winner);
            System.out.println("输家: " + loser);
            System.out.println("总时长: " + totalDuration + "秒");

            // 设置 DTO
            dto.setWinner(winner);
            dto.setLoser(loser);
            dto.setForbiddenSpeech(totalDuration); // 根据需要设置
            dto.setRodeoDesc(code);
        }else{
            // 比赛记录处理
            // "[mirai:at:294253294] 开了一枪🔫，枪响了，被冲昏了4分9秒头脑，并爽快地输掉了这局比赛。"
            // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
            String loser = messageList.get(0);
            int totalDuration = 0;
            // 提取输家信息

            // 提取时长
            String timeStr = messageList.get(1);
            String durationStr = timeStr.replaceAll(".*?被冲昏了", "").split("头脑")[0];
            String[] timeParts = durationStr.split("分|秒");
            if(durationStr.contains("分")){
                int minutes = Integer.parseInt(timeParts[0]);
                int seconds = timeParts.length > 1 ? Integer.parseInt(timeParts[1]) : 0;
                totalDuration += minutes * 60 + seconds;
            }else {
                totalDuration = Integer.parseInt(timeParts[0]);
            }

            // 设置 DTO
            dto.setLoser(loser);
            dto.setForbiddenSpeech(totalDuration); // 设定总时长（秒）
            dto.setRodeoDesc(code);

            // 输出结果（可选）
            System.out.println("输家: " + loser);
            System.out.println("总时长: " + totalDuration + "秒");
        }

        // 轮盘
        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(redeo.getPlayingMethod());
        strategy.record(redeo, dto);

        if(RodeoManager.isDuelOver(redeo)){
            strategy.endGame(redeo);
            return;
        }

        System.out.println(code);

    }
}
