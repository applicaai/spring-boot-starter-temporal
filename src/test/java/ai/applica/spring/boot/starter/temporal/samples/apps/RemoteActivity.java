package ai.applica.spring.boot.starter.temporal.samples.apps;

import ai.applica.spring.boot.starter.temporal.HelloWorkflow;
import ai.applica.spring.boot.starter.temporal.RemoteActivities;
import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.EnableTemporal;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

public class RemoteActivity {

  @Component
  @TemporalWorkflow("one")
  public static class NoHelloWorkflowImpl implements HelloWorkflow {

    @ActivityStub private RemoteActivities activities;

    @Override
    public String process() {
      return null;
    }
  }

  @EnableTemporal
  @SpringBootApplication()
  public static class RemoteActivityApp implements CommandLineRunner {

    @Autowired private WorkflowFactory fact;

    public void run(String... input) {

      fact.makeStub(HelloWorkflow.class, NoHelloWorkflowImpl.class);
      System.out.println("\n\nHelloWorkflow stub created successfully!\n\n");

      System.exit(0);
    }
  }

  /**
   * Runs an application that creates a workflow stub of type {@link HelloWorkflow} with a local
   * implementation of type {@link NoHelloWorkflowImpl}. The class {@link NoHelloWorkflowImpl} has
   * an activity stub of type {@link RemoteActivities} which does not have a local implementation -
   * it simulates a situation where one service orchestrates activities on other services.
   *
   * <p>This application tests that the {@link org.springframework.context.ApplicationContext} loads
   * beans successfully, given that the configuration of {@link RemoteActivities} is in the YAML
   * file - under {@code spring.temporal.activityStubs}.
   */
  public static void main(String[] args) {
    SpringApplication.run(RemoteActivityApp.class, args);
  }
}
