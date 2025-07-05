package fish.plus.mirai.plugin.obj.dto;

import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import lombok.Data;

@Data
public class RodeoEndGameInfoDto {

    private String player;

     // 系统计算赢的场次
    private int score;
    // 禁言时长
    private int forbiddenSpeech;

    //
    private int integral;  // 积分值

    // 轮盘使用的
    private Double penalty;

    private int shotCount = 0;
}
