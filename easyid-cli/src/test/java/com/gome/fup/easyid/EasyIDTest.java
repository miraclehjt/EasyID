package com.gome.fup.easyid;

import com.gome.fup.easyid.id.EasyID;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by fupeng-ds on 2017/8/3.
 */
public class EasyIDTest {

    @Test
    public void testNextId() {
        ApplicationContext context = new ClassPathXmlApplicationContext("classpath:spring-config.xml");
        EasyID easyID = context.getBean(EasyID.class);
        long id = easyID.nextId();
        System.out.println(id);
    }
}
