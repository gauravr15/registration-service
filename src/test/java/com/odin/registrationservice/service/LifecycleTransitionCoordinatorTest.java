package com.odin.registrationservice.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.odin.registrationservice.dto.LifecycleTransitionRequest;
import com.odin.registrationservice.dto.LifecycleTransitionResult;
import com.odin.registrationservice.repo.ProfileRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.client.ResourceAccessException;

class LifecycleTransitionCoordinatorTest {
    private final ProfileRepository repository = mock(ProfileRepository.class);
    private final LifecycleTransitionCoordinator coordinator =
            new LifecycleTransitionCoordinator(repository);

    @Test
    void lostResponseRetryReusesCommandId() {
        when(repository.transition(any())).thenThrow(
                new RuntimeException(new ResourceAccessException("connection lost")))
                .thenReturn(result(2L));

        assertEquals(2L, coordinator.execute(70, "subject-a", "ACTIVATE", 1L)
                .getLifecycleRevision());

        ArgumentCaptor<LifecycleTransitionRequest> requests =
                ArgumentCaptor.forClass(LifecycleTransitionRequest.class);
        verify(repository, times(2)).transition(requests.capture());
        assertSame(requests.getAllValues().get(0), requests.getAllValues().get(1));
        assertEquals(requests.getAllValues().get(0).getCommandId(),
                requests.getAllValues().get(1).getCommandId());
    }

    @Test
    void independentActivationsUseDifferentUnpredictableIds() {
        when(repository.transition(any())).thenReturn(result(2L), result(4L));
        coordinator.execute(70, "subject-a", "ACTIVATE", 1L);
        coordinator.execute(70, "subject-a", "ACTIVATE", 3L);
        ArgumentCaptor<LifecycleTransitionRequest> requests =
                ArgumentCaptor.forClass(LifecycleTransitionRequest.class);
        verify(repository, times(2)).transition(requests.capture());
        assertNotEquals(requests.getAllValues().get(0).getCommandId(),
                requests.getAllValues().get(1).getCommandId());
        requests.getAllValues().forEach(request ->
                assertDoesNotThrow(() -> UUID.fromString(request.getCommandId())));
    }

    @Test
    void semanticFailureIsNotRetried() {
        RuntimeException failure = new RuntimeException("conflict");
        when(repository.transition(any())).thenThrow(failure);
        assertSame(failure, assertThrows(RuntimeException.class,
                () -> coordinator.execute(70, "subject-a", "ACTIVATE", 1L)));
        verify(repository).transition(any());
    }

    private LifecycleTransitionResult result(long revision) {
        LifecycleTransitionResult result = new LifecycleTransitionResult();
        result.setLifecycleRevision(revision);
        return result;
    }
}
