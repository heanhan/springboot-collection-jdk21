package org.example.mongodb.entity;

import lombok.Data;

import java.io.Serializable;

@Data
public class Address implements Serializable {

    private String code;

    private String  address_detail;
}
