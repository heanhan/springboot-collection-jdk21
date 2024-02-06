package org.example.mongodb.entity;

import lombok.Data;
import lombok.experimental.Accessors;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.io.Serializable;


@Document(value = "books")
@Data
@Accessors(chain = true)
public class Books implements Serializable {

    @Id
    private String id;

    private String author;

    private Double favCount;

    private String tag;

    private String title;

    private String type;


}
