package com.example.active7.workflow;

/**
 * 流程节点和业务状态集中管理，避免 Java 与 BPMN 中写重复字符串。
 */
public final class LeaveProcessConstants {

    public static final String PROCESS_KEY = "leaveApproval";

    public static final String NODE_MANAGER = "managerApproval";
    public static final String NODE_HR = "hrApproval";
    public static final String NODE_DIRECTOR = "directorApproval";
    public static final String NODE_RESUBMIT = "employeeResubmit";
    public static final String NODE_CONFIRM = "employeeConfirm";
    public static final String NODE_END = "END";

    public static final String STATUS_SUBMITTED = "SUBMITTED";
    public static final String STATUS_RETURNED = "RETURNED";
    public static final String STATUS_MANAGER_APPROVED = "MANAGER_APPROVED";
    public static final String STATUS_HR_APPROVED = "HR_APPROVED";
    public static final String STATUS_DIRECTOR_APPROVED = "DIRECTOR_APPROVED";
    public static final String STATUS_CONFIRM_PENDING = "CONFIRM_PENDING";
    public static final String STATUS_APPROVED = "APPROVED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String STATUS_WITHDRAWN = "WITHDRAWN";

    public static final String TYPE_ANNUAL = "ANNUAL";
    public static final String TYPE_SICK = "SICK";
    public static final String TYPE_PERSONAL = "PERSONAL";
    public static final String TYPE_MARRIAGE = "MARRIAGE";
    public static final String TYPE_BEREAVEMENT = "BEREAVEMENT";

    private LeaveProcessConstants() {
    }
}
