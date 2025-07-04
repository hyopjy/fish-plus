package fish.plus.mirai.plugin.util;

import cn.chahuyun.hibernateplus.Configuration;
import cn.chahuyun.hibernateplus.DriveType;
import cn.chahuyun.hibernateplus.HibernatePlusService;
import fish.plus.mirai.plugin.JavaPluginMain;
import org.hibernate.HibernateException;
import org.hibernate.SessionFactory;

import static fish.plus.mirai.plugin.JavaPluginMain.SERVER_DATA_SOURCE_IP;

/**
 * 说明
 *
 * @author Moyuyanli
 * @Description :hibernate
 * @Date 2022/7/30 22:47
 */
public class HibernateUtil {


    /**
     * 数据库连接前缀
     */
    private static final String SQL_PATH_PREFIX = "jdbc:h2:file:";

    /**
     * 会话工厂
     */
    public static SessionFactory factory = null;

    private HibernateUtil() {

    }

    /**
     * Hibernate初始化
     *
     * @param configuration Configuration
     * @author Moyuyanli
     * @date 2022/7/30 23:04
     */
    public static void init(JavaPluginMain main) {
        Configuration configuration = HibernatePlusService.createConfiguration(main.getClass());
        configuration.setPackageName("fish.plus.mirai.plugin.entity");
        configuration.setShowSql(false);
        configuration.setFormatSql(false);
        try {
            configuration.setDriveType(DriveType.MYSQL);
            configuration.setAddress(SERVER_DATA_SOURCE_IP + "/fish-plus");
            configuration.setUser("root");
            configuration.setPassword("zyjy110.");
            HibernatePlusService.loadingService(configuration);
        } catch (HibernateException e) {
            Log.error("请删除data中的HuYanEconomy.mv.db后重新启动！", e);
            return;
        }
        Log.info("MYSQL数据库初始化成功!");
    }


}
