package com.gome.fup.easyid.zk;

import com.gome.fup.easyid.util.ConversionUtil;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;

/**
 *ZkClient抽象类
 * Created by fupeng-ds on 2017/8/3.
 */
public abstract class AbstractZkClient {

    /**
     * ZooKeeper上的根节点
     */
    protected final String ZK_ROOT_NODE = "/EasyID";

    /**
     * ZooKeeper客户端
     */
    protected ZooKeeper zooKeeper;

    /**
     *ZooKeeper服务地址
     */
    protected String address;

    /**
     * ZooKeeper会话有效时间
     */
    protected final static int SESSIONTIMEOUT = 10000;

    /**
     * 关闭zookeeper连接
     * @throws InterruptedException
     */
    public void close() throws InterruptedException {
        zooKeeper.close();
    }

    public int getCount(String node) throws KeeperException, InterruptedException {
        byte[] data = zooKeeper.getData(getNodePath(node), null, null);
        return ConversionUtil.byteArrayToInt(data);
    }

    protected String getNodePath(String node) {
        return ZK_ROOT_NODE + "/" + node;
    }
}
