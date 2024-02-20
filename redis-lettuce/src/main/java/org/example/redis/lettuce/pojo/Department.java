package org.example.redis.lettuce.pojo;

import jakarta.persistence.Entity;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;

@Data
@Entity(name = "t_department")
@DynamicInsert
@DynamicUpdate
@EqualsAndHashCode(callSuper = true)
public class Department extends BaseEntity{


}
