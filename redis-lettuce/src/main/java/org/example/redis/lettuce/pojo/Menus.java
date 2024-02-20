package org.example.redis.lettuce.pojo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.io.Serializable;

@Entity
@Table(name = "t_menus")
@Data
public class Menus implements Serializable {

    @Id
    private Long id;

    private Long pid;


}
