package com.odin.registrationservice.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LifecycleTransitionRequest {
    private String commandId;
    private Integer customerId;
    private String lifecycleSubjectId;
    private String operation;
    private Long expectedRevision;
}
