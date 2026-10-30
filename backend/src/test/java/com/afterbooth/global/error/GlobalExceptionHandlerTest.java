package com.afterbooth.global.error;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(GlobalExceptionHandlerTest.FakeController.class)
@Import(GlobalExceptionHandlerTest.FakeController.class)
class GlobalExceptionHandlerTest {

  @Autowired MockMvc mockMvc;

  @Test
  void business_exception_maps_to_its_code_and_status() throws Exception {
    mockMvc
        .perform(get("/fake/missing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("대상을 찾을 수 없어요."));
  }

  @Test
  void validation_failure_maps_to_invalid_input() throws Exception {
    mockMvc
        .perform(post("/fake/qty").contentType(MediaType.APPLICATION_JSON).content("{\"qty\":0}"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void wrong_path_variable_type_maps_to_invalid_input() throws Exception {
    mockMvc
        .perform(get("/fake/items/abc"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

  @Test
  void unsupported_method_keeps_405() throws Exception {
    mockMvc
        .perform(post("/fake/missing"))
        .andExpect(status().isMethodNotAllowed())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_REQUEST"));
  }

  @Test
  void unsupported_media_type_keeps_415() throws Exception {
    mockMvc
        .perform(post("/fake/qty").contentType(MediaType.TEXT_PLAIN).content("qty=1"))
        .andExpect(status().isUnsupportedMediaType())
        .andExpect(jsonPath("$.code").value("UNSUPPORTED_REQUEST"));
  }

  @Test
  void unexpected_exception_hides_details() throws Exception {
    mockMvc
        .perform(get("/fake/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
        .andExpect(jsonPath("$.message").value("잠시 후 다시 시도해 주세요."));
  }

  @RestController
  static class FakeController {

    record QtyRequest(@Positive int qty) {}

    @GetMapping("/fake/missing")
    void missing() {
      throw new BusinessException(ErrorCode.NOT_FOUND);
    }

    @GetMapping("/fake/items/{id}")
    void item(@PathVariable Long id) {}

    @PostMapping("/fake/qty")
    void qty(@Valid @RequestBody QtyRequest request) {}

    @GetMapping("/fake/boom")
    void boom() {
      throw new IllegalStateException("db password leaked in message");
    }
  }
}
