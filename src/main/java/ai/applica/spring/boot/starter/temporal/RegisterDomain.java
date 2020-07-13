/*
 *  Copyright (c) 2020 Applica.ai All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

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
public class RegisterDomain {

  private final TemporalProperties temporalProperties;

  @EventListener
  public void register(ContextRefreshedEvent event) throws Exception {

    // RegisterDomainRequest request = new RegisterDomainRequest();
    // request.setDescription("sample domain");
    // request.setEmitMetric(false);
    // request.setName(temporalProperties.getDomain());
    // int retentionPeriodInDays = 5;
    // request.setWorkflowExecutionRetentionPeriodInDays(retentionPeriodInDays);
    // try {
    //   temporalService.RegisterDomain(request);
    //   log.debug("Successfully registered domain {} with retentionDays={}",
    // temporalProperties.getDomain(),
    //       retentionPeriodInDays);
    // } catch (DomainAlreadyExistsError e) {
    //   log.error("domain  already exists {} {}", temporalProperties.getDomain(), e);
    // }

  }
}
