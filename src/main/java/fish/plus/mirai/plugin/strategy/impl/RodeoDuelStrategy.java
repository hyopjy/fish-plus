package fish.plus.mirai.plugin.strategy.impl;


import cn.hutool.core.collection.CollectionUtil;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import fish.plus.mirai.plugin.manager.PermissionManager;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.manager.RodeoRecordManager;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 决斗
 */
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
//        【
//        东风吹，战鼓擂，决斗场上怕过谁！
//        新的🏟[比赛场次名]已确定于[14:00-17:00]开战！
//        [@A ]与[@B ]正式展开决斗的巅峰对决！⚔[N]局比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？
//        】

        String messageFormat1 = "\r\n东风吹，战鼓擂，决斗场上怕过谁！ \r\n 新的🏟[%s]已确定于[%s-%s]开战！ \r\n";
        String messageFormat2 = "\r\n正式展开决斗的巅峰对决！⚔[%s]局比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？\r\n";

        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        Long player1 = Long.parseLong(players[0]);
        Long player2 = Long.parseLong(players[1]);

        String message1 = String.format(messageFormat1, rodeo.getVenue(), rodeo.getStartTime(),
                rodeo.getEndTime());

        String message2 = String.format(messageFormat2, rodeo.getRound());

        Message m = new PlainText(message1);
        m = m.plus(new At(player1).getDisplay(group));
        m = m.plus(" VS ");
        m = m.plus(new At(player2).getDisplay(group));
        m.plus(message2);
        group.sendMessage(m);


        // todo 开始决斗权限

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
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        Long player1 = Long.parseLong(players[0]);
        Long player2 = Long.parseLong(players[1]);

        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(rodeoId);
        if(CollectionUtil.isEmpty(records)){
            String messageFormat = "\r\n %s,%s,%s未进行任何比赛 \r\n";
            String message = String.format(messageFormat, rodeo.getVenue(),
                    new At(player1).getDisplay(group), new At(player2).getDisplay(group));
            group.sendMessage(new PlainText(message));
            return ;
        }

        List<RodeoRecord> winnerPlayers = new ArrayList<RodeoRecord>();
        List<RodeoRecord> losePlayers = new ArrayList<RodeoRecord>();
        // 局数
        Map<Integer, List<RodeoRecord>> recordsByTurns = records.stream()
                .collect(Collectors.groupingBy(RodeoRecord::getTurns));
        recordsByTurns.forEach((turns, recordList) -> {
            Optional<RodeoRecord> winnerOptional = recordList.stream().filter(r-> Objects.isNull(r.getForbiddenSpeech())
                    || r.getForbiddenSpeech().equals(0)).findAny();
            winnerOptional.ifPresent(winnerPlayers::add);

            Optional<RodeoRecord> loseOptional = recordList.stream().filter(r->  r.getForbiddenSpeech() > 0).findAny();
            loseOptional.ifPresent(losePlayers::add);
        });

        long winner;
        long lose;

        int winnerScore;
        int loseScore;
        long winnerTimeSum = 0L;
        long loseTimeSum = 0L;
        Map<String, List<RodeoRecord>> winnerMap = winnerPlayers.stream()
                .collect(Collectors.groupingBy(RodeoRecord::getPlayer));


        List<RodeoRecord> p1RecordWinList = Optional.ofNullable(winnerMap.get(""+ player1)).orElse(new ArrayList<>());
        List<RodeoRecord> p2RecordWinList = Optional.ofNullable(winnerMap.get(""+ player2)).orElse(new ArrayList<>());
        if(p1RecordWinList.size() > p2RecordWinList.size()){
            winner = player1;
            lose = player2;
            winnerTimeSum = winnerTimeSum + p1RecordWinList.stream().mapToLong(obj -> Optional.ofNullable(obj.getForbiddenSpeech()).orElse(0)).sum();
            loseTimeSum = loseTimeSum + p2RecordWinList.stream().mapToLong(obj -> Optional.ofNullable(obj.getForbiddenSpeech()).orElse(0)).sum();
            winnerScore = p1RecordWinList.size();
            loseScore = p2RecordWinList.size();
        }else {
            winner = player2;
            lose = player1;
            winnerTimeSum = winnerTimeSum + p2RecordWinList.stream().mapToLong(obj -> Optional.ofNullable(obj.getForbiddenSpeech()).orElse(0)).sum();
            loseTimeSum = loseTimeSum  + p1RecordWinList.stream().mapToLong(obj -> Optional.ofNullable(obj.getForbiddenSpeech()).orElse(0)).sum();
            winnerScore = p2RecordWinList.size();
            loseScore = p1RecordWinList.size();
        }

        Map<String, List<RodeoRecord>> loserMap = losePlayers.stream()
                .collect(Collectors.groupingBy(RodeoRecord::getPlayer));
        List<RodeoRecord> winerLoseList = Optional.ofNullable(loserMap.get(""+ winner)).orElse(new ArrayList<>());
        List<RodeoRecord> loseLoseList = Optional.ofNullable(loserMap.get(""+ lose)).orElse(new ArrayList<>());

        winnerTimeSum = winnerTimeSum + winerLoseList.stream().mapToLong(obj -> Optional.ofNullable(obj.getForbiddenSpeech()).orElse(0)).sum();
        loseTimeSum = loseTimeSum  + loseLoseList.stream().mapToLong(obj -> Optional.ofNullable(obj.getForbiddenSpeech()).orElse(0)).sum();

        // 决斗存入赢+输的场次
        String messageFormat = "\r\n %s结束，恭喜胜者%s以[%s:%s]把对手%s鸡哔！🔫\r\n %s共被禁言%s 秒\r\n %s共被禁言%s 秒\r\n 菜！就！多！练！ ";


        String message = String.format(messageFormat, rodeo.getVenue(), new At(winner).getDisplay(group),
                winnerScore, loseScore, new At(lose).getDisplay(group), new At(winner).getDisplay(group),
                winnerTimeSum, new At(lose).getDisplay(group), loseTimeSum);
        group.sendMessage(new PlainText(message));

        cancelPermission(rodeo);
        RodeoManager.removeEndRodeo(rodeo);
    }

    @Override
    public RodeoRecordGameInfoDto analyzeMessage(String message) {
        return null;
    }

    @Override
    public void grantPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            PermissionManager.grantDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

    @Override
    public void cancelPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            PermissionManager.revokeDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

}
