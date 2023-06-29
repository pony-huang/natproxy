package org.github.ponking66.util;

import io.netty.buffer.ByteBuf;
import org.jboss.marshalling.*;

import java.io.IOException;

/**
 * Marshaller 序列化与反序列化工具
 *
 * @author pony
 * @date 2023/6/29
 */
public class MarshallerUtils {

    public static final class MarshallingCodecFactory {
        private static final MarshallerFactory marshallerFactory;

        private static final MarshallingConfiguration configuration = new MarshallingConfiguration();

        static {
            marshallerFactory = Marshalling.getProvidedMarshallerFactory("river");
            configuration.setVersion(4);
        }

        public static Marshaller buildMarshalling() throws IOException {
            return marshallerFactory.createMarshaller(configuration);
        }

        public static Unmarshaller buildUnMarshalling() throws IOException {
            return marshallerFactory.createUnmarshaller(configuration);
        }
    }
    private static final ThreadLocal<Marshaller> M_POOL = ThreadLocal.withInitial(() -> {
        try {
            return MarshallingCodecFactory.buildMarshalling();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });

    private static final ThreadLocal<Unmarshaller> UM_POOL = ThreadLocal.withInitial(() -> {
        try {
            return MarshallingCodecFactory.buildUnMarshalling();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    });

    private MarshallerUtils() {

    }

    public static Object readObject(ByteBuf buf) throws Exception {
        ByteInput input = new ChannelBufferByteInput(buf);
        try (Unmarshaller unmarshaller = UM_POOL.get()) {
            unmarshaller.start(input);
            Object obj = unmarshaller.readObject();
            unmarshaller.finish();
            return obj;
        }
    }

    public static void writeObject(Object msg, ByteBuf out) throws Exception {
        try (Marshaller marshaller = M_POOL.get()) {
            ChannelBufferByteOutput output = new ChannelBufferByteOutput(out);
            marshaller.start(output);
            marshaller.writeObject(msg);
            marshaller.finish();
        }
    }

    private record ChannelBufferByteInput(ByteBuf buffer) implements ByteInput {

        @Override
        public void close() throws IOException {
            // nothing to do
        }

        @Override
        public int available() throws IOException {
            return buffer.readableBytes();
        }

        @Override
        public int read() throws IOException {
            if (buffer.isReadable()) {
                return buffer.readByte() & 0xff;
            }
            return -1;
        }

        @Override
        public int read(byte[] array) throws IOException {
            return read(array, 0, array.length);
        }

        @Override
        public int read(byte[] dst, int dstIndex, int length) throws IOException {
            int available = available();
            if (available == 0) {
                return -1;
            }

            length = Math.min(available, length);
            buffer.readBytes(dst, dstIndex, length);
            return length;
        }

        @Override
        public long skip(long bytes) throws IOException {
            int readable = buffer.readableBytes();
            if (readable < bytes) {
                bytes = readable;
            }
            buffer.readerIndex((int) (buffer.readerIndex() + bytes));
            return bytes;
        }


    }


    private record ChannelBufferByteOutput(ByteBuf buffer) implements ByteOutput {


        public ChannelBufferByteOutput {
        }

        @Override
        public void close() throws IOException {
            // Nothing to do
        }

        @Override
        public void flush() throws IOException {
            // nothing to do
        }

        @Override
        public void write(int b) throws IOException {
            buffer.writeByte(b);
        }

        @Override
        public void write(byte[] bytes) throws IOException {
            buffer.writeBytes(bytes);
        }

        @Override
        public void write(byte[] bytes, int srcIndex, int length) throws IOException {
            buffer.writeBytes(bytes, srcIndex, length);
        }

        @Override
        public ByteBuf buffer() {
            return buffer;
        }
    }
}
