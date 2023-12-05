package org.example.mongodb.entity.param;

import lombok.Data;

@Data
public class SelectBooksParam {

    private String id;

    private String author;

    private Double favCount;

    private String tag;

    private String title;

    private String type;

    /**
     * 当前页
     */
    private Integer indexNumber=0;

    /**
     * 每页大小
     */
    private Integer pageSize=10;
}
