package com.example.web.config;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MovieSiteTemplate {

    @Resource
    private MovieSiteProperties movieSiteProperties;

    public MovieSiteTemplate(MovieSiteProperties movieSiteProperties) {
        this.movieSiteProperties = movieSiteProperties;
    }


    public void openSite(){
        log.info("打开网站："+movieSiteProperties.getName()+"，地址："+movieSiteProperties.getUrl() +" 学习知识。");
    }
}
