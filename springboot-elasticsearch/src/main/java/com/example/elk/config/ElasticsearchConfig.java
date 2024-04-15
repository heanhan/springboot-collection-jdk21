package com.example.elk.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.transport.ElasticsearchTransport;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.elasticsearch.ElasticsearchProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.URISyntaxException;

@Slf4j
@Configuration
@EnableConfigurationProperties({ElasticsearchProperties.class})
@ConditionalOnClass({ElasticsearchClient.class, ElasticsearchTransport.class})
public class ElasticsearchConfig {


    public ElasticsearchConfig() {
    }

    /**
     * 从elasticsearch中获取 配置信息 进行数据的封装
     * @param properties
     * @return
     */
    public static RestClient buildWithProperties(ElasticsearchProperties properties) {
        HttpHost[] hosts = properties.getUris().stream().map(ElasticsearchConfig::createHttpHost).toArray((x$0) -> new HttpHost[x$0]);
        org.elasticsearch.client.RestClientBuilder builder = RestClient.builder(hosts);
        builder.setDefaultHeaders(new BasicHeader[]{new BasicHeader("Content-type", "application/json")});
        builder.setHttpClientConfigCallback((httpClientBuilder) -> {
            httpClientBuilder.addInterceptorLast((HttpResponseInterceptor) (response, context) -> response.addHeader("X-Elastic-Product", "Elasticsearch"));
            if (hasCredentials(properties.getUsername(), properties.getPassword())) {
                // 密码配置
                CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                credentialsProvider.setCredentials(AuthScope.ANY, new UsernamePasswordCredentials(properties.getUsername(), properties.getPassword()));
                httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider);
            }
            // httpClient配置
            return httpClientBuilder;
        });
        builder.setRequestConfigCallback((requestConfigBuilder) -> {
            // request配置
            requestConfigBuilder.setConnectionRequestTimeout((int)properties.getConnectionTimeout().getSeconds() * 1000);
            requestConfigBuilder.setSocketTimeout((int)properties.getSocketTimeout().getSeconds() * 1000);
            return requestConfigBuilder;
        });
        if (properties.getPathPrefix() != null) {
            builder.setPathPrefix(properties.getPathPrefix());
        }
        return builder.build();
    }

    private static boolean hasCredentials(String username, String password) {
        return StringUtils.hasText(username) && StringUtils.hasText(password);
    }

    private static HttpHost createHttpHost(String uri) {
        try {
            return createHttpHost(URI.create(uri));
        } catch (IllegalArgumentException var2) {
            return HttpHost.create(uri);
        }
    }

    private static HttpHost createHttpHost(URI uri) {
        if (StringUtils.hasText(uri.getUserInfo())) {
            return HttpHost.create(uri.toString());
        } else {
            try {
                return HttpHost.create((new URI(uri.getScheme(), null, uri.getHost(), uri.getPort(), uri.getPath(), uri.getQuery(), uri.getFragment())).toString());
            } catch (URISyntaxException var2) {
                throw new IllegalStateException(var2);
            }
        }
    }

}
