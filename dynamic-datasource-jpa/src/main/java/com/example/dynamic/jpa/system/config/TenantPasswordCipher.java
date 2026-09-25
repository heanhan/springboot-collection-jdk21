package com.example.dynamic.jpa.system.config;

import com.alibaba.druid.filter.config.ConfigTools;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 租户库连接密码加解密（基于 Druid {@link ConfigTools} 的 RSA 方案）。
 *
 * <p>加密用私钥、解密用公钥：管理侧新增数据源时用私钥加密明文密码后入库，
 * 运行期 Druid ConfigFilter 用公钥解密。未开启加密时所有方法原样返回，保持向后兼容。</p>
 *
 * <p>密钥生成：{@code java -cp druid.jar com.alibaba.druid.filter.config.ConfigTools <明文密码>}
 * 会输出 privateKey / publicKey / 密文，将公私钥分别配置到
 * {@code app.datasource.crypto.private-key} 与 {@code app.datasource.crypto.public-key}。</p>
 *
 * @author zhaojh
 */
@Slf4j
@Component
public class TenantPasswordCipher {

    private final TenantDataSourceProperties.Crypto crypto;

    public TenantPasswordCipher(TenantDataSourceProperties properties) {
        this.crypto = properties.getCrypto();
    }

    public boolean isEnabled() {
        return crypto.isEnabled();
    }

    /**
     * 加密明文密码；未开启加密时原样返回。
     */
    public String encrypt(String plainText) {
        if (!crypto.isEnabled()) {
            return plainText;
        }
        if (StringUtils.isBlank(crypto.getPrivateKey())) {
            throw new IllegalStateException("已开启租户密码加密，但未配置 app.datasource.crypto.private-key");
        }
        if (plainText == null) {
            return null;
        }
        try {
            return ConfigTools.encrypt(crypto.getPrivateKey(), plainText);
        } catch (Exception e) {
            throw new IllegalStateException("租户数据库密码加密失败", e);
        }
    }

    /**
     * 解密密文密码（校验/排障用途）；未开启加密时原样返回。
     */
    public String decrypt(String cipherText) {
        if (!crypto.isEnabled()) {
            return cipherText;
        }
        if (cipherText == null) {
            return null;
        }
        try {
            return ConfigTools.decrypt(crypto.getPublicKey(), cipherText);
        } catch (Exception e) {
            throw new IllegalStateException("租户数据库密码解密失败", e);
        }
    }
}
