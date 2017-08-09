package com.gome.fup.easyid.demo;

import com.gome.fup.easyid.id.EasyID;
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
        do {
            /*long id = easyID.nextId();
            System.out.println("EasyID nextId : " + id);*/
            /*executorService.execute(new Runnable() {

                public void run() {
                    long id = easyID.nextId();
                    System.out.println("EasyID nextId : " + id);
                }
            });*/
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    long id = easyID.nextId();
                    System.out.println("EasyID nextId : " + id);
                }
            });
            thread.start();
        } while (System.currentTimeMillis() - begen < 1000l);
        /*for (int i = 0; i < 1500; i++) {
            long id = easyID.nextId();
            System.out.println("EasyID nextId : " + id);
        }*/
        context.close();
        //System.exit(0);
    }


}
