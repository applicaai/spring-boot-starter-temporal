/*
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
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.EnableTemporal;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityExecutionContext;
import io.temporal.activity.ActivityInterface;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

/**
 * Demonstrates an asynchronous activity implementation. Requires a local instance of Temporal
 * server to be running.
 */
public class HelloAsyncActivityCompletion {

  static final String TASK_QUEUE = "HelloAsyncActivityCompletion";

  @WorkflowInterface
  public interface GreetingWorkflow {
    /** @return greeting string */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** Activity interface is just a POJI. * */
  @ActivityInterface
  public interface GreetingActivities {
    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#printIt. */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    /**
     * Activity stub implements activity interface and proxies calls to it to Temporal activity
     * invocations. Because activities are reentrant, only a single stub can be used for multiple
     * activity invocations.
     */
    @ActivityStub(durationInSeconds = 10)
    private GreetingActivities activities;

    @Override
    public String getGreeting(String name) {
      // This is a blocking call that returns only after the activity has completed.
      return activities.composeGreeting("Hello", name);
    }
  }

  @Component
  public static class GreetingActivitiesImpl implements GreetingActivities {
    @Autowired private ActivityCompletionClient completionClient;

    /** For _test_ perpose only to put completition client of TestEnvironment */
    public void setCompletionClient(ActivityCompletionClient completionClient) {
      this.completionClient = completionClient;
    }
    /**
     * Demonstrates how to implement an activity asynchronously. When {@link
     * io.temporal.activity.ActivityExecutionContext#doNotCompleteOnReturn()} is called the activity
     * implementation function returning doesn't complete the activity.
     */
    @Override
    public String composeGreeting(String greeting, String name) {
      // TaskToken is a correlation token used to match an activity task with its completion
      ActivityExecutionContext context = Activity.getExecutionContext();
      byte[] taskToken = context.getTaskToken();
      // In real life this request can be executed anywhere. By a separate service for
      // example.
      ForkJoinPool.commonPool().execute(() -> composeGreetingAsync(taskToken, greeting, name));
      context.doNotCompleteOnReturn();
      // When doNotCompleteOnReturn() is invoked the return value is ignored.
      return "ignored";
    }

    private void composeGreetingAsync(byte[] taskToken, String greeting, String name) {
      String result = greeting + " " + name + "!";
      // To complete an activity from a different thread or process use ActivityCompletionClient.
      // In real applications the client is initialized by a process that performs the completion.
      completionClient.complete(taskToken, result);
    }
  }

  @EnableTemporal
  @SpringBootApplication
  public static class GreetingWorkflowRequester implements CommandLineRunner {

    @Autowired private WorkflowFactory fact;

    public void run(String... input) throws Exception {
      // Start a workflow execution. Usually this is done from another program or bean.
      // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
      GreetingWorkflow workflow =
          fact.makeClient(GreetingWorkflow.class, GreetingWorkflowImpl.class);

      // Execute a workflow asynchronously returning a future
      // that can be used to wait for the workflow completion.
      CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");
      System.out.println("\n\n");
      System.out.println(greeting.get());
      System.out.println("\n\n");
      System.exit(0);
    }
  }

  public static void main(String[] args) {
    // Wait for workflow completion.
    SpringApplication.run(GreetingWorkflowRequester.class, args);
  }
}
