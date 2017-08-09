package com.gome.fup.easyid.util;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

/**
 * Created by fupeng-ds on 2017/8/9.
 */
public class JedisUtil {

    private static JedisPool jedisPool;

    private String host;

    private int port;

    private static final JedisUtil util = new JedisUtil();

    /**
     * 有效时间半个小时
     */
    private int EXPIRE = 30 * 60;

    private JedisUtil() {
    }

    public synchronized static JedisUtil newInstance(String host, int port) {
        if (jedisPool == null) {
            init(host, port);
        }
        return util;
    }

    private static void init(String host, int port) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxIdle(512);
        config.setMaxTotal(1024);
        config.setMaxWaitMillis(30000);
        config.setTestOnBorrow(true);
        jedisPool = new JedisPool(config, host, port);
    }

    private Jedis getJedis() {
        return jedisPool.getResource();
    }

    private void returnResource(Jedis jedis) {
        jedisPool.returnResource(jedis);
    }

    public String set(final String key, final String value) {
        return (String)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                String set = jedis.set(key, value);
                jedis.expire(key, EXPIRE);
                return set;
            }
        });
    }

    public String get(final String key) {
        return (String)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.get(key);
            }
        });
    }

    public Boolean exists(final String key) {
        return (Boolean)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.exists(key);
            }
        });
    }

    public Long llen(final String key) {
        return (Long)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.llen(key);
            }
        });
    }

    public String lpop(final String key) {
        return (String)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.lpop(key);
            }
        });
    }

    public Long rpush(final String key, final String value) {
        return (Long)command(new JedisCommand<Object>() {
            public Long command(Jedis jedis) {
                return jedis.rpush(key, value);
            }
        });
    }

    public Long setnx(final String key, final String value) {
        return (Long)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.setnx(key, value);
            }
        });
    }

    public Long expire(final String key, final int seconds) {
        return (Long)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.expire(key, seconds);
            }
        });
    }

    public Long del(final String key) {
        return (Long)command(new JedisCommand<Object>() {
            public Object command(Jedis jedis) {
                return jedis.del(key);
            }
        });
    }

    private Object command(JedisCommand<Object> command) {
        Jedis jedis = getJedis();
        try {
            return command.command(jedis);
        } finally {
            returnResource(jedis);
        }
    }
}
