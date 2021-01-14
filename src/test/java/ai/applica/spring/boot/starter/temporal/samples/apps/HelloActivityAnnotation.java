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

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.RetryActivityOptions;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Functions;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Demonstrates activity retries using an exponential backoff algorithm. Requires a local instance
 * of the Temporal service to be running.
 */
@Slf4j
public class HelloActivityAnnotation {

  static final String TASK_QUEUE = "HelloActivityAnnotation";

  static final int NUMBER_OF_RETRIES = 5;

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

    /**
     * To enable activity retry set {@link RetryOptions} on {@link ActivityOptions}. It also works
     * for activities invoked through {@link io.temporal.workflow.Async#function(Functions.Func)}
     * and for child workflows.
     */
    @ActivityStub(
        duration = "PT100S",
        retryOptions =
            @RetryActivityOptions(
                initialInterval = 1,
                maximumAttempts = NUMBER_OF_RETRIES,
                doNotRetry = {
                  "ai.applica.spring.boot.starter.temporal.samples.apps.CustomActivityAnnotationException"
                }))
    private GreetingActivities activities;

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after activity is completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  @Service
  public static class GreetingActivitiesImpl implements GreetingActivities {
    private int callCount;
    private long lastInvocationTime;

    @Override
    public synchronized String composeGreeting(String greeting, String name) {
      if (lastInvocationTime != 0) {
        long timeSinceLastInvocation = System.currentTimeMillis() - lastInvocationTime;
        log.info("{} milliseconds since last invocation. ", timeSinceLastInvocation);
      }
      lastInvocationTime = System.currentTimeMillis();
      if (callCount++ < NUMBER_OF_RETRIES) {
        log.info("composeGreeting activity is going to fail for the {} time", callCount);
        throw new IllegalStateException("not yet");
      }
      log.error(
          "composeGreeting activity is going to complete after {} attempt",
          callCount); // should never go here
      throw new IllegalArgumentException("Activity should not be retry");
    }
  }
}
