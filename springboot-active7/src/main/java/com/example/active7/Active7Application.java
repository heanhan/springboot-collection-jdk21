package com.example.active7;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;

/**
 * Hello world!
 *
 */

@SpringBootApplication(exclude = {
        //activiti 默认整合了security，屏蔽security认证
        SecurityAutoConfiguration.class,
        ManagementWebSecurityAutoConfiguration.class
})
public class Active7Application
{
    public static void main( String[] args )
    {
        SpringApplication.run(Active7Application.class,args);
    }
}
