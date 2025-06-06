package fish.plus.mirai.plugin.strategy.impl;

import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.entity.rodeo.Rodeo;
import fish.plus.mirai.plugin.entity.rodeo.RodeoRecord;
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

        // todo 开始轮盘权限
    }

    @Override
    public void record(Rodeo rodeo, RodeoRecordGameInfoDto dto) {
        // 存入输家
        RodeoRecord loseRodeoRecord = new RodeoRecord();
        loseRodeoRecord.setRodeoId(rodeo.getId());
        loseRodeoRecord.setPlayer(dto.getLoser());
        loseRodeoRecord.setForbiddenSpeech(dto.getForbiddenSpeech());
        loseRodeoRecord.setTurns(null);
        loseRodeoRecord.setRodeoDesc(dto.getRodeoDesc());
        loseRodeoRecord.saveOrUpdate();
        loseRodeoRecord.saveOrUpdate();
    }

    @Override
    public void endGame(Rodeo rodeo) {
        Group group = getBotGroup(rodeo.getGroupId());
        if(group == null){
            return;
        }
        Long rodeoId = rodeo.getId();
        // 所有输的记录
        List<RodeoRecord> records = RodeoRecordManager.getRecordsByRodeoId(rodeoId);


        // Create the map grouping records by player
        Map<String, List<RodeoRecord>> sumByPlayer = records.stream()
                .collect(Collectors.groupingBy(RodeoRecord::getPlayer));
        List<RodeoEndGameInfoDto> recordEndGameInfoDtos = new ArrayList<RodeoEndGameInfoDto>();
        sumByPlayer.forEach((player, record)->{
            RodeoEndGameInfoDto dto = new RodeoEndGameInfoDto();
            dto.setPlayer(player);
            dto.setScore(record.size());
            dto.setForbiddenSpeech(record.stream().filter(Objects::nonNull).mapToInt(RodeoRecord::getForbiddenSpeech).sum());
            recordEndGameInfoDtos.add(dto);
        });
        // 获取所有参赛者
        String[] players = rodeo.getPlayers().split(Constant.MM_SPILT);
        Map<String, RodeoEndGameInfoDto> dtoMap = recordEndGameInfoDtos.stream()
                .collect(Collectors.toMap(RodeoEndGameInfoDto::getPlayer, dto -> dto));
        // 将 players 数组转换为列表
        List<String> playerList = Arrays.asList(players);

        // 按照 dtoMap 中的键排序
        playerList.sort(Comparator.comparingInt(player -> {
            // 若 dtoMap 中存在该 player，则返回其索引，否则返回最大值以确保在末尾
            return dtoMap.containsKey(player) ? new ArrayList<>(dtoMap.keySet()).indexOf(player) : Integer.MAX_VALUE;
        }));

        StringBuilder message = new StringBuilder("[比赛场次名]结束，得分表如下：\r\n");
        playerList.forEach(player -> {
            RodeoEndGameInfoDto dto = dtoMap.get(player);
            String playerName = new At(Long.parseLong(player)).getDisplay(group);
            int score = 0;
            if (Objects.nonNull(dto)) {
                score = dto.getScore();
            }
            message.append(playerName).append("-").append(score);
        });

        dtoMap.forEach((player, dto) -> {
            String playerName = new At(Long.parseLong(player)).getDisplay(group);
            message.append(playerName).append("共被禁言[").append(dto.getForbiddenSpeech()+"]");
        });
        group.sendMessage(new PlainText(message));

        // todo 关闭轮盘
        RodeoManager.removeExpRodeoList();
    }

    @Override
    public RodeoRecordGameInfoDto analyzeMessage(String message) {
        return null;
    }

}
