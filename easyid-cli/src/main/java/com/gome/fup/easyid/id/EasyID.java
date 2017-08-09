package com.gome.fup.easyid.id;

import com.gome.fup.easyid.exception.NoMoreValueInRedisException;
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

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    private ZkClient zkClient;

    private JedisUtil jedisUtil;

    private final Object obj = new Object();

    /**
     *ZooKeeper服务地址
     */
    private String zkAddress;

    /**
     *rediss服务地址
     */
    private String redissAddress;



    /**
     * 获取id
     * @return
     */
    public long nextId() {
        if (nextIds(1) == null) throw new NullPointerException();
        return nextIds(1)[0];
    }

    /**
     * 获取count数量的id集合
     * @param count
     * @return
     */
    public Long[] nextIds(final int count) {
        try {
            Long[] ids = new Long[count];
            int list_min_size = zkClient.getRedisListSize() * 300;
            synchronized (obj) {
                long len = jedisUtil.llen(Constant.REDIS_LIST_NAME);
                if ((int)len < list_min_size || count > (int)len) {
                    getRedisLock();
                    //logger.info("ids in redis less then 300");
                    if (len == 0l) {
                        //synchronized为可重入锁，允许递归调用
                        Thread.sleep(50l);
                        return nextIds(count);
                    }
                }
            }
            for (int i = 0; i < count; i++) {
                String id = jedisUtil.lpop(Constant.REDIS_LIST_NAME);
                ids[i] = Long.valueOf(id);
            }
            return ids;
        } catch (InterruptedException e) {
            logger.error(e);
        } catch (NumberFormatException e) {
            logger.error(e);
        }
        return new Long[count];
    }

    /**
     * 获取redis锁；若获得，则发送消息到服务端
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void getRedisLock() {
        if (jedisUtil.setnx(Constant.REDIS_SETNX_KEY, "1") == 1l) {
            jedisUtil.expire(Constant.REDIS_SETNX_KEY, 3);
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

    public String getRedissAddress() {
        return redissAddress;
    }

    public void setRedissAddress(String redissAddress) {
        this.redissAddress = redissAddress;
    }

    public void afterPropertiesSet() throws Exception {
        //初始化ZkClient
        this.zkClient = new ZkClient(zkAddress);
        String[] split = this.redissAddress.split(":");
        this.jedisUtil = JedisUtil.newInstance(split[0], Integer.valueOf(split[1]));
    }
}
