package fish.plus.mirai.plugin.obj.dto;

import lombok.Data;

import java.io.Serial;
import java.io.Serializable;

@Data
public class MessageContentDTO implements Serializable {

    @Serial
    private static final long serialVersionUID = -711248153042937522L;
    private String messageType;

    private Long groupId;

}
