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

package ai.applica.spring.boot.starter.temporal.config;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.EnableTemporal;
import ai.applica.spring.boot.starter.temporal.processors.WorkflowAnnotationBeanPostProcessor;
import io.grpc.ManagedChannelBuilder;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnClass({EnableTemporal.class})
@EnableConfigurationProperties(TemporalProperties.class)
@Import(WorkflowAnnotationBeanPostProcessor.class)
@RequiredArgsConstructor
public class TemporalBootstrapConfiguration {
  @Bean
  public ActivityCompletionClient defaultActivitiCompletitionClient(
      TemporalProperties temporalProperties) {
    return defaultClient(temporalProperties).newActivityCompletionClient();
  }

  @Bean
  public WorkflowFactory defaulWorkflowFactory(TemporalProperties temporalProperties) {
    return new WorkflowFactory(temporalProperties, defaultClient(temporalProperties));
  }

  @Bean
  public WorkerFactory defaultWorkerFactory(TemporalProperties temporalProperties) {
    return WorkerFactory.newInstance(defaultClient(temporalProperties));
  }

  @Bean
  public WorkflowClient defaultClient(TemporalProperties temporalProperties) {
    WorkflowServiceStubs service;
    // Get worker to poll the common task queue.
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    if (temporalProperties.getHost() != null) {
      ManagedChannelBuilder<?> channel =
          ManagedChannelBuilder.forAddress(
              temporalProperties.getHost(), temporalProperties.getPort());
      if (!temporalProperties.getUseSsl()) {
        channel.usePlaintext();
      }
      WorkflowServiceStubsOptions options =
          WorkflowServiceStubsOptions.newBuilder()
              .setChannel(channel.build())
              .setEnableHttps(temporalProperties.getUseSsl())
              .build();
      service = WorkflowServiceStubs.newInstance(options);
    } else {
      // Get the default connection for the local docker
      service = WorkflowServiceStubs.newInstance();
    }
    return WorkflowClient.newInstance(service);
  }
}
