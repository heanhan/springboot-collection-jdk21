package com.example.web;

import com.example.web.config.MovieSiteProperties;
import com.example.web.config.MovieSiteTemplate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(MovieSiteProperties.class)
public class ExpandWebStarterAutoConfiguration {

    private MovieSiteProperties movieSiteProperties;

    public ExpandWebStarterAutoConfiguration(MovieSiteProperties movieSiteProperties) {
        this.movieSiteProperties = movieSiteProperties;
    }

    @Bean
    @ConditionalOnMissingBean(MovieSiteTemplate.class)
    public MovieSiteTemplate movieSiteTemplate(){
        return new MovieSiteTemplate(movieSiteProperties);
    }
}

