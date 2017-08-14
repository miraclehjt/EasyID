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

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        final EasyID easyID = context.getBean(EasyID.class);
        //final JedisUtil jedisUtil = JedisUtil.newInstance("192.168.56.102", 6379);
        for (int i = 0; i < 5000; i++) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    long id = 0;
                    try {
                        id = easyID.nextId();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    System.out.println("EasyID nextId : " + id + ",time:" + System.currentTimeMillis());
                    //jedisUtil.incr("count");
                }
            });
            thread.start();
        }
        context.close();
    }


}
