package com.gome.fup.easyid.id;

import com.gome.fup.easyid.exception.NoMoreValueInRedisException;
import com.gome.fup.easyid.handler.EncoderHandler;
import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.util.Constant;
import com.gome.fup.easyid.util.IpUtil;
import com.gome.fup.easyid.util.KryoUtil;
import com.gome.fup.easyid.util.MessageType;
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
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.TimeoutUtils;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * 客户端ID生成类
 * Created by fupeng-ds on 2017/8/3.
 */
public class EasyID {

    private static final Logger logger = Logger.getLogger(EasyID.class);

    /**
     * redis队列中最低ID数量，低于此数量时，服务端开始生成新的ID并存入redis队列
     */
    private final long REDIS_LIST_MIN_SIZE = 300l;

    /**
     * 服务端开始生成新的ID的开关
     */
    private volatile boolean flag = false;

    private RedisOperations<Serializable, Serializable> redisTemplate;

    private ExecutorService executorService = Executors.newFixedThreadPool(4);

    private ZkClient zkClient;

    private final Object obj = new Object();

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
        final byte[] key = KryoUtil.objToByte(Constant.REDIS_LIST_NAME);
        return redisTemplate.execute(new RedisCallback<Long[]>() {
            public Long[] doInRedis(RedisConnection connection) throws DataAccessException {
                try {
                    Long[] ids = new Long[count];
                    synchronized (obj) {
                        Long len = connection.lLen(key);
                        byte[] setnex_key = KryoUtil.objToByte(Constant.REDIS_SETNX_KEY);
                        if (len < REDIS_LIST_MIN_SIZE) {
                            getRedisLock(connection, setnex_key);
                            logger.info("ids in redis less then 300");
                            if (len == 0l) {
                                //synchronized为可重入锁，允许递归调用
                                return nextIds(count);
                            }
                        }
                        if (count > len.intValue()) {
                            logger.error("count:" + count + ",len:" + len.intValue());
                            throw new NoMoreValueInRedisException("没有足够的值");
                        }
                        for (int i = 0; i < count; i++) {
                            byte[] bytes = connection.lPop(key);
                            ids[i] = KryoUtil.byteToObj(bytes, Long.class);
                        }
                    }
                    return ids;
                } catch (KeeperException e) {
                    logger.error(e);
                } catch (InterruptedException e) {
                    logger.error(e);
                }
                return null;
            }
        });
    }

    /**
     * 获取redis锁；若获得，则发送消息到服务端
     * @param connection
     * @param setnex_key
     * @throws KeeperException
     * @throws InterruptedException
     */
    private void getRedisLock(RedisConnection connection, byte[] setnex_key) throws KeeperException, InterruptedException {
        if (connection.setNX(setnex_key, KryoUtil.objToByte(1))) {    //获得redis锁
            logger.info("get redis synchronized!");
            //设置redis锁的有效时间3秒
            connection.expire(setnex_key, TimeoutUtils.toMillis(3l, TimeUnit.SECONDS));
            //发送创建ID的请求到服务端
            send();
        }
    }

    /**
     * 开启另外的线程，访问服务端
     */
    private void send() throws KeeperException, InterruptedException {
        //通过zookeeper的负载均衡算法，获取服务端ip地址
        String ip = zkClient.balance();
        final String host = IpUtil.getHost(ip);
        final int port = Constant.EASYID_SERVER_PORT;
        executorService.submit(new Runnable() {
            public void run() {
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
            }
        });

    }

    public RedisOperations<Serializable, Serializable> getRedisTemplate() {
        return redisTemplate;
    }

    public void setRedisTemplate(RedisOperations<Serializable, Serializable> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
}
