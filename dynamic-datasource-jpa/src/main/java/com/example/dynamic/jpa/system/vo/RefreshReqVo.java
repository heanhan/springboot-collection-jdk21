package com.example.dynamic.jpa.system.vo;

import lombok.Data;
import lombok.ToString;

/**
 * 刷新 token 请求。
 *
 * @author zhaojh
 */
@Data
public class RefreshReqVo {

    @ToString.Exclude
    private String refreshToken;
}
