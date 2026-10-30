package com.afterbooth;

import org.springframework.boot.SpringApplication;

public class TestAfterboothApplication {

  public static void main(String[] args) {
    SpringApplication.from(AfterboothApplication::main)
        .with(TestcontainersConfiguration.class)
        .run(args);
  }
}
