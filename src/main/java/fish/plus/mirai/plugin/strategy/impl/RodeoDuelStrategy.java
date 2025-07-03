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

        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
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

//            2.该场比赛结束后，统计双方的得分和总被禁言时长
//【
//        [比赛场次名]结束，恭喜胜者@B以[3:1]把对手@A鸡哔！🔫
//    @B共被禁言[秒]
//    @A共被禁言[秒]
//    菜！就！多！练！
//            】
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }

        Long rodeoId = rodeo.getId();
        // 获取参赛玩家列表
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        Long player1 = Long.parseLong(players[0]);
        Long player2 = Long.parseLong(players[1]);

        // 获取当前赛事的所有记录
        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(rodeoId);

        // 如果没有比赛记录，则直接返回未进行比赛的消息
        if (CollectionUtil.isEmpty(records)) {
            String messageFormat = "\r\n %s,%s,%s未进行任何比赛 \r\n";
            String message = String.format(messageFormat, rodeo.getVenue(),
                    new At(player1).getDisplay(group), new At(player2).getDisplay(group));
            group.sendMessage(new PlainText(message));

            cancelGame(rodeo);
            return;
        }

        // 初始化胜者和败者统计信息
        Map<Long, Integer> winCountMap = new HashMap<>(); // 胜场次数统计
        Map<Long, Long> forbiddenSpeechMap = new HashMap<>(); // 禁言时长统计

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

        // 判断胜者
        Long winner = p1WinCount > p2WinCount ? player1 : player2;
        Long loser = p1WinCount > p2WinCount ? player2 : player1;

        // 构建输出消息
//        String messageFormat = "\r\n %s结束，恭喜胜者%s以[%d:%d]把对手%s鸡哔！🔫\r\n" ;
//                "%s共被禁言%d 秒\r\n" +
//                "%s共被禁言%d 秒\r\n" +
//                "菜！就！多！练！ ";
        Message m = new PlainText(String.format("\r\n %s结束，恭喜胜者", rodeo.getVenue()));
        m = m.plus(new At(winner));
        m = m.plus(String.format("以[%d:%d]把对手", p1WinCount, p2WinCount));
        m = m.plus(new At(loser));
        m = m.plus(" 鸡哔！🔫 \r\n");
        m = m.plus(new At(player1));
        m = m.plus(String.format("共被禁言%d 秒\r\n", p1ForbiddenTime));
        m = m.plus(new At(player2));
        m = m.plus(String.format("共被禁言%d 秒\r\n", p2ForbiddenTime));
        m.plus("菜！就！多！练！ ");
        // 发送消息
        group.sendMessage(m);


        if(1 == rodeo.getGiveProp()){
            // 赢家获取全能道具
            Message m1 = new PlainText(String.format("[%s]结束，恭喜胜者获取全能道具 🎁：%s \r\n", rodeo.getVenue(), rodeo.getPropName()));
            m1 = m1.plus(new At(winner));
            m1 = m1.plus(" - 获得道具: ");
            m1 = m1.plus(rodeo.getPropCode() + "\r\n");
            group.sendMessage(m1);

            List<Long> userIds = new ArrayList<>();
            userIds.add(winner);
            publishPropEvent(rodeo.getGroupId(), userIds, rodeo.getPropCode());
        }
        cancelGame(rodeo);
    }

    public void cancelGame(Rodeo rodeo){
        try{
            cancelPermission(rodeo);
        }catch (Exception e){
        }finally {
            RodeoManager.removeEndRodeo(rodeo);
        }
    }

    @Override
    public RodeoRecordGameInfoDto analyzeMessage(String message) {
        return null;
    }

    @Override
    public void grantPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            log.info("决斗授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.grantDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

    @Override
    public void cancelPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            log.info("决斗取消授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.revokeDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

}
