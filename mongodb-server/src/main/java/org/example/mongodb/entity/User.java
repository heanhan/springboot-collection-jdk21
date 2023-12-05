package org.example.mongodb.entity;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
public class User implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 用户名  账号
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    /**
     * 别名
     */
    private String nickName;

    /**
     * 实名
     */
    private String realName;

    /**
     * 身份证号
     */
    private String idCardNo;

    /**
     * 爱好
     */
    private List<String> hobby;

    /**
     * 性别
     */
    private boolean sex;

    /**
     * 个人介绍  座右铭之类
     */
    private String descriptions;

    private Date createTime;

    private Data updateTime;

    /**
     * 数据是否有效，ture,有效; false 是无效
     */
    private Boolean deleted;


}
