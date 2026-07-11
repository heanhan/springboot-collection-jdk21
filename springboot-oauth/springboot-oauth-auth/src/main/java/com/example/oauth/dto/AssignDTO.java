package com.example.oauth.dto;

import lombok.Data;

import java.util.List;

/**
 * 通用「分配」请求 DTO
 * <p>
 * 用于给用户分配角色、给角色分配权限/菜单等场景，承载一组目标 ID。
 */
@Data
public class AssignDTO {

    /** 目标 ID 列表（角色 ID / 权限 ID / 菜单 ID） */
    private List<Long> ids;
}
