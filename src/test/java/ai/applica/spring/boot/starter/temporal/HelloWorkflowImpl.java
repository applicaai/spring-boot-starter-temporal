package ai.applica.spring.boot.starter.temporal;

import org.springframework.stereotype.Service;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;

@Service
@TemporalWorkflow("one")
public class HelloWorkflowImpl implements HelloWorkflow {

  @ActivityStub(durationInSeconds =  10)
  public SimpleService simpleService;

  @Override
  public String process() {
    return "Hello from " + this.getClass() + " including " + simpleService.say("Stuu");
  }
}
