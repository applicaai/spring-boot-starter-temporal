package ai.applica.spring.boot.starter.temporal;

import org.springframework.stereotype.Service;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;

@Service
@TemporalWorkflow("two")
public class HelloWorkflowImplTwo implements HelloWorkflow {

  @ActivityStub(durationInSeconds = 10)
  private SimpleService simpleService;

 
  @Override
  public String process() {
    
    return "Hello from " + this.getClass() + " including " + simpleService.say("Kuku");
  }

}
