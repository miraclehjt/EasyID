package com.gome.fup.easyid.demo;

import com.gome.fup.easyid.id.EasyID;
import com.gome.fup.easyid.util.JedisUtil;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by fupeng-ds on 2017/8/4.
 */
public class Main {

    private static ExecutorService executorService = Executors.newFixedThreadPool(8);

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        final EasyID easyID = context.getBean(EasyID.class);
        long begen = System.currentTimeMillis();
        int count = 0;
        final JedisUtil jedisUtil = JedisUtil.newInstance("192.168.56.102", 6379);
        do {
            executorService.execute(new Runnable() {

                public void run() {
                    long id = easyID.nextId();
                    System.out.println("EasyID nextId : " + id);
                    jedisUtil.incr("count");
                }
            });
        } while (System.currentTimeMillis() - begen < 1000l);
        System.out.println(count);
        context.close();
        //System.exit(0);
    }


}
