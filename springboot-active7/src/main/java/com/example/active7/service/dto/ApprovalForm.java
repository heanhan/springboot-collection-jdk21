package com.example.active7.service.dto;

import jakarta.validation.constraints.Size;

/**
 * 同意、拒绝或退回补充的审批表单。
 */
public class ApprovalForm {

    private boolean approved;
    private boolean returnToApplicant;

    @Size(max = 500, message = "审批意见最多 500 字")
    private String comment;

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public boolean isReturnToApplicant() {
        return returnToApplicant;
    }

    public void setReturnToApplicant(boolean returnToApplicant) {
        this.returnToApplicant = returnToApplicant;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
