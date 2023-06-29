package org.github.ponking66.protoctl;

import lombok.Data;
import lombok.ToString;
import lombok.experimental.Accessors;

/**
 * @author pony
 * @date 2023/4/27
 */
@Data
@Accessors(chain = true)
@ToString
public class Header {

    /**
     * 魔数 (暂时用不上)
     */
    private int magic = 0xbb9527;

    /**
     * 消息长度
     */
    private int length;

    /**
     * 会话 ID (暂时用不上)
     */
    private long sessionID;

    /**
     * 消息类型
     */
    private byte type;

    /**
     * 版本号 (暂时用不上)
     */
    private byte version = 0x01;

    /**
     * 状态
     */
    private int status = Status.SUCCESS.getVal();

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
