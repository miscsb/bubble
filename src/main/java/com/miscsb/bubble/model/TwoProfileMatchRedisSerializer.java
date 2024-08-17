package com.miscsb.bubble.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;
import org.springframework.util.Assert;

public class TwoProfileMatchRedisSerializer implements RedisSerializer<TwoProfileMatch> {
    private static TwoProfileMatchRedisSerializer instance;
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private TwoProfileMatchRedisSerializer() {}

    @Override
    public byte[] serialize(TwoProfileMatch value) throws SerializationException {
        Assert.notNull(value, "TwoProfileMatch must not be null");
        Assert.hasText(value.profile1(), "TwoProfileMatch must have a profile1");
        Assert.hasText(value.profile2(), "TwoProfileMatch must have a profile2");
        byte[] buf = new byte[value.profile1().length() + value.profile2().length() + 1];
        if (value.profile1().compareTo(value.profile2()) < 0) {
            System.arraycopy(value.profile1().getBytes(), 0, buf, 0, value.profile1().length());
            System.arraycopy(value.profile2().getBytes(), 0, buf, value.profile1().length() + 1, value.profile2().length());
        } else {
            System.arraycopy(value.profile2().getBytes(), 0, buf, 0, value.profile2().length());
            System.arraycopy(value.profile1().getBytes(), 0, buf, value.profile2().length() + 1, value.profile1().length());
        }
        buf[value.profile1().length()] = '#';
        return buf;
    }

    @Override
    public TwoProfileMatch deserialize(byte[] bytes) throws SerializationException {
        logger.warn("Deserializing object of type TwoProfileMatch {}", new String(bytes));
        int i = 0;
        while (bytes[i] != '#') i++;
        return new TwoProfileMatch(new String(bytes, 0, i), new String(bytes, i + 1, bytes.length - i - 1));
    }

    public static TwoProfileMatchRedisSerializer instance() {
        if (instance == null) {
            instance = new TwoProfileMatchRedisSerializer();
        }
        return instance;
    }
}
