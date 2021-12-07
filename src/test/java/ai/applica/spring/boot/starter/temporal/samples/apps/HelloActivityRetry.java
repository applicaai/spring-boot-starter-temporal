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

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.ActivityOptionsModifier;
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.EnableTemporal;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Functions;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Demonstrates activity retries using an exponential backoff algorithm. Requires a local instance
 * of the Temporal service to be running.
 */
public class HelloActivityRetry {

  static final String TASK_QUEUE = "HelloActivityRetry";

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
    @ActivityStub(startToClose = "PT10S")
    private GreetingActivities activities;

    @ActivityOptionsModifier
    private ActivityOptions.Builder modifyOptions(
        Class<GreetingActivities> cls, ActivityOptions.Builder options) {
      options.setRetryOptions(
          RetryOptions.newBuilder()
              .setInitialInterval(Duration.ofSeconds(1))
              .setDoNotRetry(IllegalArgumentException.class.getName())
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
    private long lastInvocationTime;

    @Override
    public synchronized String composeGreeting(String greeting, String name) {
      if (lastInvocationTime != 0) {
        long timeSinceLastInvocation = System.currentTimeMillis() - lastInvocationTime;
        System.out.print(timeSinceLastInvocation + " milliseconds since last invocation. ");
      }
      lastInvocationTime = System.currentTimeMillis();
      if (++callCount < 4) {
        System.out.println("composeGreeting activity is going to fail");
        throw new IllegalStateException("not yet");
      }
      System.out.println("composeGreeting activity is going to complete");
      return greeting + " " + name + "!";
    }
  }

  @EnableTemporal
  @SpringBootApplication
  public static class GreetingWorkflowRequester implements CommandLineRunner {

    @Autowired private WorkflowFactory fact;

    public void run(String... input) throws Exception {
      // Start a workflow execution. Usually this is done from another program or bean.
      // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
      GreetingWorkflow workflow = fact.makeStub(GreetingWorkflow.class, GreetingWorkflowImpl.class);

      // Execute a workflow waiting for it to complete. See {@link
      // io.temporal.samples.hello.HelloSignal}
      // for an example of starting workflow without waiting synchronously for its result.
      String greeting = workflow.getGreeting("World");
      System.out.println("\n\n");
      System.out.println(greeting);
      System.out.println("\n\n");
      System.exit(0);
    }
  }

  public static void main(String[] args) {
    // Wait for workflow completion.
    SpringApplication.run(GreetingWorkflowRequester.class, args);
  }
}
