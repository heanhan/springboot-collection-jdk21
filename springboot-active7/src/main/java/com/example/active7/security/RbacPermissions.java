package com.example.active7.security;

public final class RbacPermissions {

    public static final String AUTH_VIEW = "auth:view";
    public static final String LEAVE_CREATE = "leave:create";
    public static final String LEAVE_VIEW_MINE = "leave:view:mine";
    public static final String LEAVE_VIEW_ALL = "leave:view:all";
    public static final String LEAVE_VIEW_BALANCE = "leave:view:balance";
    public static final String LEAVE_WITHDRAW = "leave:withdraw";
    public static final String LEAVE_TASK_VIEW = "leave:task:view";
    public static final String LEAVE_TASK_CLAIM = "leave:task:claim";
    public static final String LEAVE_TASK_APPROVE = "leave:task:approve";
    public static final String LEAVE_TASK_RESUBMIT = "leave:task:resubmit";
    public static final String PROCESS_VIEW = "process:view";
    public static final String PROCESS_DEPLOY = "process:deploy";
    public static final String RBAC_VIEW = "rbac:view";
    public static final String RBAC_MANAGE = "rbac:manage";

    private RbacPermissions() {
    }
}
