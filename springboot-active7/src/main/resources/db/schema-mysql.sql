CREATE TABLE IF NOT EXISTS sys_user (
    user_id VARCHAR(64) PRIMARY KEY COMMENT '用户ID',
    username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录账号',
    password VARCHAR(100) NOT NULL COMMENT 'BCrypt 加密后的密码',
    display_name VARCHAR(64) NOT NULL COMMENT '姓名',
    department VARCHAR(64) COMMENT '所属部门',
    enabled BOOLEAN NOT NULL DEFAULT TRUE COMMENT '是否启用：1-启用，0-禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统用户表';

CREATE TABLE IF NOT EXISTS sys_role (
    role_code VARCHAR(32) PRIMARY KEY COMMENT '角色编码',
    role_name VARCHAR(64) NOT NULL COMMENT '角色名称',
    description VARCHAR(255) COMMENT '角色说明'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统角色表';

CREATE TABLE IF NOT EXISTS sys_user_role (
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    PRIMARY KEY (user_id, role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户与角色关系表';

CREATE TABLE IF NOT EXISTS leave_request (
    id VARCHAR(64) PRIMARY KEY COMMENT '请假申请ID',
    applicant VARCHAR(64) NOT NULL COMMENT '申请人登录账号',
    applicant_name VARCHAR(64) NOT NULL COMMENT '申请人姓名',
    department VARCHAR(64) COMMENT '申请部门',
    leave_type VARCHAR(32) NOT NULL COMMENT '请假类型',
    start_time DATETIME NOT NULL COMMENT '请假开始时间',
    end_time DATETIME NOT NULL COMMENT '请假结束时间',
    leave_days DECIMAL(5,1) NOT NULL COMMENT '请假天数',
    reason VARCHAR(500) NOT NULL COMMENT '请假事由',
    has_medical_proof BOOLEAN NOT NULL DEFAULT FALSE COMMENT '是否提供医疗证明：1-是，0-否',
    status VARCHAR(32) NOT NULL COMMENT '业务状态',
    current_node VARCHAR(64) COMMENT '当前流程节点编码',
    process_instance_id VARCHAR(64) COMMENT 'Activiti 流程实例ID',
    last_comment VARCHAR(500) COMMENT '最近一次审批意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_leave_request_applicant (applicant, created_at),
    INDEX idx_leave_request_status (status, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假申请表';

CREATE TABLE IF NOT EXISTS leave_record (
    id VARCHAR(64) PRIMARY KEY COMMENT '审批轨迹ID',
    record_seq BIGINT NOT NULL AUTO_INCREMENT COMMENT '轨迹顺序号，保证同一秒内的记录稳定排序',
    request_id VARCHAR(64) NOT NULL COMMENT '请假申请ID',
    node VARCHAR(64) NOT NULL COMMENT '流程节点编码',
    action VARCHAR(32) NOT NULL COMMENT '办理动作',
    operator VARCHAR(64) NOT NULL COMMENT '办理人登录账号',
    comment VARCHAR(500) COMMENT '办理意见',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '办理时间',
    UNIQUE KEY uk_leave_record_seq (record_seq),
    INDEX idx_leave_record_request (request_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='请假审批轨迹表';

CREATE TABLE IF NOT EXISTS leave_balance (
    user_id VARCHAR(64) NOT NULL COMMENT '用户ID',
    balance_year INT NOT NULL COMMENT '余额所属年份',
    annual_days DECIMAL(5,1) NOT NULL COMMENT '年假总额度',
    used_days DECIMAL(5,1) NOT NULL DEFAULT 0 COMMENT '已使用年假天数',
    PRIMARY KEY (user_id, balance_year)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工年假余额表';

CREATE TABLE IF NOT EXISTS sys_permission (
    permission_code VARCHAR(64) PRIMARY KEY COMMENT '权限编码',
    permission_name VARCHAR(64) NOT NULL COMMENT '权限名称',
    permission_type VARCHAR(16) NOT NULL COMMENT '权限类型：API-接口，MENU-菜单',
    resource VARCHAR(128) NOT NULL COMMENT '权限对应的资源或接口范围',
    description VARCHAR(255) COMMENT '权限说明'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统权限表';

CREATE TABLE IF NOT EXISTS sys_role_permission (
    role_code VARCHAR(32) NOT NULL COMMENT '角色编码',
    permission_code VARCHAR(64) NOT NULL COMMENT '权限编码',
    PRIMARY KEY (role_code, permission_code),
    INDEX idx_role_permission_permission (permission_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='角色与权限关系表';

CREATE TABLE IF NOT EXISTS sys_refresh_token (
    token_id VARCHAR(64) PRIMARY KEY COMMENT 'Refresh Token 的 SHA-256 哈希值',
    user_id VARCHAR(64) NOT NULL COMMENT '令牌所属用户ID',
    expires_at DATETIME NOT NULL COMMENT '令牌过期时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '令牌创建时间',
    revoked_at DATETIME COMMENT '令牌撤销时间，为空表示有效',
    INDEX idx_refresh_token_user (user_id, revoked_at),
    INDEX idx_refresh_token_expires (expires_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户刷新令牌表';
