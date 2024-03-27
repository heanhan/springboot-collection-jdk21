package com.examle.security6.config;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
public class JwtProperties {

    @Value(value = "${jwt.signingKey}")
    private String signingKey;

    @Value(value = "${jwt.expire}")
    private Long expire;
}
