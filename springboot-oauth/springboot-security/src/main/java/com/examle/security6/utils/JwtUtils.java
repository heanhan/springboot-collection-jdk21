package com.examle.security6.utils;

import com.examle.security6.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

@Component
public class JwtUtils {


    private JwtProperties jwtProperties;

    /**
     * @Description: 生成令牌
     * @Author: 翰戈.summer
     * @Date: 2023/11/16
     * @Param: Map
     * @Return: String jwt
     */
    public String getJwt(Map<String, Object> claims) {

        String signingKey = jwtProperties.getSigningKey();
        Long expire = jwtProperties.getExpire();

        return Jwts.builder()
                .setClaims(claims) //设置载荷内容
                .signWith(SignatureAlgorithm.HS256, signingKey) //设置签名算法
                .setExpiration(new Date(System.currentTimeMillis() + expire)) //设置有效时间
                .compact();
    }

    /**
     * @Description: 解析令牌
     * @Author: 翰戈.summer
     * @Date: 2023/11/16
     * @Param: String jwt
     * @Return: Claims claims
     */
    public Claims parseJwt(String jwt) {

        String signingKey = jwtProperties.getSigningKey();

        return Jwts.parser()
                .setSigningKey(signingKey) //指定签名密钥
                .parseClaimsJws(jwt) //开始解析令牌
                .getBody();
    }

}
