package com.afterbooth.intake.controller;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.afterbooth.intake.domain.IntakeError;
import com.afterbooth.intake.domain.IntakeException;
import com.afterbooth.intake.domain.IntakeStage;
import com.afterbooth.intake.domain.IntakeStatus;
import com.afterbooth.intake.dto.IntakeDraftResponse;
import com.afterbooth.intake.service.IntakeService;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(IntakeController.class)
class IntakeControllerTest {
  @Autowired MockMvc mvc;
  @MockitoBean IntakeService service;

  @Test
  void createReturnsAcceptedProcessingSnapshot() throws Exception {
    when(service.create(any(), eq(1L), any(), eq("image/png")))
        .thenReturn(
            new IntakeDraftResponse(
                2L,
                1L,
                IntakeStatus.PROCESSING,
                IntakeStage.ESTIMATE,
                null,
                List.of(),
                OffsetDateTime.parse("2026-10-31T12:00:00+09:00")));
    mvc.perform(
            multipart("/api/intake/drafts")
                .file(new MockMultipartFile("photo", "photo.png", "image/png", new byte[] {1}))
                .param("eventId", "1")
                .header("Idempotency-Key", UUID.randomUUID()))
        .andExpect(status().isAccepted())
        .andExpect(jsonPath("$.status").value("PROCESSING"))
        .andExpect(jsonPath("$.stage").value("ESTIMATE"))
        .andExpect(jsonPath("$.failureMessage").doesNotExist())
        .andExpect(jsonPath("$.createdAt").value("2026-10-31T12:00:00+09:00"));
  }

  @Test
  void missingAndMalformedKeyReturnInvalidInput() throws Exception {
    mvc.perform(
            multipart("/api/intake/drafts")
                .file(new MockMultipartFile("photo", new byte[] {1}))
                .param("eventId", "1"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    mvc.perform(
            multipart("/api/intake/drafts")
                .file(new MockMultipartFile("photo", new byte[] {1}))
                .param("eventId", "1")
                .header("Idempotency-Key", "bad"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    verifyNoInteractions(service);
  }

  @Test
  void rejectsUnknownCategoryAndNonPositiveQuantityAndEmptyLines() throws Exception {
    for (String body :
        List.of(
            "{\"lines\":[]}",
            "{\"lines\":[{\"lineId\":1,\"category\":\"UNKNOWN\",\"quantity\":1,\"sizeCm\":{\"width\":1,\"depth\":1,\"height\":1},\"condition\":\"GOOD\"}]}",
            "{\"lines\":[{\"lineId\":1,\"category\":\"DISPLAY_RACK\",\"quantity\":0,\"sizeCm\":{\"width\":1,\"depth\":1,\"height\":1},\"condition\":\"GOOD\"}]}")) {
      mvc.perform(
              post("/api/intake/drafts/1/confirm")
                  .header("Idempotency-Key", UUID.randomUUID())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(body))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
    verifyNoInteractions(service);
  }

  @Test
  void mapsDraftConflictToContractError() throws Exception {
    when(service.get(1L)).thenThrow(new IntakeException(IntakeError.DRAFT_NOT_READY));
    mvc.perform(get("/api/intake/drafts/1"))
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.code").value("DRAFT_NOT_READY"));
  }

  @Autowired
  @org.springframework.beans.factory.annotation.Qualifier("handlerExceptionResolver")
  org.springframework.web.servlet.HandlerExceptionResolver resolver;

  @Test
  void multipartLimitMapsBeforeAHandlerIsSelected() throws Exception {
    var request =
        new org.springframework.mock.web.MockHttpServletRequest("POST", "/api/intake/drafts");
    var response = new org.springframework.mock.web.MockHttpServletResponse();
    resolver.resolveException(
        request,
        response,
        null,
        new org.springframework.web.multipart.MaxUploadSizeExceededException(10 * 1024 * 1024));
    org.assertj.core.api.Assertions.assertThat(response.getStatus()).isEqualTo(400);
    org.assertj.core.api.Assertions.assertThat(response.getContentAsString())
        .contains("INVALID_INPUT");
  }

  @Autowired tools.jackson.databind.json.JsonMapper json;

  @Test
  void persistedDraftResponseRetainsSeoulOffsetOnReplay() {
    var first =
        new IntakeDraftResponse(
            2L,
            1L,
            IntakeStatus.PROCESSING,
            IntakeStage.ESTIMATE,
            null,
            List.of(),
            OffsetDateTime.parse("2026-10-31T12:00:00+09:00"));
    var replay = json.readValue(json.writeValueAsString(first), IntakeDraftResponse.class);
    org.assertj.core.api.Assertions.assertThat(replay).isEqualTo(first);
    org.assertj.core.api.Assertions.assertThat(json.writeValueAsString(replay))
        .contains("2026-10-31T12:00:00+09:00");
  }
}
