package com.example.jpa.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "t_work")
@DynamicInsert
@DynamicUpdate
public class WorkInfo extends BaseEntity{


    private Long personId;

    /**
     * 工作名称
     */
    private String workName;

    /**
     * 工作职级
     */
    private String workLevel;

    /**
     * 工作内容
     */
    private String workContent;

    /**
     * 首次入职时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern="yyyy-MM-dd HH:mm:ss")
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    private LocalDateTime firstWorkTime;

}
