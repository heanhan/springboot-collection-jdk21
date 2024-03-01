package com.example.minio.dto;

import lombok.Data;

@Data
public class FileInfo {

    /**
     * 文件名
     */
    private String filename;

    /**
     * 是否为目录
     */
    private Boolean directory;

    /**
     * 文件大小
     */
    private String size;


}
