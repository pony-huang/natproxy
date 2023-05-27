package org.github.ponking66.protoctl;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

import java.util.HashMap;
import java.util.Map;

/**
 * @author pony
 * @date 2023/4/27
 */
@Data
@Accessors(chain = true)
@ToString
public class Header {

    /**
     * 魔数
     */
    private int magic = 0xabef9876;

    /**
     * 消息长度
     */
    private int length;

    /**
     * 会话ID
     */
    private long sessionID;

    /**
     * 消息类型
     */
    private byte type;

    /**
     * 版本号
     */
    private byte version = 0x01;

    /**
     * 状态
     */
    private int status = Status.SUCCESS.getVal();

    /**
     * 扩展字段
     */
    private Map<String, Object> extended = new HashMap<String, Object>();

    public enum Status {
        SUCCESS(200),

        FAILED(500);

        private final int val;

        Status(int val) {
            this.val = val;
        }


        public int getVal() {
            return val;
        }
    }

}
