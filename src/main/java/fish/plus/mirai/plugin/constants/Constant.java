package fish.plus.mirai.plugin.constants;


import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;

/**
 * 固定常量
 *
 * @author Moyuyanli
 * @date 2022/11/14 12:43
 */
public interface Constant {


    DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");


    String SPILT = "-";

    String SPILT2 = "|";

    String MM_SPILT = ",";

}

