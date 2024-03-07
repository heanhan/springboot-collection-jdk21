package com.example.test.pojo;


import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;

import java.io.Serializable;

@Tag(name = "测试类", description = "测试实体类")
@Data
public class TestEntity implements Serializable {


    @Schema(name = "id", type = "long")
    private Long id;

    @Schema(name = "用户名", type = "string")
    private String userName;

    @Schema(name = "用户id", type = "String[]")
    private String[] hobby;
}
