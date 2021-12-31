/*
 *  Copyright (c) 2020 Applica.ai All Rights Reserved
 *
 *  Copyright (c) 2020 Temporal Technologies, Inc. All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Modifications copyright (C) 2017 Uber Technologies, Inc.
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

package ai.applica.spring.boot.starter.temporal.samples.apps;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityOptionsModifier;
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Demonstrates activity options modifier. Requires a local instance of the Temporal service to be
 * running.
 */
public class HelloActivityOptionsModifier {

  static final String TASK_QUEUE = "HelloActivityOptionsModifier";
  private static int MAX_ATTEMPTS = 2;

  @WorkflowInterface
  public interface GreetingWorkflow {
    /** @return greeting string */
    @WorkflowMethod
    String getGreeting(String name);
  }

  @ActivityInterface
  public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
  }

  /**
   * GreetingWorkflow implementation that demonstrates activity stub configured with {@link
   * RetryOptions}.
   */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    @ActivityStub(startToClose = "PT10S")
    private GreetingActivities activities;

    @ActivityOptionsModifier
    private ActivityOptions.Builder modifyOptions(
        Class<GreetingActivities> cls, ActivityOptions.Builder options) {
      options.setRetryOptions(
          RetryOptions.newBuilder()
              .setMaximumAttempts(
                  MAX_ATTEMPTS) // sets maximum attempts to call activity before ActivityFailure
              .build());
      return options;
    }

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after activity is completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  @Service
  public static class GreetingActivitiesImpl implements GreetingActivities {
    private int callCount;

    @Override
    public synchronized String composeGreeting(String greeting, String name) {
      if (++callCount < MAX_ATTEMPTS) {
        System.out.println("composeGreeting activity is going to fail");
        throw new IllegalStateException("not yet");
      }
      return greeting + " " + name + "!";
    }
  }
}
