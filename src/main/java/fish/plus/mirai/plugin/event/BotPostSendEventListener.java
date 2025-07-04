package fish.plus.mirai.plugin.event;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.cron.CronUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import fish.plus.mirai.plugin.constants.Constant;
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

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

        String code = event.getMessage().serializeToMiraiCode();
        // [轮盘]
        // [决斗]
        RodeoRecordGameInfoDto dto = bindRodeoRecordGameInfoDto(code, event);
        if(Objects.isNull(dto)){
            System.out.println("解析消息为null ");
            return;
        }
        if(CollectionUtil.isEmpty(dto.getAtUser())){
            System.out.println("at 为00 ");
            return;
        }
        Rodeo redeo = RodeoManager.getCurrent(event.getTarget().getId(), dto.getAtUser());
        if(Objects.isNull(redeo)){
            // 如果用户没有正在进行的比赛
            System.out.println("如果用户没有正在进行的比赛*** " + event.getTarget().getId() + "====" + JSONUtil.toJsonStr(dto.getAtUser()));
            return;
        }
        // 轮盘
        RodeoStrategy strategy =  RodeoFactory.createRodeoDuelStrategy(redeo.getPlayingMethod());
        strategy.record(redeo, dto);
        if(RodeoManager.isDuelOver(redeo)){
            strategy.endGame(redeo);
            strategy.removeEndTask(redeo);
            return;
        }
        System.out.println(code);

    }

    // 处理消息
    // 决斗
    // <target-win> 😙了一口<target-lose> 的【<position>】，让对方被冲昏了<mute-f>头脑。恭喜<target-win> 获得一分！
    // SingleMessage 拆分的list: ["934415751", "😙了一口", "952746839", "的身体，让对方被冲昏了<mute-f>头脑。恭喜", "934415751", "获得一分！"]
    // 6

    // <target-lose> 的<position>被<target-win> 开枪击中，但对方是管理员，逃掉了<mute-f>的禁言
    // SingleMessage 拆分的list: ["934415751", "的身体被", "952746839", "开枪击中，但对方是管理员，逃掉了1分15秒的禁言"]
    // 4

    // 轮盘
    // <target> 开了一枪🔫，枪响了，被冲昏了<mute-f>头脑，并爽快地输掉了这局比赛。
    // SingleMessage 拆分的list: ["934415751", "了一枪🔫，枪响了，被冲昏了5秒头脑，并爽快地输掉了这局比赛。"]
    // 2

    // <target> 开了一枪，枪没响，还剩<remain-chamber>轮，幸运之神暂时眷顾于此。恭喜<target>获得一分！
    // SingleMessage 拆分的list: ["934415751", "开了一枪，枪没响，还剩3轮，幸运之神暂时眷顾于此。恭喜","934415751","获得一分！"]
    // 4

    // <target> 开了一枪，枪响了，但对方是管理员，逃掉了<mute-f>的禁言
    // SingleMessage 拆分的list: ["934415751", "开了一枪，枪响了，但对方是管理员，逃掉了1分77秒的禁言"]  -- 这种算target获胜
    // 2

    // 此轮俄罗斯轮盘因超时结束，因未能全部击发发起人<target>被禁言<mute-f>
    // SingleMessage 拆分的list: ["此轮俄罗斯轮盘因超时结束，因未能全部击发发起人", "934415751", "被禁言20秒"]  -- 这种算target获胜


    private RodeoRecordGameInfoDto bindRodeoRecordGameInfoDto(String code, MessagePostSendEvent event) {
        // 场景类型判断（新增轮盘超时类型）
        boolean isTargetMsg = code.contains("获得一分")
                || code.contains("开枪击中，但对方是管理员")
                || code.contains("并爽快地输掉了这局比赛")
                || code.contains("逃掉了")
                || code.contains("被禁言")
                || code.contains("超时结束");  // 关键新增
        if (!isTargetMsg) return null;

        // 解析消息链
        List<String> messageList = new ArrayList<>();
        Set<Long> atUser = new HashSet<>();
        for (SingleMessage msg : event.getMessage()) {
            if (msg instanceof At) {
                long target = ((At) msg).getTarget();
                atUser.add(target);
                messageList.add(String.valueOf(target));
            } else if (msg instanceof PlainText) {
                messageList.add(((PlainText) msg).serializeToMiraiCode());
            }
        }

        int size = messageList.size();
        RodeoRecordGameInfoDto dto = new RodeoRecordGameInfoDto();
        dto.setAtUser(atUser);
        dto.setRodeoDesc(code);

        // 场景精确匹配（修正6种情况）
        try {
            /* 决斗场景（2种） */
            if (size == 6) {
                // 类型1：决斗获胜（6元素）
                dto.setWinner(messageList.get(4));  // 赢家在索引4
                dto.setLoser(messageList.get(2));   // 输家在索引2
                dto.setForbiddenSpeech(parseDuration(messageList.get(3)));
            }
            else if (size == 4 && code.contains("开枪击中，但对方是管理员")) {
                // 类型2：决斗-管理员豁免（4元素）
                dto.setWinner(messageList.get(2));  // 赢家为开枪者（索引2）
                dto.setLoser(messageList.get(0));   // 输家为被击中者（索引0）
                dto.setForbiddenSpeech(0);
            }
            /* 轮盘场景（4种） */
            else if (size == 4) {
                // 类型3：轮盘正常获胜（4元素）
                dto.setWinner(messageList.get(2));  // 赢家在索引2
//                dto.setLoser(messageList.get(0));   // 操作为输家（索引0）
                // 此场景无禁言
            }
            else if (size == 2 && code.contains("但对方是管理员")) {
                // 类型4：轮盘-管理员豁免（2元素）
                dto.setWinner(messageList.get(0));  // 操作者获胜
//                dto.setForbiddenSpeech(parseDuration(messageList.get(1)));
            }
            else if (size == 2) {
                // 类型5：轮盘-正常失败（2元素）
                dto.setLoser(messageList.get(0));  // 输家
                dto.setForbiddenSpeech(parseDuration(messageList.get(1)));
            }
            else if (size == 3) {  // 关键修正：超时场景是3元素！
                // 类型6：轮盘超时（3元素）
                dto.setLoser(messageList.get(1));  // 发起人（索引1）
                dto.setForbiddenSpeech(parseDuration(messageList.get(2)));
            }
            else return null;  // 未匹配任何场景
        } catch (Exception e) {
            return null;
        }
        return dto;
    }

    // 万能时间解析（支持5种格式）
    public static int parseDuration(String text) {
        // 优化正则：匹配三种格式 (X分Y秒、X分、Y秒)
        Matcher m = Pattern.compile("(\\d+)分(\\d+)秒|(\\d+)分|(\\d+)秒").matcher(text);
        if (m.find()) {
            // 格式1: X分Y秒 (分组1和2)
            if (m.group(1) != null && m.group(2) != null) {
                return Integer.parseInt(m.group(1)) * 60 + Integer.parseInt(m.group(2));
            }
            // 格式2: X分 (分组3)
            else if (m.group(3) != null) {
                return Integer.parseInt(m.group(3)) * 60;
            }
            // 格式3: Y秒 (分组4)
            else if (m.group(4) != null) {
                return Integer.parseInt(m.group(4));
            }
        }
        return 0;
    }

}
