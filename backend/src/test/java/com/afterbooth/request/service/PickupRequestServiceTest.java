package com.afterbooth.request.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.afterbooth.request.domain.PickupRequest;
import com.afterbooth.request.domain.RequestFailure;
import com.afterbooth.request.repository.PickupRequestRepository;
import com.afterbooth.request.repository.RequestIdempotency;
import com.afterbooth.request.repository.RequestReferences;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import tools.jackson.databind.json.JsonMapper;

class PickupRequestServiceTest {
  private final PickupRequestRepository repository = mock(PickupRequestRepository.class);
  private final RequestReferences references = mock(RequestReferences.class);
  private final RequestIdempotency idempotency = mock(RequestIdempotency.class);
  private final JsonMapper json = JsonMapper.builder().build();
  private final PickupRequestService service =
      new PickupRequestService(
          repository,
          references,
          idempotency,
          Clock.fixed(Instant.parse("2026-11-01T00:00:00Z"), ZoneOffset.UTC),
          json);
  private static final String BODY =
      "{\"organizationId\":2,\"category\":\"DISPLAY_RACK\",\"quantity\":2,\"vehicleType\":\"SEDAN\",\"purpose\":\"주말 행사\",\"transcript\":\"진열대 두 개\"}";

  @Test
  void persistsReviewedRequestAndReadsItForAllocation() {
    when(references.organizationExists(2L)).thenReturn(true);
    when(repository.saveAndFlush(any())).thenAnswer(call -> call.getArgument(0));
    var saved = service.create(UUID.randomUUID(), BODY);
    assertThat(saved.quantity()).isEqualTo(2);
    assertThat(saved.transcript()).isEqualTo("진열대 두 개");
    assertThat(saved.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    var captured = org.mockito.ArgumentCaptor.forClass(PickupRequest.class);
    verify(repository).saveAndFlush(captured.capture());
    when(repository.findById(12L)).thenReturn(Optional.of(captured.getValue()));
    assertThat(service.get(12L)).isEqualTo(saved);
    verify(idempotency).complete(any(), anyString());
  }

  @Test
  void replaysStoredResponseWithoutCreatingAnotherRequest() {
    String saved =
        "{\"id\":12,\"organizationId\":2,\"category\":\"DISPLAY_RACK\",\"quantity\":2,\"vehicleType\":\"SEDAN\",\"pickupDeadline\":\"2026-11-02T20:00:00+09:00\",\"createdAt\":\"2026-11-01T09:00:00+09:00\"}";
    when(idempotency.begin(any(), anyString(), any())).thenReturn(saved);
    var replayed = service.create(UUID.randomUUID(), BODY);
    assertThat(replayed.id()).isEqualTo(12L);
    assertThat(replayed.createdAt().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    assertThat(replayed.pickupDeadline().getOffset()).isEqualTo(ZoneOffset.ofHours(9));
    verifyNoInteractions(repository, references);
  }

  @ParameterizedTest
  @ValueSource(strings = {"0", "-1", "1.5", "\"2\"", "null", "2147483648"})
  void rejectsInvalidQuantityBeforeDatabaseAccess(String quantity) {
    String body = BODY.replace("\"quantity\":2", "\"quantity\":" + quantity);
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), body))
        .isInstanceOf(RequestFailure.class);
    verifyNoInteractions(repository, references, idempotency);
  }

  @Test
  void rejectsUnknownOrganization() {
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), BODY))
        .isInstanceOf(RequestFailure.class);
    verifyNoInteractions(repository);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "\"category\":0",
        "\"vehicleType\":0",
        "\"purpose\":2",
        "\"transcript\":true",
        "\"pickupDeadline\":123",
        "\"organizationId\":\"2\""
      })
  void rejectsScalarCoercionForKnownFields(String field) {
    String fieldName = field.substring(0, field.indexOf(':'));
    String body = BODY.replaceAll(fieldName + ":[^,}]+", field);
    if (!BODY.contains(fieldName)) body = BODY.substring(0, BODY.length() - 1) + "," + field + "}";
    String invalid = body;
    assertThatThrownBy(() -> service.create(UUID.randomUUID(), invalid))
        .isInstanceOf(RequestFailure.class);
    verifyNoInteractions(repository, references, idempotency);
  }
}
