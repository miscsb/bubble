package com.miscsb.redis;

import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnection;
import org.springframework.util.Assert;

public class RedisCuckooFilter {

    private final String key;
    private final RedisProbabilisticCommands commands;

    public RedisCuckooFilter(String key, RedisConnectionFactory connectionFactory) {
        Assert.hasText(key, "key must not be empty");
        Assert.notNull(connectionFactory, "connectionFactory must not be null");
        this.key = key;
        this.commands = RedisProbabilisticCommands.fromLettuceConnection((LettuceConnection) connectionFactory.getConnection());
    }

    public Boolean reserve(Integer size) {
        return commands.cfReserve(this.key, size);
    }

    public Boolean add(String value) {
        return commands.cfAdd(this.key, value);
    }

    public Boolean del(String value) {
        return commands.cfDel(this.key, value);
    }

    public Boolean exists(String value) {
        return commands.cfExists(this.key, value);
    }

}
