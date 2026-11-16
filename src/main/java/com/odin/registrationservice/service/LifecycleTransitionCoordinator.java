package com.odin.registrationservice.service;

import com.odin.registrationservice.dto.LifecycleTransitionRequest;
import com.odin.registrationservice.dto.LifecycleTransitionResult;
import com.odin.registrationservice.repo.ProfileRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;

@Component
@RequiredArgsConstructor
public class LifecycleTransitionCoordinator {
    private final ProfileRepository profileRepository;

    public LifecycleTransitionResult execute(Integer customerId, String lifecycleSubjectId,
            String operation, Long expectedRevision) {
        LifecycleTransitionRequest request = LifecycleTransitionRequest.builder()
                .commandId(UUID.randomUUID().toString())
                .customerId(customerId)
                .lifecycleSubjectId(lifecycleSubjectId)
                .operation(operation)
                .expectedRevision(expectedRevision)
                .build();
        try {
            return profileRepository.transition(request);
        } catch (RuntimeException failure) {
            if (!hasResourceAccessCause(failure)) {
                throw failure;
            }
            return profileRepository.transition(request);
        }
    }

    private boolean hasResourceAccessCause(Throwable failure) {
        Throwable current = failure;
        while (current != null) {
            if (current instanceof ResourceAccessException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
