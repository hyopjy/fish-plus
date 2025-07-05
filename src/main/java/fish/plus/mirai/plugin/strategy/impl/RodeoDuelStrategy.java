package fish.plus.mirai.plugin.strategy.impl;


import cn.hutool.core.collection.CollectionUtil;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import fish.plus.mirai.plugin.manager.PermissionManager;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.manager.RodeoRecordManager;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

import java.util.*;


/**
 * 决斗
 */
@Slf4j
public class RodeoDuelStrategy extends RodeoAbstractStrategy {

    private static class Holder {
        static final RodeoDuelStrategy INSTANCE = new RodeoDuelStrategy();
    }

    private RodeoDuelStrategy() {} // 私有构造函数

    public static RodeoDuelStrategy getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void startGame(Rodeo rodeo) {
//        分配决斗[双方][规定时间段]内的比赛[局数]（按局数给权限）
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }
//
//        开场播报里需要加个赛制
//
//        东风吹，战鼓擂，决斗场上怕过谁！
//        新的🏟[大波测试赛决斗第一场02]BO5已确定于[18:02:00-18:20:00]开战！
//        @首届决斗大赛禁言冠军 VS @屁屁

        String roundStr = rodeo.getRound() >=10 ? rodeo.getRound()+"" : "0"+rodeo.getRound();

        String messageFormat1 = "\r\n东风吹，战鼓擂，决斗场上怕过谁！ \r\n 新的🏟[%s] B%s 已确定于[%s-%s]开战！ \r\n";

//        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        String[] players = rodeo.getPlayerIds();
        long player1 = Long.parseLong(players[0]);
        long player2 = Long.parseLong(players[1]);

        String message1 = String.format(messageFormat1, rodeo.getVenue(), roundStr, rodeo.getStartTime(),
                rodeo.getEndTime());

        Message m = new PlainText(message1);
        m = m.plus(new At(player1));
        m = m.plus(" VS ");
        m = m.plus(new At(player2));
        group.sendMessage(m);

    }

    @Override
    public void record(Rodeo rodeo, RodeoRecordGameInfoDto dto) {
        // 用户同一时间段 只能参加一场比赛
        // 每个时间段只有一场比赛
        // 存入输、赢家
        // 记录第几局
        // 获取当前最大的局数
        if(RodeoManager.isDuelOver(rodeo)){
            return;
        }
        Integer maxRound = RodeoRecordManager.getMaxTurnsByRodeoId(rodeo.getId());
        if(maxRound >= rodeo.getRound()){
            return;
        }
        Integer currentRound = maxRound +1;
        RodeoRecord winnerRodeoRecord = new RodeoRecord();
        winnerRodeoRecord.setRodeoId(rodeo.getId());
        winnerRodeoRecord.setPlayer(dto.getWinner());
        winnerRodeoRecord.setForbiddenSpeech(0);
        winnerRodeoRecord.setTurns(currentRound);
        winnerRodeoRecord.setRodeoDesc(dto.getRodeoDesc());
        winnerRodeoRecord.setWinFlag(1);
        winnerRodeoRecord.saveOrUpdate();


        RodeoRecord loseRodeoRecord = new RodeoRecord();
        loseRodeoRecord.setRodeoId(rodeo.getId());
        loseRodeoRecord.setPlayer(dto.getLoser());
        loseRodeoRecord.setForbiddenSpeech(dto.getForbiddenSpeech());
        loseRodeoRecord.setTurns(currentRound);
        loseRodeoRecord.setRodeoDesc(dto.getRodeoDesc());
        loseRodeoRecord.setWinFlag(0);
        loseRodeoRecord.saveOrUpdate();
    }

    @Override
    public void endGame(Rodeo rodeo) {
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }

        Long rodeoId = rodeo.getId();
        String[] players = rodeo.getPlayerIds();
        Long player1 = Long.parseLong(players[0]);
        Long player2 = Long.parseLong(players[1]);

        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(rodeoId);

        if (CollectionUtil.isEmpty(records)) {
            String message = String.format("\r\n %s,%s,%s未进行任何比赛 \r\n",
                    rodeo.getVenue(),
                    new At(player1).getDisplay(group),
                    new At(player2).getDisplay(group));
            group.sendMessage(new PlainText(message));
            cancelGame(rodeo);
            return;
        }
        // 初始化胜者和败者统计信息
        Map<Long, Integer> winCountMap = new HashMap<>();
        Map<Long, Long> forbiddenSpeechMap = new HashMap<>();
        // 遍历所有比赛记录，统计胜场次数和禁言时长
        for (RodeoRecord record : records) {
            Long player = Long.parseLong(record.getPlayer());
            if (record.getWinFlag() == 1) { // 如果该玩家获胜
                winCountMap.put(player, winCountMap.getOrDefault(player, 0) + 1);
            }
            // 统计禁言时长
            forbiddenSpeechMap.put(player, forbiddenSpeechMap.getOrDefault(player, 0L) +
                    Optional.ofNullable(record.getForbiddenSpeech()).orElse(0));
        }
        // 获取两位玩家的胜场次数和禁言时长
        int p1WinCount = winCountMap.getOrDefault(player1, 0);
        int p2WinCount = winCountMap.getOrDefault(player2, 0);
        long p1ForbiddenTime = forbiddenSpeechMap.getOrDefault(player1, 0L);
        long p2ForbiddenTime = forbiddenSpeechMap.getOrDefault(player2, 0L);

        // ========== 关键修改：增加平局判定 ==========
        StringBuilder messageBuilder = new StringBuilder();
        messageBuilder.append("\r\n").append(rodeo.getVenue()).append("结束，");

        if (p1WinCount == p2WinCount) {
            // 平局消息格式
            messageBuilder.append("双方以[")
                    .append(p1WinCount).append(":").append(p2WinCount)
                    .append("]战平！🤝\r\n");
        } else {
            // 胜负判定
            Long winner = p1WinCount > p2WinCount ? player1 : player2;
            Long loser = winner.equals(player1) ? player2 : player1;

            messageBuilder.append("恭喜胜者")
                    .append(new At(winner).getDisplay(group))
                    .append("以[")
                    .append(p1WinCount).append(":").append(p2WinCount)
                    .append("]把对手")
                    .append(new At(loser).getDisplay(group))
                    .append("鸡哔！🔫\r\n");
        }

        // 添加禁言信息（平局/胜负都显示）
        messageBuilder.append(new At(player1).getDisplay(group))
                .append("共被禁言").append(p1ForbiddenTime).append("秒\r\n")
                .append(new At(player2).getDisplay(group))
                .append("共被禁言").append(p2ForbiddenTime).append("秒\r\n")
                .append("菜！就！多！练！");

        group.sendMessage(new PlainText(messageBuilder.toString()));

        // ========== 平局时不发放道具 ==========
        if (p1WinCount != p2WinCount && rodeo.getGiveProp() == 1) {
            Long winner = p1WinCount > p2WinCount ? player1 : player2;
            StringBuilder propMsg = new StringBuilder();
            propMsg.append("[").append(rodeo.getVenue()).append("]结束，恭喜胜者获取全能道具 🎁：")
                    .append(rodeo.getPropName()).append(" \r\n")
                    .append(new At(winner).getDisplay(group))
                    .append(" - 获得道具: ")
                    .append(rodeo.getPropCode()).append("\r\n");

            group.sendMessage(new PlainText(propMsg.toString()));
            publishPropEvent(rodeo.getGroupId(), Collections.singletonList(winner), rodeo.getPropCode());
        }

        cancelGame(rodeo);
    }

    @Override
    public void cancelGame(Rodeo rodeo){
        try{
            cancelPermission(rodeo);
        }catch (Exception e){
        }finally {
            RodeoManager.removeCron(rodeo);
            RodeoManager.removeEndRodeo(rodeo);
        }
    }

    @Override
    public void grantPermission(Rodeo rodeo) {
//        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        String[] players = rodeo.getPlayerIds();
        for(String player: players){
            log.info("决斗授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.grantDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

    @Override
    public void cancelPermission(Rodeo rodeo) {
//        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        String[] players = rodeo.getPlayerIds();
        for(String player: players){
            log.info("决斗取消授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.revokeDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

}
