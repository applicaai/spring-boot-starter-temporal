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
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
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
    @ActivityStub(duration = 10)
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
  // FIXME
  // @EnableTemporal
  // @SpringBootApplication
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
