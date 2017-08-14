package com.gome.fup.easyid.id;

import com.gome.fup.easyid.handler.EncoderHandler;
import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.util.*;
import com.gome.fup.easyid.zk.ZkClient;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.apache.log4j.Logger;
import org.apache.zookeeper.KeeperException;
import org.springframework.beans.factory.InitializingBean;
import redis.clients.jedis.ShardedJedis;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 客户端ID生成类
 * Created by fupeng-ds on 2017/8/3.
 */
public class EasyID implements InitializingBean{

    private static final Logger logger = Logger.getLogger(EasyID.class);

    /**
     * 服务端开始生成新的ID的开关
     */
    private volatile boolean flag = false;

    private ExecutorService executorService = Executors.newFixedThreadPool(16);

    private ZkClient zkClient;

    private JedisUtil jedisUtil;

    /**
     *ZooKeeper服务地址
     */
    private String zkAddress;

    /**
     *rediss服务地址
     */
    private String redisAddress;



    /**
     * 获取id
     * @return
     */
    public long nextId() throws InterruptedException {
        long begin = System.currentTimeMillis();
        int list_min_size = zkClient.getRedisListSize() * 300;
        String id = "";
        ShardedJedis jedis = jedisUtil.getJedis();
        long len = 0l;
        try {
            len = jedis.llen(Constant.REDIS_LIST_NAME);
            if ((int) len < list_min_size) {
                getRedisLock(jedis);
            }
            id = jedis.lpop(Constant.REDIS_LIST_NAME);
        } finally {
            jedisUtil.returnResource(jedis);
        }
        if (len == 0l || null == id || "".equals(id)) {
            Thread.sleep(100l);
            return nextId();
        }
        //logger.info("nextId use time : " + (System.currentTimeMillis() - begin));
        System.out.println("nextId use time : " + (System.currentTimeMillis() - begin));
        return Long.valueOf(id);
    }

    /**
     * 获取redis锁；若获得，则发送消息到服务端
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void getRedisLock(ShardedJedis jedis) {
        if (jedis.setnx(Constant.REDIS_SETNX_KEY, "1") == 1l) {
            jedis.expire(Constant.REDIS_SETNX_KEY, 3);
            try {
                send();
            } catch (KeeperException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开启另外的线程，访问服务端
     */
    private void send() throws KeeperException, InterruptedException {
        executorService.submit(new Callable<Object>() {
            public Object call() throws Exception {
                //通过zookeeper的负载均衡算法，获取服务端ip地址
                String ip = zkClient.balance();
                final String host = IpUtil.getHost(ip);
                final int port = Constant.EASYID_SERVER_PORT;
                EventLoopGroup group = new NioEventLoopGroup();
                try {
                    Bootstrap bootstrap = new Bootstrap();
                    bootstrap.group(group).channel(NioSocketChannel.class)
                            .handler(new ChannelInitializer<SocketChannel>() {

                                @Override
                                protected void initChannel(SocketChannel ch)
                                        throws Exception {
                                    ch.pipeline()
                                            .addLast(new EncoderHandler());
                                }
                            }).option(ChannelOption.SO_KEEPALIVE, true);
                    // 链接服务器
                    ChannelFuture future = bootstrap.connect(host, port).sync();
                    // 将request对象写入outbundle处理后发出
                    future.channel().writeAndFlush(new Request(MessageType.REQUEST_TYPE_CREATE)).sync();
                    // 服务器同步连接断开时,这句代码才会往下执行
                    future.channel().closeFuture().sync();

                } catch (Exception e) {
                    logger.error(e);
                } finally {
                    group.shutdownGracefully();
                }
                return null;
            }
        });
    }

    public boolean isFlag() {
        return flag;
    }

    public void setFlag(boolean flag) {
        this.flag = flag;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }

    public String getZkAddress() {
        return zkAddress;
    }

    public void setZkAddress(String zkAddress) {
        this.zkAddress = zkAddress;
    }

    public String getRedisAddress() {
        return redisAddress;
    }

    public void setRedisAddress(String redisAddress) {
        this.redisAddress = redisAddress;
    }

    public void afterPropertiesSet() throws Exception {
        //初始化ZkClient
        this.zkClient = new ZkClient(zkAddress);
        this.jedisUtil = JedisUtil.newInstance(redisAddress);
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                zkClient.close();
                jedisUtil.close();
                executorService.shutdown();
            }
        }));
    }
}
