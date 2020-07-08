package ai.applica.spring.boot.starter.temporal;

import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

@WorkflowInterface
public interface HelloWorkflow {

  @WorkflowMethod
  String process();
}
