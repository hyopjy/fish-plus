package fish.plus.mirai.plugin.event;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import fish.plus.mirai.plugin.strategy.RodeoFactory;
import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import net.mamoe.mirai.event.EventHandler;
import net.mamoe.mirai.event.EventPriority;
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
    @EventHandler(priority = EventPriority.LOWEST)
    public void onMessage(@NotNull MessagePostSendEvent event) {


//            MqttClientStart.getInstance().subscribeTopic("test/topic");
//            MqttClientStart.getInstance().publishMessage("test/topic", event.getMessage().contentToString());
        String code = event.getMessage().serializeToMiraiCode();

        System.out.println("监听到消息*** " + code);
        // [轮盘]
        // [决斗]
        if(!(code.contains("获得一分") || code.contains("并爽快地输掉了这局比赛")) || code.contains("开枪击中，但对方是管理员")){
            return;
        }

        // [mirai:at:952746839] 😙了一口[mirai:at:1811756096] 的【肩膀🤷‍♀】，让对方被冲昏了3秒头脑。恭喜[mirai:at:952746839] 获得一分！
        // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
        //<target> 开了一枪🔫，枪响了，被冲昏了<mute-f>头脑，并爽快地输掉了这局比赛。
        // <target-lose> 的<position>被<target-win> 开枪击中，但对方是管理员，逃掉了<mute-f>的禁言

        // 处理消息
        // 决斗
        // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
        // <target-lose> 的<position>被<target-win> 开枪击中，但对方是管理员，逃掉了<mute-f>的禁言

        // 两位决斗者同时亲亲，<target-2> 😙了一口<target-1> 的【<position-1>】，让对方被冲昏了<mute-f-1>头脑，
        //          <target-1> 😙了一口<target-2> 的【<position-2>】，让对方被冲昏了<mute-f-2>头脑，两人👩‍❤️‍💋‍👩在一起难解难分

        // 轮盘
        // <target> 开了一枪🔫，枪响了，被冲昏了<mute-f>头脑，并爽快地输掉了这局比赛。

        // <target> 开了一枪，枪没响，还剩<remain-chamber>轮，幸运之神暂时眷顾于此。恭喜<target>获得一分！
        // <target> 开了一枪，枪响了，但对方是管理员，逃掉了<mute-f>的禁言
        // 此轮俄罗斯轮盘因超时结束，因未能全部击发发起人<target>被禁言<mute-f>



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
        if(!(messageList.size() == 6 || messageList.size() == 2 || messageList.size() == 4)){
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
        if (messageList.size() == 4) {
            atUser.add(Long.parseLong(messageList.get(0)));
            atUser.add(Long.parseLong(messageList.get(2)));
        }
        if(CollectionUtil.isEmpty(atUser)){
            System.out.println("at 为00 ");
            return;
        }

        Rodeo redeo = RodeoManager.getCurrent(event.getTarget().getId(), atUser);
        if(Objects.isNull(redeo)){
            // 如果用户没有正在进行的比赛
            System.out.println("如果用户没有正在进行的比赛*** " + event.getTarget().getId() + "====" + JSONUtil.toJsonStr(atUser));
            return;
        }
        // 如果有 则记录

        // [mirai:at:294253294] 😙了一口[mirai:at:952746839] 的【身体】，让对方被冲昏了1分40秒头脑。恭喜[mirai:at:294253294] 获得一分！
        // [mirai:at:952746839] 😙了一口[mirai:at:1811756096] 的【肩膀🤷‍♀】，让对方被冲昏了3秒头脑。恭喜[mirai:at:952746839] 获得一分！
        // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
        RodeoRecordGameInfoDto dto = new RodeoRecordGameInfoDto();
        if (DUEL.equals(redeo.getPlayingMethod())) {
            int totalDuration = 0;
            String winner = "";
            String loser = "";
            // 提取时长
            String timeStar = "";
            String durationStr = "";
            if(messageList.size() == 6){
              winner = messageList.get(4);
              loser = messageList.get(2);
              timeStar = messageList.get(3);
              durationStr = timeStar.replaceAll(".*?让对方被冲昏了", "").split("头脑")[0];
            }
            if(messageList.size() == 4){
                winner = messageList.get(2);
                loser = messageList.get(0);
                timeStar = messageList.get(3);
                // 但对方是管理员，逃掉了<mute-f>的禁言
                durationStr = timeStar.replaceAll(".*?逃掉了", "").split("的禁言")[0];
            }

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
            // 提取输家信息
            // 提取时长
            String loser = "";
            String timeStr = "";
            String durationStr ="";
            if (messageList.size() == 6) {
                String winner = messageList.get(4);
                // 大乱斗记入输的 和赢家
                loser = messageList.get(2);
                timeStr = messageList.get(3);
                durationStr = timeStr.replaceAll(".*?让对方被冲昏了", "").split("头脑")[0];

                dto.setWinner(winner);
            }
            if(messageList.size() == 4){
                String winner = messageList.get(2);
                loser = messageList.get(0);
                timeStr = messageList.get(3);
                durationStr = timeStr.replaceAll(".*?逃掉了", "").split("的禁言")[0];
                dto.setWinner(winner);
            }
            if(messageList.size() == 2){
                loser = messageList.get(0);
                timeStr = messageList.get(1);
                durationStr = timeStr.replaceAll(".*?被冲昏了", "").split("头脑")[0];
            }
            // 比赛记录处理
            // "[mirai:at:294253294] 开了一枪🔫，枪响了，被冲昏了4分9秒头脑，并爽快地输掉了这局比赛。"
            // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！

            int totalDuration = 0;
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
