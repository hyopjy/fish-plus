package fish.plus.mirai.plugin.entity.rodeo;

import cn.chahuyun.hibernateplus.HibernateFactory;
import fish.plus.mirai.plugin.util.HibernateUtil;
import fish.plus.mirai.plugin.util.Log;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "RodeoRecord")
@Table
@Getter
@Setter
public class RodeoRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 场次id
     */
    private Long rodeoId;

    /**
     * 玩家
     */
    private String player;

    /**
     * 禁言时长
     */
    private Integer ForbiddenSpeech;

    /**
     * 第几局
     */
    private Integer turns;

    /**
     * 比赛描述
     */
    @Column(columnDefinition = "text")
    private String rodeoDesc;

    private int winFlag;

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
