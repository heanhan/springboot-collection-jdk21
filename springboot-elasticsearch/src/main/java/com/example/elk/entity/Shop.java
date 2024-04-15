package com.example.elk.entity;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "shop")
public class Shop{

    /**
     * 编号
     */
    @Id
    private String id;

    /**
     * 名称
     */
    @Field(type = FieldType.Text, name = "name")
    private String name;

    /**
     * 分类
     */
    @Field(type = FieldType.Text, name = "category")
    private String category;

    /**
     * 价格
     */
    @Field(type = FieldType.Double, name = "price")
    private Double price;

    /**
     * 来源
     */
    @Field(type = FieldType.Keyword,name = "source")
    private String source;

    /**
     * 介绍
     */
    @Field(type = FieldType.Keyword,name = "content")
    private String content;


}
