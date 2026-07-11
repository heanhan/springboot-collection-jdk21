package com.example.oauth.service.impl;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.entity.OAuthClient;
import com.example.oauth.repository.OAuthClientRepository;
import com.example.oauth.service.OAuthClientService;
import com.example.oauth.service.TokenService;
import com.example.oauth.vo.TokenVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Collectors;

/**
 * OAuth 客户端服务实现
 */
@Slf4j
@Service
public class OAuthClientServiceImpl implements OAuthClientService {

    private static final String GRANT_CLIENT_CREDENTIALS = "client_credentials";

    private final OAuthClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;

    public OAuthClientServiceImpl(OAuthClientRepository clientRepository,
                                  PasswordEncoder passwordEncoder,
                                  TokenService tokenService) {
        this.clientRepository = clientRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public Page<OAuthClient> page(String keyword, Pageable pageable) {
        if (StringUtils.hasText(keyword)) {
            return clientRepository.findByClientNameContaining(keyword, pageable);
        }
        return clientRepository.findAll(pageable);
    }

    @Override
    public OAuthClient getById(Long id) {
        return clientRepository.findById(id)
                .orElseThrow(() -> new BizException(404, "客户端不存在: " + id));
    }

    @Override
    @Transactional
    public OAuthClient create(OAuthClient client) {
        if (!StringUtils.hasText(client.getClientId())) {
            throw new BizException(400, "客户端标识 clientId 不能为空");
        }
        if (!StringUtils.hasText(client.getClientSecret())) {
            throw new BizException(400, "客户端密钥 clientSecret 不能为空");
        }
        if (clientRepository.existsByClientId(client.getClientId())) {
            throw new BizException(400, "客户端标识已存在: " + client.getClientId());
        }
        client.setId(null);
        client.setClientSecret(passwordEncoder.encode(client.getClientSecret()));
        if (!StringUtils.hasText(client.getGrantTypes())) {
            client.setGrantTypes(GRANT_CLIENT_CREDENTIALS);
        }
        if (client.getStatus() == null) {
            client.setStatus(1);
        }
        return clientRepository.save(client);
    }

    @Override
    @Transactional
    public OAuthClient update(OAuthClient client) {
        if (client.getId() == null) {
            throw new BizException(400, "更新客户端时 ID 不能为空");
        }
        OAuthClient existing = getById(client.getId());
        existing.setClientName(client.getClientName());
        existing.setScopes(client.getScopes());
        existing.setGrantTypes(client.getGrantTypes());
        existing.setAccessTokenValidity(client.getAccessTokenValidity());
        if (client.getStatus() != null) {
            existing.setStatus(client.getStatus());
        }
        // clientId 不可修改；clientSecret 非空才更新
        if (StringUtils.hasText(client.getClientSecret())) {
            existing.setClientSecret(passwordEncoder.encode(client.getClientSecret()));
        }
        return clientRepository.save(existing);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        clientRepository.deleteById(id);
    }

    @Override
    public TokenVO issueToken(String clientId, String clientSecret) {
        if (!StringUtils.hasText(clientId) || !StringUtils.hasText(clientSecret)) {
            throw new BizException(400, "client_id 与 client_secret 不能为空");
        }
        OAuthClient client = clientRepository.findByClientId(clientId);
        if (client == null) {
            throw new BizException(401, "客户端不存在或凭证无效");
        }
        if (client.getStatus() != null && client.getStatus() == 0) {
            throw new BizException(403, "客户端已被禁用: " + clientId);
        }
        if (!passwordEncoder.matches(clientSecret, client.getClientSecret())) {
            throw new BizException(401, "客户端不存在或凭证无效");
        }
        if (StringUtils.hasText(client.getGrantTypes())
                && !client.getGrantTypes().contains(GRANT_CLIENT_CREDENTIALS)) {
            throw new BizException(403, "客户端不支持 client_credentials 授权模式");
        }
        Collection<String> scopes = parseScopes(client.getScopes());
        long ttl = client.getAccessTokenValidity() == null ? 0L : client.getAccessTokenValidity();
        return tokenService.generateClientToken(clientId, scopes, ttl);
    }

    private Collection<String> parseScopes(String scopes) {
        if (!StringUtils.hasText(scopes)) {
            return Collections.emptyList();
        }
        return Arrays.stream(scopes.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toList());
    }
}
