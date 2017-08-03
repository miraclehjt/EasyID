package com.gome.fup.easyid.zk;

import org.apache.zookeeper.KeeperException;
import org.junit.Test;

import java.io.IOException;

/**
 * Created by fupeng-ds on 2017/8/3.
 */
public class ZkClientTest {

    @Test
    public void testRegister() throws InterruptedException, IOException, KeeperException {
        ZkClient client = new ZkClient("127.0.0.1:2181");
        client.register("testRegister");
    }

    @Test
    public void testIncrease() throws InterruptedException, IOException, KeeperException {
        ZkClient client = new ZkClient("127.0.0.1:2181");
        int increase = client.increase("testRegister");
        System.out.println(increase);
    }

    @Test
    public void testDecrease() throws InterruptedException, IOException, KeeperException {
        ZkClient client = new ZkClient("127.0.0.1:2181");
        int increase = client.decrease("testRegister");
        System.out.println(increase);
    }
}
