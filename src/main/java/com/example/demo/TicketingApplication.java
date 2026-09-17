package com.example.demo;

import com.example.demo.cli.MenuController;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TicketingApplication {

  public static void main(String[] args) {
    boolean cliMode = false;
    for (String a : args) {
      if (a.equals("--cli")) {
        cliMode = true;
        break;
      }
    }

    SpringApplication app = new SpringApplication(TicketingApplication.class);
    if (cliMode) {
      app.setWebApplicationType(
        org.springframework.boot.WebApplicationType.NONE
      );
      // Disable the scheduled sweeper in CLI mode
      app.setAdditionalProfiles("cli");
    }

    ConfigurableApplicationContext ctx = app.run(args);

    if (cliMode) {
      MenuController menu = ctx.getBean(MenuController.class);
      menu.run();
      ctx.close();
    }
  }
}
