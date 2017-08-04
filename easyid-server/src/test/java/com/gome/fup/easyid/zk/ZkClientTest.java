package com.gome.fup.easyid.zk;

import com.gome.fup.easyid.snowflake.Snowflake;
import com.gome.fup.easyid.util.KryoUtil;
import org.apache.zookeeper.KeeperException;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;

/**
 * Created by fupeng-ds on 2017/8/3.
 */
public class ZkClientTest {

    @Test
    public void testRegister() throws InterruptedException, IOException, KeeperException {
        ZkClient client = new ZkClient("127.0.0.1:2181");
        client.register("192.168.56.102");
    }

    @Test
    public void testIncrease() throws InterruptedException, IOException, KeeperException {
        ZkClient client = new ZkClient("127.0.0.1:2181");
        int increase = client.increase("testRegister");
        System.out.println(increase);
    }

    @Test
    public void testDecrease() throws InterruptedException, IOException, KeeperException {
        ZkClient client = new ZkClient("127.0.0.1:2181");
        int increase = client.decrease("testRegister");
        System.out.println(increase);
    }

    @Test
    public void testRedis() {
        String key = "EasyID";
        final byte[] bytes = KryoUtil.objToByte(key);
        Snowflake snowflake = new Snowflake();
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        RedisTemplate redisTemplate = context.getBean(RedisTemplate.class);
        long id = snowflake.nextId();
        System.out.println(id);
        final byte[] value = KryoUtil.objToByte(id);
        redisTemplate.execute(new RedisCallback() {
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.rPush(bytes,value);
            }
        });

    }
}
