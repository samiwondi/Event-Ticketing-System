package com.example.demo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

class EventControllerIT extends AbstractIntegrationTest {

  @Autowired
  TestRestTemplate rest;

  @Test
  void listEvents_returnsEmptyPage() {
    ResponseEntity<String> res = rest.getForEntity(
      "/api/events?page=0&size=10",
      String.class
    );
    assertThat(res.getStatusCode().value()).isEqualTo(200);
    assertThat(res.getBody()).contains("\"content\":[]");
  }

  @Test
  void unknownEvent_returns404() {
    ResponseEntity<String> res = rest.getForEntity(
      "/api/events/00000000-0000-0000-0000-000000000000",
      String.class
    );
    assertThat(res.getStatusCode().value()).isEqualTo(404);
  }
}
