package com.example.oauth.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.IdClass;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * 用户-角色关联实体
 * <p>
 * 以关联实体 + 复合主键（@IdClass）的方式显式映射 sys_user_role 关联表，
 * 替代 SysUser 与 SysRole 之间的 @ManyToMany/@JoinTable 关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_user_role")
@IdClass(SysUserRole.PK.class)
public class SysUserRole implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 用户 ID */
    @Id
    @Column(name = "user_id")
    private Long userId;

    /** 角色 ID */
    @Id
    @Column(name = "role_id")
    private Long roleId;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long userId;
        private Long roleId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PK)) {
                return false;
            }
            PK pk = (PK) o;
            return Objects.equals(userId, pk.userId) && Objects.equals(roleId, pk.roleId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(userId, roleId);
        }
    }
}
