package com.gome.fup.easyid.util;

import redis.clients.jedis.Jedis;

/**
 * jedis命令接口
 * Created by fupeng-ds on 2017/8/9.
 */
public interface JedisCommand<T> {

    T command(Jedis jedis);
}
