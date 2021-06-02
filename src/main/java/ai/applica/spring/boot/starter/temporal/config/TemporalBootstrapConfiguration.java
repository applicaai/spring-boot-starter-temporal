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

import static java.util.Optional.ofNullable;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowServiceStubOptions;
import ai.applica.spring.boot.starter.temporal.processors.ActivityAnnotationBeanPostProcessor;
import ai.applica.spring.boot.starter.temporal.processors.WorkflowAnnotationBeanPostProcessor;
import io.grpc.ManagedChannelBuilder;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableConfigurationProperties(TemporalProperties.class)
@Import({WorkflowAnnotationBeanPostProcessor.class, ActivityAnnotationBeanPostProcessor.class})
@RequiredArgsConstructor
public class TemporalBootstrapConfiguration {

  private final TemporalOptionsConfiguration temporalOptionsConfiguration;

  @Bean
  public ActivityCompletionClient defaultActivityCompletionClient(
      TemporalProperties temporalProperties) {
    return defaultClient(temporalProperties).newActivityCompletionClient();
  }

  @Bean
  public WorkflowFactory defaultWorkflowFactory(TemporalProperties temporalProperties) {
    return new WorkflowFactory(
        temporalProperties, defaultClient(temporalProperties), temporalOptionsConfiguration);
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
      if (temporalProperties.getUseSsl() == null || !temporalProperties.getUseSsl()) {
        channel.usePlaintext();
      }
      WorkflowServiceStubsOptions options =
          mapWorkflowServiceStubsOptions(channel, temporalProperties);
      service = WorkflowServiceStubs.newInstance(options);
    } else {
      // Get the default connection for the local docker
      service =
          WorkflowServiceStubs.newInstance(
              WorkflowServiceStubsOptions.newBuilder()
                  .setDisableHealthCheck(true)
                  .setEnableKeepAlive(false)
                  .build());
    }
    WorkflowClientOptions.Builder optionsBuilder =
        temporalOptionsConfiguration.modifyClientOptions(WorkflowClientOptions.newBuilder());
    return WorkflowClient.newInstance(service, optionsBuilder.build());
  }

  private WorkflowServiceStubsOptions mapWorkflowServiceStubsOptions(
      ManagedChannelBuilder<?> channel, TemporalProperties temporalProperties) {
    WorkflowServiceStubsOptions.Builder builder =
        WorkflowServiceStubsOptions.newBuilder()
            .setChannel(channel.build())
            .setEnableHttps(temporalProperties.getUseSsl());
    WorkflowServiceStubOptions options = temporalProperties.getWorkflowServiceStubOptions();
    if (options != null) {
      ofNullable(options.getDisableHealthCheck()).ifPresent(builder::setDisableHealthCheck);
      ofNullable(options.getHealthCheckAttemptTimeout())
          .ifPresent(builder::setHealthCheckAttemptTimeout);
      ofNullable(options.getHealthCheckTimeout()).ifPresent(builder::setHealthCheckTimeout);
      ofNullable(options.getEnableKeepAlive()).ifPresent(builder::setEnableKeepAlive);
      ofNullable(options.getKeepAliveTime()).ifPresent(builder::setKeepAliveTime);
      ofNullable(options.getKeepAliveTimeout()).ifPresent(builder::setKeepAliveTimeout);
      ofNullable(options.getKeepAlivePermitWithoutStream())
          .ifPresent(builder::setKeepAlivePermitWithoutStream);
      ofNullable(options.getRpcLongPollTimeout()).ifPresent(builder::setRpcLongPollTimeout);
      ofNullable(options.getRpcQueryTimeout()).ifPresent(builder::setRpcQueryTimeout);
      ofNullable(options.getRpcTimeout()).ifPresent(builder::setRpcTimeout);
      ofNullable(options.getConnectionBackoffResetFrequency())
          .ifPresent(builder::setConnectionBackoffResetFrequency);
      ofNullable(options.getGrpcReconnectFrequency()).ifPresent(builder::setGrpcReconnectFrequency);
    }
    return builder.build();
  }
}
