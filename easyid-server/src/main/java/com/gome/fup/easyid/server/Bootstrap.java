package com.gome.fup.easyid.server;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * EasyID服务启动入口
 * Created by fupeng-ds on 2017/8/2.
 */
public class Bootstrap {

    public static void main(String[] args) {
        new ClassPathXmlApplicationContext("classpath:spring-config.xml");
    }

}
