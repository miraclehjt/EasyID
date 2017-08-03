package com.gome.fup.easyid.id;

import com.gome.fup.easyid.exception.NoMoreValueInRedisException;
import com.gome.fup.easyid.util.KryoUtil;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.Serializable;

/**
 * 客户端ID生成类
 * Created by fupeng-ds on 2017/8/3.
 */
public class EasyID {

    /**
     * redis key
     */
    private final String REDIS_LIST_NAME = "EasyID";

    /**
     * redis队列中最低ID数量，低于此数量时，服务端开始生成新的ID并存入redis队列
     */
    private final long REDIS_LIST_MIN_SIZE = 300l;

    /**
     * 服务端开始生成新的ID的开关
     */
    private volatile boolean flag = false;

    private RedisTemplate<Serializable, Serializable> redisTemplate;

    /**
     * 获取id
     * @return
     */
    public long nextId() {
        return nextIds(1)[0];
    }

    /**
     * 获取count数量的id集合
     * @param count
     * @return
     */
    public Long[] nextIds(final int count) {
        return redisTemplate.execute(new RedisCallback<Long[]>() {
            public Long[] doInRedis(RedisConnection connection) throws DataAccessException {
                byte[] key = KryoUtil.objToByte(REDIS_LIST_NAME);
                Long len = connection.lLen(key);
                if (count > len.intValue()) {
                    try {
                        throw new NoMoreValueInRedisException("没有足够的值");
                    } catch (NoMoreValueInRedisException e) {
                        e.printStackTrace();
                    }
                }
                if (len < REDIS_LIST_MIN_SIZE) {
                    //打开开关，服务端需要生产更多的id
                    flag = true;
                }
                Long[] ids = new Long[count];
                for (int i = 0; i < count; i++) {
                    byte[] bytes = connection.lPop(key);
                    ids[i] = KryoUtil.byteToObj(bytes, Long.class);
                }
                return ids;
            }
        });
    }

    public RedisTemplate getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

}
