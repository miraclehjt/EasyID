package com.gome.fup.easyid.zk;

import com.gome.fup.easyid.util.ConversionUtil;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;

/**
 * ZooKeeper客户端类，在ZooKeeper上注册服务信息，并实现负载均衡
 * Created by fupeng-ds on 2017/8/3.
 */
public class ZkClient {

    /**
     * ZooKeeper上的根节点
     */
    private final String ZK_ROOT_NODE = "/EasyID";

    /**
     * ZooKeeper客户端
     */
    private ZooKeeper zooKeeper;

    /**
     *ZooKeeper服务地址
     */
    private String address;

    /**
     * ZooKeeper会话有效时间
     */
    private final static int SESSIONTIMEOUT = 10000;

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

    public int getCount(String node) throws KeeperException, InterruptedException {
        byte[] data = zooKeeper.getData(getNodePath(node), null, null);
        return ConversionUtil.byteArrayToInt(data);
    }

    private String getNodePath(String node) {
        return ZK_ROOT_NODE + "/" + node;
    }

    /**
     * 负载均衡算法：zk中连接数最少的节点ip
     * @return
     * @throws KeeperException
     * @throws InterruptedException
     */
    public String balance() throws KeeperException, InterruptedException {
        List<String> nodes = zooKeeper.getChildren(ZK_ROOT_NODE, null);
        String result = null;
        int min = 0;
        for (int i = 0; i < nodes.size(); i++) {
            int count = getCount(nodes.get(i));
            if (i == 0 || count < min) {
                min = count;
                result = nodes.get(i);
            }
        }
        return result;
    }
}
