package com.example.jpa.dto;


import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class PersonWorkDto {

    //用户id
    private Long  personId;

    //用户名
    private String userName;

    //性别
    private Boolean sex;

    //自我介绍
    private String selfInfo;

    ///工作id
    private Long workId;

    //工作名称
    private String workName;

    //级别
    private String workLevel;

    //工作内容
    private String workContent;

    public PersonWorkDto(Long personId, String userName, Boolean sex, String selfInfo, Long workId, String workName, String workLevel, String workContent) {
        this.personId = personId;
        this.userName = userName;
        this.sex = sex;
        this.selfInfo = selfInfo;
        this.workId = workId;
        this.workName = workName;
        this.workLevel = workLevel;
        this.workContent = workContent;
    }
}
