package fish.plus.mirai.plugin.strategy.impl;

import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import fish.plus.mirai.plugin.manager.PermissionManager;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.obj.dto.PlayerStats;
import fish.plus.mirai.plugin.obj.dto.RodeoEndGameInfoDto;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import lombok.extern.slf4j.Slf4j;
import net.mamoe.mirai.contact.Group;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;
import fish.plus.mirai.plugin.manager.RodeoRecordManager;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
/**
 * 轮盘
 */
public class RodeoRouletteStrategy extends RodeoAbstractStrategy {

    private static class Holder {
        static final RodeoRouletteStrategy INSTANCE = new RodeoRouletteStrategy();
    }

    private RodeoRouletteStrategy() {} // 私有构造函数

    public static RodeoRouletteStrategy getInstance() {
        return Holder.INSTANCE;
    }

    @Override
    public void startGame(Rodeo rodeo) {
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }

        String messageFormat1= "\r\n东风吹，战鼓擂，轮盘赛上怕过谁！\r\n新的🏟[%s]正式开战！比赛时长[%s]，参赛选手有： \r\n";

        String messageFormat2= "\r\n轮盘比赛正式打响！🔫[%s]的比赛，谁将笑傲鱼塘🤺，谁又将菜然神伤🥬？\r\n";

        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);

        long playerTime = DateUtil.between(DateUtil.parse(rodeo.getStartTime(),
                DatePattern.NORM_TIME_PATTERN), DateUtil.parse(rodeo.getEndTime(), DatePattern.NORM_TIME_PATTERN), DateUnit.MINUTE);

        String message1 = String.format(messageFormat1, rodeo.getVenue(), playerTime+"分钟");
        String message2 = String.format(messageFormat2, playerTime+"分钟");

        Message m = new PlainText(message1);
        for(String str : players){
            Long playerId = Long.parseLong(str);
            m = m.plus(new At(playerId));
        }
        m = m.plus(message2);

        group.sendMessage(m);

    }

    @Override
    public void record(Rodeo rodeo, RodeoRecordGameInfoDto dto) {
        // 存入输家
        if(StringUtils.isNotBlank(dto.getLoser())){
            RodeoRecord loserodeorecord = new RodeoRecord();
            loserodeorecord.setRodeoId(rodeo.getId());
            loserodeorecord.setPlayer(dto.getLoser());
            loserodeorecord.setForbiddenSpeech(dto.getForbiddenSpeech());
            loserodeorecord.setTurns(null);
            loserodeorecord.setRodeoDesc(dto.getRodeoDesc());
            loserodeorecord.setWinFlag(0);
            loserodeorecord.saveOrUpdate();
        }
        if(StringUtils.isNotBlank(dto.getWinner())){
            RodeoRecord winerrodeorecord = new RodeoRecord();
            winerrodeorecord.setRodeoId(rodeo.getId());
            winerrodeorecord.setPlayer(dto.getWinner());
            winerrodeorecord.setForbiddenSpeech(0);
            winerrodeorecord.setTurns(null);
            winerrodeorecord.setRodeoDesc(dto.getRodeoDesc());
            winerrodeorecord.setWinFlag(1);
            winerrodeorecord.saveOrUpdate();
        }

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

        // 按玩家分组记录（使用Map<String, PlayerStats>存储统计数据）
        Map<String, PlayerStats> playerStatsMap = records.stream()
                .collect(Collectors.groupingBy(
                        RodeoRecord::getPlayer,
                        Collectors.collectingAndThen(Collectors.toList(), list -> {
                            PlayerStats stats = new PlayerStats();
                            stats.setShotCount(list.size());
                            stats.setTotalForbidden(list.stream()
                                    .mapToInt(RodeoRecord::getForbiddenSpeech)
                                    .sum());
                            // 计算惩罚得分：禁言时长 ÷ 开枪总数（分母为0时计负分）
                            stats.setScore((stats.getShotCount() > 0)
                                    ? (double) stats.getTotalForbidden() / stats.getShotCount()
                                    : -stats.getTotalForbidden());
                            return stats;
                        })
                ));

        // 获取所有参赛者
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        List<String> allPlayers = Arrays.asList(players);

        // 创建用于存储结果的DTO列表
        List<RodeoEndGameInfoDto> recordEndGameInfoDtos = new ArrayList<>();
        allPlayers.forEach(player -> {
            RodeoEndGameInfoDto dto = new RodeoEndGameInfoDto();
            dto.setPlayer(player);

            PlayerStats stats = playerStatsMap.getOrDefault(player, new PlayerStats());
            dto.setPenalty(stats.getScore());           // 保留精确小数
            dto.setShotCount(stats.getShotCount());
            dto.setForbiddenSpeech(stats.getTotalForbidden());
            recordEndGameInfoDtos.add(dto);
        });

        // 按得分升序排序（0分排第一，负分随后）
        recordEndGameInfoDtos.sort(Comparator.comparingDouble(RodeoEndGameInfoDto::getScore));

        // 构建消息内容
//        StringBuilder message = new StringBuilder("[" + rodeo.getVenue() + "]结束，排名如下：\n");
        Message m = new PlainText(String.format("[%s]结束，排名如下\r\n", rodeo.getVenue()));
        int rank = 1;
        for (RodeoEndGameInfoDto dto : recordEndGameInfoDtos) {
            m =  m.plus(rank++ + ".");
            m = m.plus(new At(Long.parseLong(dto.getPlayer())));
            m = m.plus(" - 得分: ");
            m = m.plus(String.format("%.1f", dto.getPenalty()));
            m = m.plus(" (禁言总时长: "  + dto.getForbiddenSpeech() + "秒, 开枪次数: " + dto.getShotCount() + ") \r\n");
        }
        // 发送消息
        group.sendMessage(m);

        try {
            cancelPermission(rodeo);
        } catch (Exception e) {

        } finally {
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
            log.info("轮盘授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.grantDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.ROULETTE_PERMISSION);
        }

    }

    @Override
    public void cancelPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            log.info("轮盘取消授权：groupId: {}, player：{}", rodeo.getGroupId(), player);
            PermissionManager.revokeDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.ROULETTE_PERMISSION);
        }
    }

}
