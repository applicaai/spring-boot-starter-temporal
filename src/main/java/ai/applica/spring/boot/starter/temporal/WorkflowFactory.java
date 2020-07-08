package ai.applica.spring.boot.starter.temporal;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import java.time.Duration;
import lombok.RequiredArgsConstructor;

/**
 * E - workflow interface
 * I - workflow implementation
 * Two parameters so to autowire both by interface and implementation 
 */
@RequiredArgsConstructor
public class WorkflowFactory<E, I> {

  private final WorkflowClient workflowClient;
  private final WorkflowOption option;
  private final String key;
  private final Class<E> clazzInterface;

  public E next() {
    WorkflowOptions options = WorkflowOptions.newBuilder()
        .setTaskQueue(key)
        .setWorkflowExecutionTimeout(
            Duration.ofSeconds(option.getExecutionTimeout()))
        .build();

    return workflowClient.newWorkflowStub(clazzInterface, options);
  }

}
