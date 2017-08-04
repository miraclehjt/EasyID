package com.gome.fup.easyid.demo;

import com.gome.fup.easyid.id.EasyID;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by fupeng-ds on 2017/8/4.
 */
public class Main {

    public static void main(String[] args) {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        EasyID easyID = context.getBean(EasyID.class);
        System.out.println(System.currentTimeMillis());
        for (int i = 0; i < 3000; i++) {
            long id = easyID.nextId();
            System.out.println("EasyID nextId " + i + " : " + id);
        }
        System.out.println(System.currentTimeMillis());
    }
}
