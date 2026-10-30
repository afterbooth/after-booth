package spike;

import java.util.function.Supplier;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/** Text, tool calling and image input through Spring AI's ChatClient (OpenAI starter, Meta base URL). */
@Component
class SpringAiChecks {

  private final ChatClient chat;
  private final CallLog log;

  SpringAiChecks(ChatClient.Builder builder, CallLog log) {
    this.chat = builder.build();
    this.log = log;
  }

  void run(String image) {
    check(
        "spring-ai-text",
        "rack-sedan",
        () -> 1,
        () ->
            chat.prompt()
                .user("높이 180cm 진열대가 왜 일반 승용차에 실리지 않는지 한국어 한 문장으로 설명해 주세요.")
                .call()
                .chatResponse());

    VehicleTools tools = new VehicleTools();
    check(
        "spring-ai-tools",
        "check-vehicle-fit",
        () -> tools.invocations.get() + 1,
        () ->
            chat.prompt()
                .user("물품 101번 진열대를 승용차(SEDAN)로 가져갈 수 있는지 checkVehicleFit 도구로 확인하고 한국어 한 문장으로 답해 주세요.")
                .tools(tools)
                .call()
                .chatResponse());

    check(
        "spring-ai-image",
        "rack-gondola-single",
        () -> 1,
        () ->
            chat.prompt()
                .user(
                    u ->
                        u.text("이 사진 속 물건이 무엇인지, 대략 몇 cm 크기인지 한국어 한 문장으로 답해 주세요.")
                            .media(MimeTypeUtils.IMAGE_JPEG, new FileSystemResource(image)))
                .call()
                .chatResponse());
  }

  private void check(
      String capability, String inputId, Supplier<Integer> httpCalls, Supplier<ChatResponse> call) {
    long started = System.nanoTime();
    try {
      ChatResponse response = call.get();
      long latency = (System.nanoTime() - started) / 1_000_000;
      Usage usage = response.getMetadata().getUsage();
      log.record(
          new CallLog.Result(
              capability,
              inputId,
              true,
              latency,
              usage.getPromptTokens(),
              usage.getCompletionTokens(),
              httpCalls.get(),
              response.getResult().getOutput().getText(),
              null));
    } catch (RuntimeException e) {
      long latency = (System.nanoTime() - started) / 1_000_000;
      log.record(
          new CallLog.Result(
              capability,
              inputId,
              false,
              latency,
              null,
              null,
              httpCalls.get(),
              null,
              e.getClass().getName() + ": " + e.getMessage()));
    }
  }
}
