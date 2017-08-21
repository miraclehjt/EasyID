package com.gome.pop.fup.easyid.server;

import com.gome.pop.fup.easyid.handler.DecoderHandler;
import com.gome.pop.fup.easyid.handler.Handler;
import com.gome.pop.fup.easyid.model.Request;
import com.gome.pop.fup.easyid.snowflake.Snowflake;
import com.gome.pop.fup.easyid.util.*;
import com.gome.pop.fup.easyid.zk.ZkClient;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 服务端，接收创建id的请求
 * Created by fupeng-ds on 2017/8/2.
 */
@Component
public class Server implements Runnable {

    private static final Logger logger = Logger.getLogger(Server.class);

    private ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Autowired
    private Snowflake snowflake;

    private ZkClient zkClient;

    private String redisAddress;

    private String zookeeperAddres;

    private JedisUtil jedisUtil;

    public void start() throws Exception {
        jedisUtil = JedisUtil.newInstance(redisAddress);
        String localHost = IpUtil.getLocalHost();
        Cache.set(Constant.LOCALHOST, localHost, -1l);
        zkClient = new ZkClient(zookeeperAddres);
        zkClient.register(localHost);
        //查看redis中是否有id,没有则创建
        pushIdsInRedis();
        //启动服务
        executorService.submit(this);
        logger.info("EasyID Server started!");
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                zkClient.close();
                jedisUtil.close();
                executorService.shutdown();
            }
        }));
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
        }
    }

    private void pushIdsInRedis() throws KeeperException, InterruptedException {
        //从zookeeper中获取队列长度参数
        int base = zkClient.getRedisListSize();
        Long len = jedisUtil.llen(Constant.REDIS_LIST_NAME);
        if (len == null || len.intValue() == 0 || len.intValue() < (base * 300)) {
            long[] ids = snowflake.nextIds((base * 1000) - len.intValue());
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

    public String getZookeeperAddres() {
        return zookeeperAddres;
    }

    public void setZookeeperAddres(String zookeeperAddres) {
        this.zookeeperAddres = zookeeperAddres;
    }

    public String getRedisAddress() {
        return redisAddress;
    }

    public void setRedisAddress(String redisAddress) {
        this.redisAddress = redisAddress;
    }
}
