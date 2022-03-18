package ai.applica.spring.boot.starter.temporal;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import org.springframework.stereotype.Component;

@Component
@TemporalWorkflow("one")
public class HelloRemoteWorkflowImpl implements HelloWorkflow {

  @ActivityStub private RemoteActivities activities;

  @Override
  public String process() {
    return activities.echo("Hello remote!");
  }
}
