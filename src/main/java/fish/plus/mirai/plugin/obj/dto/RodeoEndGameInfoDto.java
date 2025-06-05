package fish.plus.mirai.plugin.obj.dto;

import lombok.Data;

@Data
public class RodeoEndGameInfoDto {

    private String player;

    private int score;

    private int forbiddenSpeech;
}
