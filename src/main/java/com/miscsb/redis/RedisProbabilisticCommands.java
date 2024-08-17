package com.miscsb.redis;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.output.BooleanOutput;
import io.lettuce.core.output.CommandOutput;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;

import javax.annotation.Nullable;

public interface RedisProbabilisticCommands {

    @Nullable
    Boolean cfReserve(String key, Integer size);

    @Nullable
    Boolean cfAdd(String key, String value);

    @Nullable
    Boolean cfExists(String key, String value);

    @Nullable
    Boolean cfDel(String key, String value);

    static RedisProbabilisticCommands fromLettuceConnection(LettuceConnection lettuce) {
        CommandOutput<?, ?, ?> output = new BooleanOutput<>(ByteArrayCodec.INSTANCE);

        return new RedisProbabilisticCommands() {
            @Nullable
            @Override
            public Boolean cfReserve(String key, Integer size) {
                Object res = lettuce.execute("CF.RESERVE", output, key.getBytes(), size.toString().getBytes());
                return (Boolean) res;
            }

            @Nullable
            @Override
            public Boolean cfAdd(String key, String value) {
                Object res = lettuce.execute("CF.ADD", output, key.getBytes(), value.getBytes());
                return (Boolean) res;
            }

            @Nullable
            @Override
            public Boolean cfExists(String key, String value) {
                Object res = lettuce.execute("CF.EXISTS", output, key.getBytes(), value.getBytes());
                return (Boolean) res;
            }

            @Nullable
            @Override
            public Boolean cfDel(String key, String value) {
                Object res = lettuce.execute("CF.DEL", output, key.getBytes(), value.getBytes());
                return (Boolean) res;
            }
        };
    }
}
