package com.example.elk.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Document(indexName = "discuss_post")
public class DiscussPost {

    @Id
    private String id;

    // 用户id
    @Field(type = FieldType.Integer, name = "price")
    private Integer userId;

    // 帖子的标题 eg.互联网校招
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String title;

    // 帖子的内容
    @Field(type = FieldType.Text, analyzer = "ik_max_word", searchAnalyzer = "ik_smart")
    private String content;

    // type为帖子类型，0--普通帖子，1--置顶帖子，2--精华帖子
    @Field(type = FieldType.Integer)
    private int type;

    // 帖子的状态，0--正常，1--精华，2--拉黑
    @Field(type = FieldType.Integer)
    private int status;

    // 帖子创建的时间
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @Field(type = FieldType.Date)
    private Date createTime;

    // 帖子评论的数量
    @Field(type = FieldType.Integer)
    private int commentCount;

    // 记录帖子的分数
    @Field(type = FieldType.Double)
    private double score;

}
