package ai.applica.spring.boot.starter.temporal;

import ai.applica.spring.boot.starter.temporal.config.TemporalBootstrapConfiguration;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;

@Slf4j
@ConditionalOnBean(TemporalBootstrapConfiguration.class)
@Configuration
@RequiredArgsConstructor
public class RegisterDomain  {

  private final TemporalProperties temporalProperties;

  @EventListener
  public void register(ContextRefreshedEvent event) throws Exception {
    // log.debug("trying to register domain :{} using host:{} and port:{}", temporalProperties.getDomain(),
    //     temporalProperties.getHost(), temporalProperties.getPort());

    // IWorkflowService temporalService = new WorkflowServiceTChannel(
    //     temporalProperties.getHost(), temporalProperties.getPort());
    // RegisterDomainRequest request = new RegisterDomainRequest();
    // request.setDescription("sample domain");
    // request.setEmitMetric(false);
    // request.setName(temporalProperties.getDomain());
    // int retentionPeriodInDays = 5;
    // request.setWorkflowExecutionRetentionPeriodInDays(retentionPeriodInDays);
    // try {
    //   temporalService.RegisterDomain(request);
    //   log.debug("Successfully registered domain {} with retentionDays={}", temporalProperties.getDomain(),
    //       retentionPeriodInDays);
    // } catch (DomainAlreadyExistsError e) {
    //   log.error("domain  already exists {} {}", temporalProperties.getDomain(), e);
    // }

  }
}
