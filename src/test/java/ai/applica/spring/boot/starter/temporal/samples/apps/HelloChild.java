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
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

/**
 * Demonstrates a child workflow. Requires a local instance of the Temporal server to be running.
 */
public class HelloChild {

  static final String TASK_QUEUE = "HelloChild";

  /** The parent workflow interface. */
  @WorkflowInterface
  public interface GreetingWorkflow {
    /** @return greeting string */
    @WorkflowMethod
    String getGreeting(String name);
  }

  /** The child workflow interface. */
  @WorkflowInterface
  public interface GreetingChild {
    @WorkflowMethod
    String composeGreeting(String greeting, String name);
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#printIt. */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    @Override
    public String getGreeting(String name) {
      // Workflows are stateful. So a new stub must be created for each new child.
      GreetingChild child = Workflow.newChildWorkflowStub(GreetingChild.class);

      // This is a non blocking call that returns immediately.
      // Use child.composeGreeting("Hello", name) to call synchronously.
      // Promise<String> greeting = Async.function(child::composeGreeting, "Hello", name);
      // Do something else here.
      // return greeting.get(); // blocks waiting for the child to complete
      return child.composeGreeting("Hello", name);
    }
  }

  /**
   * The child workflow implementation. A workflow implementation must always be public for the
   * Temporal library to be able to create instances.
   */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class GreetingChildImpl implements GreetingChild {
    @Override
    public String composeGreeting(String greeting, String name) {
      return greeting + " " + name + "!";
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
