package com.gome.fup.easyid.handler;

import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.snowflake.Snowflake;
import com.gome.fup.easyid.util.Constant;
import com.gome.fup.easyid.util.IpUtil;
import com.gome.fup.easyid.util.KryoUtil;
import com.gome.fup.easyid.util.MessageType;
import com.gome.fup.easyid.zk.ZkClient;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;

import java.io.Serializable;

/**
 * 请求处理handler
 * Created by fupeng-ds on 2017/8/3.
 */
public class Handler extends SimpleChannelInboundHandler<Request> {

    private RedisOperations<Serializable, Serializable> redisTemplate;

    private Snowflake snowflake;

    private ZkClient zkClient;

    private int redis_list_size;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        if (request.getType() == MessageType.REQUEST_TYPE_CREATE) {
            String ip = IpUtil.getLocalHost();
            zkClient.increase(ip);
            redisTemplate.execute(new RedisCallback<Object>() {
                public Object doInRedis(RedisConnection connection) throws DataAccessException {
                    byte[] key = KryoUtil.objToByte(Constant.REDIS_LIST_NAME);
                    Long len = connection.lLen(key);
                    if (null == len) len = 0l;
                    //批量生成id
                    long[] ids = snowflake.nextIds(redis_list_size - len.intValue());
                    //将生成的id存入redis队列
                    for (long id : ids) {
                        connection.rPush(key, KryoUtil.objToByte(id));
                    }
                    //释放redis锁
                    connection.del(KryoUtil.objToByte(Constant.REDIS_SETNX_KEY));
                    return null;
                }
            });
            //zkClient.decrease(ip);
            ctx.writeAndFlush("").addListener(ChannelFutureListener.CLOSE);
        }
    }

    public Handler(RedisOperations<Serializable, Serializable> redisTemplate, Snowflake snowflake, ZkClient zkClient, int redis_list_size) {
        this.redisTemplate = redisTemplate;
        this.snowflake = snowflake;
        this.zkClient = zkClient;
        this.redis_list_size = redis_list_size;
    }

    public RedisOperations<Serializable, Serializable> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisOperations<Serializable, Serializable> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Snowflake getSnowflake() {
        return snowflake;
    }

    public void setSnowflake(Snowflake snowflake) {
        this.snowflake = snowflake;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    public int getRedis_list_size() {
        return redis_list_size;
    }

    public void setRedis_list_size(int redis_list_size) {
        this.redis_list_size = redis_list_size;
    }
}
