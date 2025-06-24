package fish.plus.mirai.plugin.obj.dto;


import fish.plus.mirai.plugin.strategy.RodeoStrategy;
import lombok.Data;

@Data
public class PlayerStats {
    int shotCount = 0;
    int totalForbidden = 0;
    Double penalty = RodeoStrategy.DEFAULT_PENALTY;
}
