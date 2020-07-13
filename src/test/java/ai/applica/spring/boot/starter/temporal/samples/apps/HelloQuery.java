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
import io.temporal.client.WorkflowClient;
import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

/** Demonstrates query capability. Requires a local instance of Temporal server to be running. */
public class HelloQuery {

  static final String TASK_QUEUE = "HelloQuery";

  @WorkflowInterface
  public interface GreetingWorkflow {

    @WorkflowMethod
    void createGreeting(String name);

    /** Returns greeting as a query value. */
    @QueryMethod
    String queryGreeting();
  }

  /** GreetingWorkflow implementation that updates greeting after sleeping for 5 seconds. */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    private String greeting;

    @Override
    public void createGreeting(String name) {
      greeting = "Hello " + name + "!";
      // Workflow code always uses WorkflowThread.sleep
      // and Workflow.currentTimeMillis instead of standard Java ones.
      Workflow.sleep(Duration.ofSeconds(2));
      greeting = "Bye " + name + "!";
    }

    @Override
    public String queryGreeting() {
      return greeting;
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

      // Start workflow asynchronously to not use another thread to query.
      WorkflowClient.start(workflow::createGreeting, "World");
      // After start for getGreeting returns, the workflow is guaranteed to be started.
      // So we can send a signal to it using workflow stub.

      System.out.println("\n\n");
      System.out.println(workflow.queryGreeting()); // Should print Hello...
      // Note that inside a workflow only WorkflowThread.sleep is allowed. Outside
      // WorkflowThread.sleep is not allowed.
      Thread.sleep(2500);
      System.out.println(workflow.queryGreeting()); // Should print Bye ...
      System.out.println("\n\n");
      System.exit(0);
    }
  }

  public static void main(String[] args) {
    // Wait for workflow completion.
    SpringApplication.run(GreetingWorkflowRequester.class, args);
  }
}
