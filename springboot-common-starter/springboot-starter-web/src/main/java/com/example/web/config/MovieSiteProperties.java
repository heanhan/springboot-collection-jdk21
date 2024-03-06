package com.example.web.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "movie.site")
public class MovieSiteProperties {

    /**
     * 网站名称
     */
    private String name;
    /** 网站地址 */
    private String url;
    //省去getter、setter方法
}
