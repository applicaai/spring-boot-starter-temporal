package ai.applica.spring.boot.starter.temporal.config;

import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import ai.applica.spring.boot.starter.temporal.annotations.EnableTemporal;
import ai.applica.spring.boot.starter.temporal.processors.WorkflowAnnotationBeanPostProcessor;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass({EnableTemporal.class})
@EnableConfigurationProperties(TemporalProperties.class)
@Import(WorkflowAnnotationBeanPostProcessor.class)
@RequiredArgsConstructor
public class TemporalBootstrapConfiguration {

  private final TemporalProperties temporalProperties;

  @Bean
  public WorkerFactory defaultWorkerFactory(TemporalProperties temporalProperties) {
    return WorkerFactory.newInstance(defaultClient(temporalProperties));
  }

  @Bean
  public WorkflowClient defaultClient(TemporalProperties temporalProperties) {
    // Get worker to poll the common task queue.
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
   
    return WorkflowClient.newInstance(service);
  }


}
