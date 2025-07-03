package fish.plus.mirai.plugin.strategy.impl;


import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.manager.PermissionManager;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.obj.dto.RodeoEndGameInfoDto;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import fish.plus.mirai.plugin.util.Log;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import fish.plus.mirai.plugin.manager.RodeoRecordManager;


import java.util.*;
import java.util.stream.Collectors;

/**
 * 大乱斗
 */
@Slf4j
public class RodeoSuperSmashBrothersStrategy extends RodeoAbstractStrategy {
//    大乱斗（多人决斗，逻辑同轮盘）
//            1.分配决斗[多方][时间段]（10分钟左右，手动配置）内的比赛（按时间段给权限）
//            【
//    东风吹，战鼓擂，决斗场上怕过谁！
//    新的🏟[比赛场次名]正式开战！比赛时长[10分钟]，参赛选手有：@A@B@C@D
//    大乱斗比赛正式打响！🔫[10分钟]的比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？
//            】

    private static class Holder {
        static final RodeoSuperSmashBrothersStrategy INSTANCE = new RodeoSuperSmashBrothersStrategy();
    }

    private RodeoSuperSmashBrothersStrategy() {} // 私有构造函数

    public static RodeoSuperSmashBrothersStrategy getInstance() {
        return RodeoSuperSmashBrothersStrategy.Holder.INSTANCE;
    }

    @Override
    public void startGame(Rodeo rodeo) {
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }

        String messageFormat1= "\r\n东风吹，战鼓擂，轮盘赛上怕过谁！ \r\n新的🏟[%s]正式开战！比赛时长[%s]，参赛选手有： \r\n";

        String messageFormat2= "\r\n 轮盘比赛正式打响！🔫[%s]的比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？\r\n";

        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);

        long playerTime = DateUtil.between(DateUtil.parse(rodeo.getStartTime(),
                DatePattern.NORM_TIME_PATTERN), DateUtil.parse(rodeo.getEndTime(), DatePattern.NORM_TIME_PATTERN), DateUnit.MINUTE);

        String message1 = String.format(messageFormat1, rodeo.getVenue(), playerTime+"分钟");
        String message2 = String.format(messageFormat2, playerTime+"分钟");

        Message m = new PlainText(message1);
        for(String str : players){
            long playerId = Long.parseLong(str);
            m = m.plus(new At(playerId));
        }
        m = m.plus(message2);

        group.sendMessage(m);

    }

    @Override
    public void record(Rodeo rodeo, RodeoRecordGameInfoDto dto) {
        // 存入输家
        // 存入输家
        RodeoRecord loserodeorecord = new RodeoRecord();
        loserodeorecord.setRodeoId(rodeo.getId());
        loserodeorecord.setPlayer(dto.getLoser());
        loserodeorecord.setForbiddenSpeech(dto.getForbiddenSpeech());
        loserodeorecord.setTurns(null);
        loserodeorecord.setRodeoDesc(dto.getRodeoDesc());
        loserodeorecord.setWinFlag(0);
        loserodeorecord.saveOrUpdate();


        RodeoRecord winnerRodeoRecord = new RodeoRecord();
        winnerRodeoRecord.setRodeoId(rodeo.getId());
        winnerRodeoRecord.setPlayer(dto.getWinner());
        winnerRodeoRecord.setForbiddenSpeech(0);
        winnerRodeoRecord.setTurns(null);
        winnerRodeoRecord.setRodeoDesc(dto.getRodeoDesc());
        winnerRodeoRecord.setWinFlag(1);
        winnerRodeoRecord.saveOrUpdate();

    }

    @Override
    public void endGame(Rodeo rodeo) {
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }
        Long rodeoId = rodeo.getId();
        // 获取当前赛事的所有记录
        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(rodeoId);

        // 按玩家分组记录
        Map<String, List<RodeoRecord>> recordsByPlayer = records.stream()
                .filter(obj -> Objects.nonNull(obj) && Objects.nonNull(obj.getPlayer()))
                .collect(Collectors.groupingBy(RodeoRecord::getPlayer));

        // 获取所有参赛者列表
        String[] playersArray = rodeo.getPlayers().split(Constant.MM_SPILT);
        List<String> allPlayers = Arrays.asList(playersArray);

        List<RodeoEndGameInfoDto> dtoList = new ArrayList<>();

        // 遍历所有参赛者生成统计数据（包含无记录的玩家）
         allPlayers.forEach(player -> {
            List<RodeoRecord> playerRecords = recordsByPlayer.getOrDefault(player, Collections.emptyList());

            RodeoEndGameInfoDto dto = new RodeoEndGameInfoDto();
            dto.setPlayer(player);

            int winCount = 0;
            int totalForbidden = 0;

            if (!CollectionUtil.isEmpty(playerRecords)) {
                // 计算获胜次数（只统计winFlag=1的记录）
                winCount = (int) playerRecords.stream()
                        .filter(r -> r.getWinFlag() == 1)
                        .count();

                // 计算总禁言时长
                totalForbidden = playerRecords.stream()
                        .mapToInt(RodeoRecord::getForbiddenSpeech)
                        .sum();
            }

            dto.setScore(winCount);
            dto.setForbiddenSpeech(totalForbidden);

            // 计算积分：得分 - 禁言时长/90（整数除法）
            int integral = winCount - (totalForbidden / 90);
            dto.setIntegral(integral);  // 需要给DTO添加integral字段

            dtoList.add(dto);
        });

        // 按积分降序排序（积分相同则按原始顺序）
        List<RodeoEndGameInfoDto> integralRanking = dtoList.stream()
                .sorted(Comparator.comparingInt(RodeoEndGameInfoDto::getIntegral).reversed())
                .toList();

        // 构建消息内容
        Message m = new PlainText(String.format("[%s]结束，比赛结束\r\n \uD83C\uDFC6  积分排行榜：\r\n", rodeo.getVenue()));

        int currentRank = 1;  // 当前显示的名次
        Integer lastIntegral = null;  // 上一个玩家的积分

        for (int i = 0; i < integralRanking.size(); i++) {
            RodeoEndGameInfoDto dto = integralRanking.get(i);

            // 处理并列排名：积分相同则名次不变
            if (lastIntegral != null && !lastIntegral.equals(dto.getIntegral())) {
                currentRank = i + 1;  // 积分不同时更新名次
            }
            lastIntegral = dto.getIntegral();

            // 拼接排名信息
            m = m.plus(currentRank + ".");
            m = m.plus(new At(Long.parseLong(dto.getPlayer())));
            m = m.plus(String.format(" - %d分（%d分，%d秒）\r\n",
                    dto.getIntegral(),
                    dto.getScore(),
                    dto.getForbiddenSpeech()));
        }

        // 发送消息
        group.sendMessage(m);
        // 给第一名奖励
        if(rodeo.getGiveProp()){
            rankedFirst(integralRanking, rodeo);
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


    private void rankedFirst(List<RodeoEndGameInfoDto> integralRanking, Rodeo rodeo) {

        // 找到第一个有效分数（跳过-99分）
        // 找出所有第一名玩家
        List<RodeoEndGameInfoDto> firstPlacePlayers = new ArrayList<>();
        int topScore; // 初始化最高分为-1

        if (!integralRanking.isEmpty()) {
            topScore = integralRanking.get(0).getIntegral();  // 获取最高积分

            // 只有最高分大于0时才添加第一名玩家
            if (topScore > 0) {
                firstPlacePlayers = integralRanking.stream()
                        .filter(dto -> dto.getIntegral() == topScore)
                        .collect(Collectors.toList());
            }
        } else {
            topScore = -1;
        }
        if(CollectionUtil.isEmpty(firstPlacePlayers)){
            Log.info("【轮盘】-rankedFirst： 筛选第一的分数： " + topScore +" 数据长度为空");
            return;
        }

        Message m = new PlainText(String.format("[%s]结束，恭喜第一名获取道具 🎁：%s \r\n", rodeo.getVenue(), rodeo.getPropName()));
        int rank = 1;
        List<Long> userIds = new ArrayList<>();
        for (RodeoEndGameInfoDto dto : firstPlacePlayers) {
            userIds.add(Long.parseLong(dto.getPlayer()));
            m =  m.plus(rank++ + ".");
            m = m.plus(new At(Long.parseLong(dto.getPlayer())));
            m = m.plus(" - 获得道具: ");
            m = m.plus(rodeo.getPropCode() + "\r\n");
        }
        publishPropEvent(rodeo.getGroupId(), userIds, rodeo.getPropCode());

        Group group = getBotGroup(rodeo.getGroupId());
        group.sendMessage(m);

    }

    @Override
    public RodeoRecordGameInfoDto analyzeMessage(String message) {
        return null;
    }

    @Override
    public void grantPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            log.info("大乱斗授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.grantDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }

    @Override
    public void cancelPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            log.info("大乱斗取消授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.revokeDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.DUEL_PERMISSION);
        }
    }
}
