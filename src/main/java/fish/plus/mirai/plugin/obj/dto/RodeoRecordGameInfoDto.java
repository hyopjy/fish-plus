package fish.plus.mirai.plugin.obj.dto;

import lombok.Data;

import java.util.Set;

@Data
public class RodeoRecordGameInfoDto {


    private String winner;

    private String loser;

    Integer ForbiddenSpeech;

    String rodeoDesc;

    Set<Long> atUser;
}

