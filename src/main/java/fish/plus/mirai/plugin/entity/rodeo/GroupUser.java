package fish.plus.mirai.plugin.entity.rodeo;

import cn.chahuyun.hibernateplus.HibernateFactory;
import fish.plus.mirai.plugin.util.Log;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity(name = "GroupUser")
@Table
@Getter
@Setter
public class GroupUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long groupId;

    private Long userId;

    private String userNick;

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
