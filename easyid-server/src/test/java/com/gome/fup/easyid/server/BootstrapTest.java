package com.gome.fup.easyid.server;

import com.gome.fup.easyid.util.IpUtil;
import com.gome.fup.easyid.zk.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * Created by fupeng-ds on 2017/8/4.
 */
public class BootstrapTest {

    public static void main(String[] args) throws IOException, InterruptedException, KeeperException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        ZkClient client = context.getBean(ZkClient.class);
        client.register(IpUtil.getLocalHost());
    }
}
