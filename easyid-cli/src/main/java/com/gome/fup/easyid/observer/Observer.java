package com.gome.fup.easyid.observer;

import com.gome.fup.easyid.handler.DecoderHandler;
import com.gome.fup.easyid.handler.EncoderHandler;
import com.gome.fup.easyid.id.EasyID;
import com.gome.fup.easyid.model.Request;
import com.gome.fup.easyid.util.Constant;
import com.gome.fup.easyid.util.IpUtil;
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
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * 观察者类，监听EasyID状态变化，通知服务端创建ID
 * Created by fupeng-ds on 2017/8/3.
 */
public class Observer implements InitializingBean, ApplicationContextAware{

    private static final Logger logger = Logger.getLogger(Observer.class);

    private EasyID easyID;

    private ZkClient zkClient;

    public void afterPropertiesSet() throws Exception {
        //启动一个线程，来监听EasyID中flag开关的变化
        Thread thread = new Thread(new Sender());
        thread.start();
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        this.easyID = context.getBean(EasyID.class);
        this.zkClient = context.getBean(ZkClient.class);
    }

    private class Sender implements Runnable {

        /**
         * 服务端ip地址
         */
        private String host;

        /**
         * 服务端端口号
         */
        private int port;

        public void run() {
            while (true) {
                if (easyID.isFlag()) {
                    try {
                        //通过zookeeper的负载均衡算法，获取服务端ip地址
                        String ip = zkClient.balance();
                        host = IpUtil.getHost(ip);
                        port = Constant.EASYID_SERVER_PORT;
                        //向服务端发送创建ID请求
                        send();
                        logger.info("send request to server!");
                        //关闭开关
                        easyID.setFlag(false);
                    } catch (KeeperException e) {
                        e.printStackTrace();
                        logger.equals(e.getMessage());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        logger.equals(e.getMessage());
                    }
                }
            }
        }

        /**
         * 访问服务端
         */
        private void send() {
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
                e.printStackTrace();
                logger.equals(e.getMessage());
            } finally {
                group.shutdownGracefully();
            }
        }

        public String getHost() {
            return host;
        }

        public void setHost(String host) {
            this.host = host;
        }

        public int getPort() {
            return port;
        }

        public void setPort(int port) {
            this.port = port;
        }
    }

    public EasyID getEasyID() {
        return easyID;
    }

    public void setEasyID(EasyID easyID) {
        this.easyID = easyID;
    }

    public ZkClient getZkClient() {
        return zkClient;
    }

    public void setZkClient(ZkClient zkClient) {
        this.zkClient = zkClient;
    }
}
