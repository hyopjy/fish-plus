package fish.plus.mirai.plugin.strategy.impl;

import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
import fish.plus.mirai.plugin.manager.PermissionManager;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.obj.dto.RodeoEndGameInfoDto;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
import net.mamoe.mirai.contact.Group;
import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import net.mamoe.mirai.message.data.At;
import net.mamoe.mirai.message.data.Message;
import net.mamoe.mirai.message.data.PlainText;
import fish.plus.mirai.plugin.manager.RodeoRecordManager;

import java.util.*;
import java.util.stream.Collectors;

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
            m = m.plus(new At(playerId).getDisplay(group));
        }
        m = m.plus(message2);

        group.sendMessage(m);

    }

    @Override
    public void record(Rodeo rodeo, RodeoRecordGameInfoDto dto) {
        // 存入输家
        RodeoRecord loserodeorecord = new RodeoRecord();
        loserodeorecord.setRodeoId(rodeo.getId());
        loserodeorecord.setPlayer(dto.getLoser());
        loserodeorecord.setForbiddenSpeech(dto.getForbiddenSpeech());
        loserodeorecord.setTurns(null);
        loserodeorecord.setRodeoDesc(dto.getRodeoDesc());
        loserodeorecord.setWinFlag(0);
        loserodeorecord.saveOrUpdate();
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
        Map<String, List<RodeoRecord>> sumByPlayer = records.stream()
                .collect(Collectors.groupingBy(RodeoRecord::getPlayer));

        // 创建用于存储结果的 DTO 列表
        List<RodeoEndGameInfoDto> recordEndGameInfoDtos = new ArrayList<>();

        // 遍历每个玩家的记录并计算总分和禁言时长
        sumByPlayer.forEach((player, record) -> {
            RodeoEndGameInfoDto dto = new RodeoEndGameInfoDto();
            dto.setPlayer(player);
            dto.setScore(record.size()); // 总局数作为分数
            dto.setForbiddenSpeech(record.stream()
                    .filter(Objects::nonNull)
                    .mapToInt(RodeoRecord::getForbiddenSpeech)
                    .sum()); // 累加禁言时长
            recordEndGameInfoDtos.add(dto);
        });

        // 获取所有参赛者
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        List<String> allPlayers = Arrays.asList(players);

        // 将未参赛的玩家添加到 DTO 列表中，并设置默认值
        allPlayers.forEach(player -> {
            if (!sumByPlayer.containsKey(player)) {
                RodeoEndGameInfoDto dto = new RodeoEndGameInfoDto();
                dto.setPlayer(player);
                dto.setScore(0); // 未参赛，分数为 0
                dto.setForbiddenSpeech(0); // 未参赛，禁言时长为 0
                recordEndGameInfoDtos.add(dto);
            }
        });

        // 按禁言时长倒序排序
        recordEndGameInfoDtos.sort(Comparator.comparingInt(RodeoEndGameInfoDto::getForbiddenSpeech).reversed());

        // 构建消息内容
        StringBuilder message = new StringBuilder("[" + rodeo.getVenue() + "]结束，按禁言时长倒序排名如下：\r\n");

        recordEndGameInfoDtos.forEach(dto -> {
            String playerName = new At(Long.parseLong(dto.getPlayer())).getDisplay(group);
            message.append(playerName)
                    .append(" - 禁言时长: ")
                    .append(dto.getForbiddenSpeech())
                    .append("秒 - 得分: ")
                    .append(dto.getScore())
                    .append("\r\n");
        });

        // 发送消息
        group.sendMessage(new PlainText(message.toString()));

        // todo 关闭轮盘
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
            PermissionManager.grantDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.ROULETTE_PERMISSION);
        }
    }

    @Override
    public void cancelPermission(Rodeo rodeo) {
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        for(String player: players){
            PermissionManager.revokeDuelPermission(rodeo.getGroupId(), Long.parseLong(player), PermissionManager.ROULETTE_PERMISSION);
        }
    }

}
