package fish.plus.mirai.plugin.obj.dto;

import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import lombok.Data;

@Data
public class RodeoEndGameInfoDto {

    private String player;

    private int score = RodeoStrategy.DEFAULT_SCORE;

    private int forbiddenSpeech;

    private Double penalty;

    private int shotCount = 0;
}
