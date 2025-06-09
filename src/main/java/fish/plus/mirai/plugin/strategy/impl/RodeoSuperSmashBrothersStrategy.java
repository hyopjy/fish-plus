package fish.plus.mirai.plugin.strategy.impl;


import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.manager.PermissionManager;
import fish.plus.mirai.plugin.manager.RodeoManager;
import fish.plus.mirai.plugin.obj.dto.RodeoEndGameInfoDto;
import fish.plus.mirai.plugin.obj.dto.RodeoRecordGameInfoDto;
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
            Long playerId = Long.parseLong(str);
            m = m.plus(new At(playerId).getDisplay(group));
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
                .collect(Collectors.groupingBy(RodeoRecord::getPlayer));

        // 获取所有参赛者列表
        String[] playersArray = rodeo.getPlayers().split(Constant.MM_SPILT);
        List<String> allPlayers = Arrays.asList(playersArray);

        List<RodeoEndGameInfoDto> dtos = new ArrayList<>();

        // 遍历所有参赛者生成统计数据（包含无记录的玩家）
        allPlayers.forEach(player -> {
            List<RodeoRecord> playerRecords = recordsByPlayer.getOrDefault(player, Collections.emptyList());

            RodeoEndGameInfoDto dto = new RodeoEndGameInfoDto();
            dto.setPlayer(player);

            // 计算获胜次数（只统计winFlag=1的记录）
            int winCount = (int) playerRecords.stream()
                    .filter(r -> r.getWinFlag() == 1)
                    .count();
            dto.setScore(winCount);

            // 计算总禁言时长
            int totalForbidden = playerRecords.stream()
                    .mapToInt(RodeoRecord::getForbiddenSpeech)
                    .sum();
            dto.setForbiddenSpeech(totalForbidden);

            dtos.add(dto);
        });

        // 构建排行榜（按分数降序）
        List<RodeoEndGameInfoDto> scoreRanking = dtos.stream()
                .sorted(Comparator.comparingInt(RodeoEndGameInfoDto::getScore).reversed())
                .collect(Collectors.toList());

        // 构建禁言榜（按时长降序）
        List<RodeoEndGameInfoDto> forbiddenRanking = dtos.stream()
                .sorted(Comparator.comparingInt(RodeoEndGameInfoDto::getForbiddenSpeech).reversed())
                .collect(Collectors.toList());

        // 构建消息内容
        StringBuilder message = new StringBuilder();
        message.append("[").append(rodeo.getVenue()).append("]比赛结束\n\n");

        // 添加得分排行榜
        message.append("🏆 得分排行榜：\n");
        for (int i = 0; i < scoreRanking.size(); i++) {
            RodeoEndGameInfoDto dto = scoreRanking.get(i);
            String playerName = new At(Long.parseLong(dto.getPlayer())).getDisplay(group);
            message.append(i + 1).append(". ").append(playerName)
                    .append(" - ").append(dto.getScore()).append("分\n");
        }

        // 添加禁言排行榜
        message.append("\n🔇 禁言时长排行榜：\n");
        for (int i = 0; i < forbiddenRanking.size(); i++) {
            RodeoEndGameInfoDto dto = forbiddenRanking.get(i);
            String playerName = new At(Long.parseLong(dto.getPlayer())).getDisplay(group);
            message.append(i + 1).append(". ").append(playerName)
                    .append(" - ").append(dto.getForbiddenSpeech()).append("秒\n");
        }

        // 发送消息
        group.sendMessage(new PlainText(message.toString()));

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
