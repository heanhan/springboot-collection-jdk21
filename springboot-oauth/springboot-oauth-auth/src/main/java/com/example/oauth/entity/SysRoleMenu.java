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
 * 角色-菜单关联实体
 * <p>
 * 以关联实体 + 复合主键（@IdClass）的方式显式映射 sys_role_menu 关联表，
 * 替代 SysRole 与 SysMenu 之间的 @ManyToMany/@JoinTable 关系。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "sys_role_menu")
@IdClass(SysRoleMenu.PK.class)
public class SysRoleMenu implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 角色 ID */
    @Id
    @Column(name = "role_id")
    private Long roleId;

    /** 菜单 ID */
    @Id
    @Column(name = "menu_id")
    private Long menuId;

    /**
     * 复合主键类
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PK implements Serializable {

        private static final long serialVersionUID = 1L;

        private Long roleId;
        private Long menuId;

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof PK)) {
                return false;
            }
            PK pk = (PK) o;
            return Objects.equals(roleId, pk.roleId) && Objects.equals(menuId, pk.menuId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(roleId, menuId);
        }
    }
}
