package com.gome.fup.easyid.server;

import com.gome.fup.easyid.snowflake.Snowflake;
import com.gome.fup.easyid.util.IpUtil;
import com.gome.fup.easyid.zk.ZkClient;
import org.apache.zookeeper.KeeperException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * EasyID服务启动入口
 * Created by fupeng-ds on 2017/8/2.
 */
public class Bootstrap {

    /**
     * 例：java -jar EasyID.jar -zk127.0.0.1:2181 -workerid10 -datacenterid11
     * @param args
     * @throws IOException
     * @throws KeeperException
     * @throws InterruptedException
     */
    public static void main(String[] args) throws IOException, KeeperException, InterruptedException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        //自动管理workerid与datacenterid
        ZkClient zkClient = context.getBean(ZkClient.class);
        Snowflake snowflake = context.getBean(Snowflake.class);
        int size = zkClient.getRootChildrenSize();
        if (size > 31) {
            int times = size/31;
            size = size - (31 * times);
        }
        snowflake.setWorkerId(size);
        snowflake.setWorkerId(size);
    }

    /**
     * 解析参数
     * 参数说明：
     *         -zk127.0.0.1:2181， zookeeper地址
     *         -workerid10， snowflake的工作id，取值范围0~31
     *         -datacenterid11， snowflake的工作中心id，取值范围0~31
     * @param context
     * @param arg
     */
    private static void analysis(ClassPathXmlApplicationContext context, String arg) throws KeeperException, InterruptedException {
        if (arg.contains("-workerid")) {
            String[] split = arg.split("-workerid");
            Snowflake snowflake = context.getBean(Snowflake.class);
            snowflake.setWorkerId(Long.parseLong(split[1]));
        }
        if (arg.contains("-datacenterid")) {
            String[] split = arg.split("-datacenterid");
            Snowflake snowflake = context.getBean(Snowflake.class);
            snowflake.setDatacenterId(Long.parseLong(split[1]));
        }
    }
}
