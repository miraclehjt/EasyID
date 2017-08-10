package com.gome.fup.easyid.server;

import com.gome.fup.easyid.handler.DecoderHandler;
import com.gome.fup.easyid.handler.Handler;
import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.snowflake.Snowflake;
import com.gome.fup.easyid.util.*;
import com.gome.fup.easyid.zk.ZkClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端，接收创建id的请求
 * Created by fupeng-ds on 2017/8/2.
 */
@Component
public class Server implements Runnable, InitializingBean {

    private static final Logger logger = Logger.getLogger(Server.class);

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private Snowflake snowflake;

    @Autowired
    private ZkClient zkClient;

    private int redis_list_size;

    @Value("${easyid.redis.host}")
    private String redishost;

    @Value("${easyid.redis.port}")
    private int redisport;

    private JedisUtil jedisUtil;

    public void afterPropertiesSet() throws Exception {
        jedisUtil = JedisUtil.newInstance(redishost, redisport);
        String localHost = IpUtil.getLocalHost();
        Cache.set(Constant.LOCALHOST, localHost, -1l);
        //查看redis中是否有id,没有则创建
        pushIdsInRedis();
        //启动服务
        executorService.submit(this);
        logger.info("EasyID Server started!");
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup(8);
        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap
                    .group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .childHandler(new ChannelInitializer<SocketChannel>() {

                        @Override
                        protected void initChannel(SocketChannel socketChannel)
                                throws Exception {
                            socketChannel.pipeline()
                                    .addLast(new DecoderHandler(Request.class))
                                    .addLast(new Handler(jedisUtil, snowflake, zkClient));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT)
                    .childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
            ChannelFuture future = bootstrap.bind(IpUtil.getLocalHost(), Constant.EASYID_SERVER_PORT).sync();
            future.channel().closeFuture().sync();
        } catch (Exception e) {
            e.printStackTrace();
            logger.error(e.getMessage());
        } finally {
            workerGroup.shutdownGracefully();
            bossGroup.shutdownGracefully();
            executorService.shutdown();
        }
    }

    private void pushIdsInRedis() throws KeeperException, InterruptedException {
        //从zookeeper中获取队列长度参数
        redis_list_size = zkClient.getRedisListSize() * 1000;
        Long len = jedisUtil.llen(Constant.REDIS_LIST_NAME);
        if (len == null || len.intValue() == 0) {
            long[] ids = snowflake.nextIds(redis_list_size);
            String[] strings = ConversionUtil.longsToStrings(ids);
            jedisUtil.rpush(Constant.REDIS_LIST_NAME, strings);
        }
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
