package com.example.oauth.service;

import com.example.oauth.entity.OAuthClient;
import com.example.oauth.vo.TokenVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * OAuth 客户端服务（JDBC 存储 + client_credentials 令牌签发）
 */
public interface OAuthClientService {

    /** 分页查询客户端（keyword 按客户端名模糊，可空） */
    Page<OAuthClient> page(String keyword, Pageable pageable);

    /** 按 ID 查询客户端 */
    OAuthClient getById(Long id);

    /**
     * 新增客户端
     *
     * @param client 客户端信息（clientSecret 为明文，方法内部会 BCrypt 加密）
     */
    OAuthClient create(OAuthClient client);

    /** 更新客户端（clientId 不可改；clientSecret 非空才更新） */
    OAuthClient update(OAuthClient client);

    /** 删除客户端 */
    void delete(Long id);

    /**
     * 校验客户端凭证并签发令牌（client_credentials 模式）
     *
     * @param clientId     客户端标识
     * @param clientSecret 客户端明文密钥
     * @return 机器令牌
     */
    TokenVO issueToken(String clientId, String clientSecret);
}
