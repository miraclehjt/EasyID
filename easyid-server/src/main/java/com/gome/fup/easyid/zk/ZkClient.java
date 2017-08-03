package com.gome.fup.easyid.zk;

import com.gome.fup.easyid.util.ConversionUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;

/**
 * ZooKeeper客户端类，在ZooKeeper上注册服务信息，并实现负载均衡
 * Created by fupeng-ds on 2017/8/3.
 */
public class ZkClient extends AbstractZkClient {

    /**
     * 初始化ZooKeeper客户端，并判断是否有根节点，没有则创建持久化节点
     * @param address
     * @throws IOException
     * @throws KeeperException
     * @throws InterruptedException
     */
    public ZkClient(String address) throws IOException, KeeperException, InterruptedException {
        this.address = address;
        zooKeeper = new ZooKeeper(address, SESSIONTIMEOUT, null);
        Stat stat = zooKeeper.exists(ZK_ROOT_NODE, false);
        if (null == stat) {
            zooKeeper.create(ZK_ROOT_NODE, null, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }

    /**
     * 初始化节点，并记录连接数为0
     * @param node
     * @throws KeeperException
     * @throws InterruptedException
     */
    public void register(String node) throws KeeperException, InterruptedException {
        zooKeeper.create(getNodePath(node), ConversionUtil.intToByteArray(0), ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
    }

    /**
     * 增加节点的记录数
     * @param node
     * @throws KeeperException
     * @throws InterruptedException
     */
    public synchronized int increase(String node) throws KeeperException, InterruptedException {
        int count = getCount(node);
        zooKeeper.setData(getNodePath(node), ConversionUtil.intToByteArray(++count), -1);
        return count;
    }

    /**
     * 减少节点的记录数
     * @param node
     * @throws KeeperException
     * @throws InterruptedException
     */
    public synchronized int decrease(String node) throws KeeperException, InterruptedException {
        int count = getCount(node);
        if (count > 0) {
            zooKeeper.setData(getNodePath(node), ConversionUtil.intToByteArray(--count), -1);
        }
        return count;
    }

}
