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

  //  @Component
  //  @TemporalWorkflow("EchoWorkflow")
  //  public static class EchoNullWorkflowImpl implements EchoWorkflow {
  //
  //    @ActivityStub private RemoteActivities activities;
  //
  //    @Override
  //    public String doEcho(String word) {
  //      return null;
  //    }
  //  }

  @Component
  @TemporalWorkflow("one")
  public static class NullWorkflowImpl implements HelloWorkflow {

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

    public void run(String... input) throws Exception {
      // Start a workflow execution. This will trigger bean creation
      //      EchoWorkflow workflow = fact.makeStub(EchoWorkflow.class, EchoNullWorkflowImpl.class);
      fact.makeStub(HelloWorkflow.class, NullWorkflowImpl.class);
      System.out.println("\n\nEchoWorkflow stub created successfully!\n\n");

      System.exit(0);
    }
  }

  public static void main(String[] args) {
    // Wait for workflow completion.
    SpringApplication.run(RemoteActivityApp.class, args);
  }
}
