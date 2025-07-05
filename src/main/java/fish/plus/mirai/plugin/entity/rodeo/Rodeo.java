package fish.plus.mirai.plugin.entity.rodeo;


import cn.chahuyun.hibernateplus.HibernateFactory;
import fish.plus.mirai.plugin.constants.Constant;
import fish.plus.mirai.plugin.util.Log;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;
import java.util.Arrays;


/**
 * 决斗、轮盘、大乱斗
 */
@Entity(name = "Rodeo")
@Table
@Getter
@Setter

public class Rodeo implements Serializable {

    private static final long serialVersionUID = -5567255189132869882L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 玩法（决斗、轮盘、大乱斗）
    private String playingMethod;

    // 群组id
    private Long groupId;

    // 场次名称
    @Column(columnDefinition = "text")
    private String venue;


    // 配置日期   2024-08-23
    private String day;

    // 时间段  10:15:00
    private String startTime;

    // 时间段
    private String endTime;


    // 选手 -- 按照逗号分割的多方
    @Column(columnDefinition = "text")
    private String players;

    // 局数
    private int round;

    private int running;

    private int giveProp;

    // 奖励道具编码
    private String propCode;

    private String propName;


    public Rodeo() {
    }


    public boolean saveOrUpdate() {
        try {
            HibernateFactory.merge(this);
        } catch (Exception e) {
            Log.error("神秘商人:更新", e);
            return false;
        }

        return true;
    }

    public void remove() {
        HibernateFactory.delete(this);
    }


    public String getPlayers() {
        // 如果players为空，直接返回
        if (players == null || players.isEmpty()) {
            return "";
        }

        // 去掉开头和结尾的逗号
        if (players.startsWith(Constant.MM_SPILT) && players.endsWith(Constant.MM_SPILT)) {
            return players.substring(1, players.length() - 1);
        }
        // 只去掉开头的逗号
        else if (players.startsWith(Constant.MM_SPILT)) {
            return players.substring(1);
        }
        // 只去掉结尾的逗号
        else if (players.endsWith(Constant.MM_SPILT)) {
            return players.substring(0, players.length() - 1);
        }
        // 没有逗号的情况
        return players;
    }



    public String[] getPlayerIds() {
        if (StringUtils.isBlank(players)) {
            return new String[0];
        }

        // 分割字符串时忽略空值并返回数组
        return Arrays.stream(players.split(Constant.MM_SPILT))
                .filter(StringUtils::isNotBlank)
                .toArray(String[]::new);
    }


}
