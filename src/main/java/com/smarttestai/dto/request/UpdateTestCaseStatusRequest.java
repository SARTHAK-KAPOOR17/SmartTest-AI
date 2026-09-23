package com.smarttestai.dto.request;

import com.smarttestai.entity.TestCaseStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public class UpdateTestCaseStatusRequest {

    @NotNull(message = "Review status is required (GENERATED, REVIEWED, APPROVED, REJECTED)")
    @Schema(description = "Target review status", example = "APPROVED")
    private TestCaseStatus status;

    @Schema(description = "Optional reviewer note or justification", example = "Approved for automated Selenium script generation")
    private String reviewComment;

    public UpdateTestCaseStatusRequest() {
    }

    public UpdateTestCaseStatusRequest(TestCaseStatus status, String reviewComment) {
        this.status = status;
        this.reviewComment = reviewComment;
    }

    public TestCaseStatus getStatus() {
        return status;
    }

    public void setStatus(TestCaseStatus status) {
        this.status = status;
    }

    public String getReviewComment() {
        return reviewComment;
    }

    public void setReviewComment(String reviewComment) {
        this.reviewComment = reviewComment;
    }
}
