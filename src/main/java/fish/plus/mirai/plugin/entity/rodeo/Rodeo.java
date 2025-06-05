package fish.plus.mirai.plugin.entity.rodeo;


import cn.chahuyun.hibernateplus.HibernateFactory;
import fish.plus.mirai.plugin.util.Log;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

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
    @GeneratedValue(strategy = GenerationType.AUTO)
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


    public Rodeo() {
    }

    public Rodeo(Long groupId, String venue,
                 String day, String startTime, String endTime,
                 String players, int round, String playingMethod) {
        this.groupId = groupId;
        this.venue = venue;
        this.day = day;
        this.startTime = startTime;
        this.endTime = endTime;
        this.players = players;
        this.round = round;
        this.playingMethod = playingMethod;
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


}
