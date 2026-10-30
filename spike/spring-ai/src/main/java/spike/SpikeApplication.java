package spike;

import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

/**
 * Runs the Spring AI checks (default) or the RestClient fallback checks against Muse Spark.
 *
 * <p>{@code bootRun --args=spring-ai | rest | all}
 */
@SpringBootApplication
public class SpikeApplication {

  public static void main(String[] args) {
    SpringApplication.run(SpikeApplication.class, args);
  }

  @Bean
  CommandLineRunner run(
      SpringAiChecks springAi,
      RestClientChecks rest,
      CallLog callLog,
      @Value("${spike.image}") String image) {
    return args -> {
      String mode = args.length > 0 ? args[0] : "spring-ai";
      if (!List.of("spring-ai", "rest", "all").contains(mode)) {
        throw new IllegalArgumentException("mode must be spring-ai, rest or all: " + mode);
      }
      if (mode.equals("spring-ai") || mode.equals("all")) {
        springAi.run(image);
      }
      if (mode.equals("rest") || mode.equals("all")) {
        rest.run(image);
      }
      callLog.printSummary();
    };
  }
}
