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
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.serviceclient.WorkflowServiceStubs;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Hello World Temporal workflow that executes a single activity. Requires a local instance the
 * Temporal service to be running.
 */
public class HelloActivity {

  static final String TASK_QUEUE = "HelloActivity";

  /** Workflow interface has to have at least one method annotated with @WorkflowMethod. */
  @WorkflowInterface
  public interface GreetingWorkflow {
    /** @return greeting string */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** Activity interface is just a POJI. */
  @ActivityInterface
  public interface GreetingActivities {
    @ActivityMethod
    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#composeGreeting. */
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

  @Service
  static class SimpleExclamationBean {
    public String getExclamation() {
      return "!";
    }
  }

  @Service
  static class GreetingActivitiesImpl implements GreetingActivities {

    @Autowired private SimpleExclamationBean simpleExclamationBean;

    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + simpleExclamationBean.getExclamation();
    }
  }

  public static void main(String[] args) {
    // gRPC stubs wrapper that talks to the local docker instance of temporal service.
    WorkflowServiceStubs service = WorkflowServiceStubs.newInstance();
    // client that can be used to start and signal workflows
    WorkflowClient client = WorkflowClient.newInstance(service);

    // worker factory that can be used to create workers for specific task queues
    WorkerFactory factory = WorkerFactory.newInstance(client);
    // Worker that listens on a task queue and hosts both workflow and activity implementations.
    Worker worker = factory.newWorker(TASK_QUEUE);
    // Workflows are stateful. So you need a type to create instances.
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);
    // Activities are stateless and thread safe. So a shared instance is used.
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    // Start listening to the workflow and activity task queues.
    factory.start();

    // Start a workflow execution. Usually this is done from another program.
    // Uses task queue from the GreetingWorkflow @WorkflowMethod annotation.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete. See {@link
    // io.temporal.samples.hello.HelloSignal}
    // for an example of starting workflow without waiting synchronously for its result.
    String greeting = workflow.getGreeting("World");
    System.out.println(greeting);
    System.exit(0);
  }
}
