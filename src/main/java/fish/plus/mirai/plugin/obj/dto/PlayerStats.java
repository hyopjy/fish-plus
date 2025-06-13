package fish.plus.mirai.plugin.obj.dto;


import lombok.Data;

@Data
public class PlayerStats {
    int shotCount = 0;
    int totalForbidden = 0;
    double score = 0.0;
}
