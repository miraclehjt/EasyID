package com.gome.fup.easyid.handler;

import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.snowflake.Snowflake;
import com.gome.fup.easyid.util.*;
import com.gome.fup.easyid.zk.ZkClient;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import org.apache.log4j.Logger;

/**
 * 请求处理handler
 * Created by fupeng-ds on 2017/8/3.
 */
public class Handler extends SimpleChannelInboundHandler<Request> {

    private final static Logger LOGGER = Logger.getLogger(Handler.class);

    private JedisUtil jedisUtil;

    private Snowflake snowflake;

    private ZkClient zkClient;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Request request) throws Exception {
        if (request.getType() == MessageType.REQUEST_TYPE_CREATE) {
            try {
                final int redis_list_size = zkClient.getRedisListSize() * 1000;
                String ip = IpUtil.getLocalHost();
                zkClient.increase(ip);
                Long len = jedisUtil.llen(Constant.REDIS_LIST_NAME);
                if (null == len) len = 0l;
                //批量生成id
                long[] ids = snowflake.nextIds(redis_list_size - len.intValue());
                //将生成的id存入redis队列
                for (long id : ids) {
                    jedisUtil.rpush(Constant.REDIS_LIST_NAME, String.valueOf(id));
                }
            } finally {
                jedisUtil.del(Constant.REDIS_SETNX_KEY);
            }
            //zkClient.decrease(ip);
            ctx.writeAndFlush("").addListener(ChannelFutureListener.CLOSE);
        }
    }

    public Handler(JedisUtil jedisUtil, Snowflake snowflake, ZkClient zkClient) {
        this.jedisUtil = jedisUtil;
        this.snowflake = snowflake;
        this.zkClient = zkClient;
    }

    public JedisUtil getJedisUtil() {
        return jedisUtil;
    }

    public void setJedisUtil(JedisUtil jedisUtil) {
        this.jedisUtil = jedisUtil;
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

}
