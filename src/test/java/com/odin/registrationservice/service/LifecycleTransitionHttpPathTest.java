package com.odin.registrationservice.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odin.registrationservice.dto.LifecycleTransitionResult;
import com.odin.registrationservice.repo.ProfileRepository;
import com.odin.registrationservice.utility.Utility;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class LifecycleTransitionHttpPathTest {
    @Test
    void lostHttpResponseRetriesSerializedCommandWithSameId() throws Exception {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        ObjectMapper mapper = new ObjectMapper();
        Utility utility = new Utility();
        ReflectionTestUtils.setField(utility, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(utility, "objectMapper", mapper);
        ProfileRepository repository = new ProfileRepository();
        ReflectionTestUtils.setField(repository, "coreUpdateUrl", "http://core/v1");
        ReflectionTestUtils.setField(repository, "utility", utility);
        LifecycleTransitionCoordinator coordinator = new LifecycleTransitionCoordinator(repository);
        List<JsonNode> attempts = new ArrayList<>();

        server.expect(once(), requestTo("http://core/v1/customer/lifecycle"))
                .andRespond(request -> {
                    attempts.add(mapper.readTree(
                            ((MockClientHttpRequest) request).getBodyAsString()));
                    throw new IOException("response lost");
                });
        server.expect(once(), requestTo("http://core/v1/customer/lifecycle"))
                .andRespond(request -> {
                    attempts.add(mapper.readTree(
                            ((MockClientHttpRequest) request).getBodyAsString()));
                    return withSuccess("{\"statusCode\":2000,\"status\":\"SUCCESS\","
                            + "\"data\":{\"lifecycleState\":\"ACTIVE\","
                            + "\"lifecycleRevision\":2}}", MediaType.APPLICATION_JSON)
                            .createResponse(request);
                });

        LifecycleTransitionResult result = coordinator.execute(
                70, "subject-a", "ACTIVATE", 1L);

        assertEquals(2L, result.getLifecycleRevision());
        assertEquals(2, attempts.size());
        assertEquals(attempts.get(0).get("commandId"), attempts.get(1).get("commandId"));
        assertEquals(attempts.get(0), attempts.get(1));
        server.verify();
    }
}
