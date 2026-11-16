package com.odin.registrationservice.dto;

import lombok.Data;

@Data
public class LifecycleTransitionResult {
    private Integer customerId;
    private String lifecycleSubjectId;
    private String lifecycleState;
    private Long lifecycleRevision;
    private Boolean stateChanged;
    private Boolean replayed;
}
