package com.gome.fup.easyid.server;

import com.gome.fup.easyid.handler.DecoderHandler;
import com.gome.fup.easyid.handler.Handler;
import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.snowflake.Snowflake;
import com.gome.fup.easyid.util.Constant;
import com.gome.fup.easyid.util.IpUtil;
import com.gome.fup.easyid.util.KryoUtil;
import com.gome.fup.easyid.zk.ZkClient;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.apache.log4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.stereotype.Component;

import java.io.Serializable;
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
    private RedisOperations<Serializable, Serializable> redisTemplate;

    @Autowired
    private Snowflake snowflake;

    @Autowired
    private ZkClient zkClient;

    //@Value("#{configProperties['easyid.redis.list.size']}")
    @Value("${easyid.redis.list.size}")
    private int redis_list_size;

    public void afterPropertiesSet() throws Exception {
        //查看redis中是否有id,没有则创建
        pushIdsInRedis();
        //启动服务
        executorService.submit(this);
        logger.info("EasyID Server started!");
    }

    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
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
                                    .addLast(new Handler(redisTemplate, snowflake, zkClient, redis_list_size));
                        }
                    }).option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true);
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

    private void pushIdsInRedis() {
        redisTemplate.execute(new RedisCallback<Object>() {
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                byte[] key = KryoUtil.objToByte(Constant.REDIS_LIST_NAME);
                Long len = connection.lLen(key);
                if (len == null || len.intValue() == 0) {
                    long[] ids = snowflake.nextIds(redis_list_size);
                    for (long id : ids) {
                        connection.rPush(key, KryoUtil.objToByte(id));
                    }
                }
                return null;
            }
        });
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

}
