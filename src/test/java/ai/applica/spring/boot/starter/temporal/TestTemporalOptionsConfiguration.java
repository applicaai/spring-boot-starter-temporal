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

import ai.applica.spring.boot.starter.temporal.config.TemporalOptionsConfiguration;
import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClientOptions.Builder;
import io.temporal.common.RetryOptions;
import io.temporal.serviceclient.WorkflowServiceStubsOptions;
import io.temporal.worker.WorkerFactoryOptions;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TestTemporalOptionsConfiguration implements TemporalOptionsConfiguration {

  @Override
  public Builder modifyClientOptions(Builder newBuilder) {
    return newBuilder;
  }

  @Override
  public io.temporal.client.WorkflowOptions.Builder modifyDefaultStubOptions(
      io.temporal.client.WorkflowOptions.Builder newBuilder) {
    return newBuilder;
  }

  @Override
  public ActivityOptions.Builder modifyDefaultActivityOptions(ActivityOptions.Builder newBuilder) {
    return newBuilder.setRetryOptions(
        RetryOptions.newBuilder()
            .setDoNotRetry(
                "ai.applica.spring.boot.starter.temporal.samples.apps.CustomActivityConfigurationException")
            .build());
  }

  @Override
  public WorkerFactoryOptions.Builder modifyDefaultWorkerFactoryOptions(
      WorkerFactoryOptions.Builder newBuilder) {
    return newBuilder;
  }

  @Override
  public WorkflowServiceStubsOptions.Builder modifyWorkflowServiceStubsOptions(
      WorkflowServiceStubsOptions.Builder builder) {
    return builder;
  }
}
