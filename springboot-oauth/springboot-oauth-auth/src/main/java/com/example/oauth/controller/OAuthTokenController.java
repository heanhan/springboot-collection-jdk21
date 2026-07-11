package com.example.oauth.controller;

import com.example.oauth.common.exceptions.BizException;
import com.example.oauth.service.OAuthClientService;
import com.example.oauth.vo.TokenVO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * OAuth 令牌端点（client_credentials 客户端模式）
 * <p>
 * 供第三方应用/微服务以 client_id + client_secret 获取机器令牌（M2M Token）。
 * 该端点在白名单中放行（依靠 client_secret 自身鉴权）。
 */
@Slf4j
@RestController
@RequestMapping("/oauth")
@Api(tags = "OAuth 令牌端点")
public class OAuthTokenController {

    private final OAuthClientService clientService;

    public OAuthTokenController(OAuthClientService clientService) {
        this.clientService = clientService;
    }

    @PostMapping("/token")
    @ApiOperation("客户端令牌（client_credentials）")
    public TokenVO token(@RequestParam(value = "grant_type", defaultValue = "client_credentials") String grantType,
                         @RequestParam("client_id") String clientId,
                         @RequestParam("client_secret") String clientSecret) {
        if (!"client_credentials".equals(grantType)) {
            throw new BizException(400, "暂不支持的授权模式: " + grantType);
        }
        return clientService.issueToken(clientId, clientSecret);
    }
}
