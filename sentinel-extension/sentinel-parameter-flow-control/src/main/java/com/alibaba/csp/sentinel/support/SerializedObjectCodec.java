package com.alibaba.csp.sentinel.support;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import io.lettuce.core.codec.RedisCodec;

/**
 * @author Wolken Hu
 * @version $Id$
 * @since 2019/12/5 14:17
 */
public class SerializedObjectCodec implements RedisCodec<Object, Object> {

    private byte[] prefix = null;

    public SerializedObjectCodec() {
        this(null);
    }

    public SerializedObjectCodec(String keyPrefix) {
        if (keyPrefix != null) {
            try {
                this.prefix = keyPrefix.getBytes("UTF-8");
            } catch (UnsupportedEncodingException ignore) {
            }
        }
    }

    @Override
    public Object decodeKey(ByteBuffer byteBuffer) {
        return decode(byteBuffer);
    }

    @Override
    public Object decodeValue(ByteBuffer byteBuffer) {
        return decode(byteBuffer);
    }

    @Override
    public ByteBuffer encodeKey(Object o) {
        return encode(o);
    }

    @Override
    public ByteBuffer encodeValue(Object o) {
        return encode(o);
    }

    private ByteBuffer encode(Object o) {
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(bytes);
            os.writeObject(o);
            byte[] raw = bytes.toByteArray();
            byte[] fin;
            if (prefix != null) {
                fin = Arrays.copyOf(prefix, prefix.length + raw.length);
                System.arraycopy(raw, 0, fin, prefix.length, raw.length);
            } else {
                fin = raw;
            }
            return ByteBuffer.wrap(fin);
        } catch (IOException e) {
            return null;
        }
    }

    private Object decode(ByteBuffer byteBuffer) {
        try {
            byte[] raw = new byte[byteBuffer.remaining()];
            byteBuffer.get(raw);
            byte[] fin;
            if (prefix != null) {
                fin = Arrays.copyOfRange(raw, prefix.length, raw.length);
            } else {
                fin = raw;
            }
            ObjectInputStream is = new ObjectInputStream(new ByteArrayInputStream(fin));
            return is.readObject();
        } catch (Exception e) {
            return null;
        }
    }

}
