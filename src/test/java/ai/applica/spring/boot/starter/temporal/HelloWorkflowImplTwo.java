package ai.applica.spring.boot.starter.temporal;

import ai.applica.spring.boot.starter.temporal.annotations.Workflow;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Workflow("two")
public class HelloWorkflowImplTwo implements HelloWorkflow {

  @Autowired
  private SimpleService simpleService;

  @Override
  public String process() {
    return "Hello from " + this.getClass() + " including " + simpleService.getClass();
  }

}
