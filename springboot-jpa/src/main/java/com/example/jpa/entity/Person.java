package com.example.jpa.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "t_person")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Person implements Serializable {

    /**
     * 用户的di 主键
     */
    @Id
    private Long id;

    /**
     * 用户名称
     */
    private String userName;

    /**
     * 密码
     */
    private String password;


}
