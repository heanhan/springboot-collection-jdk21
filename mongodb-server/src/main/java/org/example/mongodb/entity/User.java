package org.example.mongodb.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@Document(collection="users")
@Accessors(chain = true)
public class User implements Serializable {

    @Id
    private String id;

    //用户名
    private String userName;

    //国籍
    private String country;

    private Date birthday;
    //邮编
    private Address address;

    //爱好
    private Favorites favorites;

    //年龄
    private Integer age;

    //薪水
    private BigDecimal salary;

    //存款
    private BigDecimal money;

    //身高 单位：m
    private String length;




}
