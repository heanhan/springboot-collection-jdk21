package com.example.jpa.entity;

import com.example.jpa.config.JpaConverterListJson;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

import java.util.List;

@Data
@Entity(name = "t_person")
@DynamicInsert
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
public class Person extends BaseEntity {



    /**
     * 用户名称
     */
    private String userName;

    /**
     * 密码
     */
    private String password;

    private Boolean sex;

    /**
     * 身高
     */
    private Double height;

    /**
     * 爱好
     */
    @Convert(converter = JpaConverterListJson.class)
    private List<String> hover;

    /**
     * 个人介绍
     */
    @Lob
    private String selfInfo;


}
