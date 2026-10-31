package com.afterbooth.request.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.afterbooth.request.domain.ConditionContext;
import com.afterbooth.request.domain.RequestFailure;
import com.afterbooth.request.dto.ConditionInterpretation;
import com.afterbooth.request.dto.Conditions;
import com.afterbooth.request.service.ConditionService;
import com.afterbooth.request.service.PickupRequestService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest({ConditionController.class, PickupRequestController.class})
class RequestControllerTest {
  @Autowired MockMvc mvc;
  @Autowired org.springframework.core.env.Environment environment;
  @MockitoBean ConditionService conditions;
  @MockitoBean PickupRequestService requests;

  @Test
  void missingContextAndIdempotencyKeyAreInvalidInput() throws Exception {
    mvc.perform(multipart("/api/conditions/interpret").param("text", "진열대"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    mvc.perform(post("/api/pickup-requests").contentType("application/json").content("{}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void invalidAudioUses415ContractError() throws Exception {
    when(conditions.interpret(any(), any(), any(), any(), any()))
        .thenThrow(RequestFailure.unsupported());
    mvc.perform(
            multipart("/api/conditions/interpret")
                .file(new MockMultipartFile("audio", "clip.ogg", "audio/ogg", new byte[] {1}))
                .param("context", "RECEIVER"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_REQUEST"));
  }

  @Test
  void exhaustedReaskUses503ContractError() throws Exception {
    when(conditions.interpret(any(), any(), any(), any(), any()))
        .thenThrow(RequestFailure.unavailable());
    mvc.perform(
            multipart("/api/conditions/interpret")
                .param("context", "RECEIVER")
                .param("text", "진열대"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.code").value("AI_UNAVAILABLE"));
  }

  @Test
  void responseOmitsOptionalFieldsRatherThanReturningNull() throws Exception {
    when(conditions.interpret(any(), any(), any(), any(), any()))
        .thenReturn(
            new ConditionInterpretation(
                ConditionContext.RECEIVER,
                "행사",
                new Conditions(null, null, null, null, "행사"),
                List.of("category", "vehicleType")));
    mvc.perform(
            multipart("/api/conditions/interpret").param("context", "RECEIVER").param("text", "행사"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.conditions.purpose").value("행사"))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("null"))));
  }

  @Test
  void configuredMultipartLimitsAllowSixtySecondWavWithFraming() {
    long fileLimit =
        org.springframework.util.unit.DataSize.parse(
                environment.getRequiredProperty("spring.servlet.multipart.max-file-size"))
            .toBytes();
    long requestLimit =
        org.springframework.util.unit.DataSize.parse(
                environment.getRequiredProperty("spring.servlet.multipart.max-request-size"))
            .toBytes();
    org.assertj.core.api.Assertions.assertThat(fileLimit).isGreaterThanOrEqualTo(1920044L);
    org.assertj.core.api.Assertions.assertThat(requestLimit).isGreaterThan(fileLimit);
  }
}
