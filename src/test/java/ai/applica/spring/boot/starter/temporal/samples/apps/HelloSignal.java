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
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions.Builder;
import io.temporal.workflow.SignalMethod;
import io.temporal.workflow.Workflow;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.stereotype.Component;

/**
 * Demonstrates asynchronous signalling of a workflow. Requires a local instance of Temporal server
 * to be running.
 */
public class HelloSignal {

  static final String TASK_QUEUE = "HelloSignal";

  /** Workflow interface must have a method annotated with @WorkflowMethod. */
  @WorkflowInterface
  public interface GreetingWorkflow {
    /**
     * @return list of greeting strings that were received through the waitForNameMethod. This
     *     method will block until the number of greetings specified are received.
     */
    @WorkflowMethod
    List<String> getGreetings();

    /** Receives name through an external signal. */
    @SignalMethod
    void waitForName(String name);

    @SignalMethod
    void exit();
  }

  /** GreetingWorkflow implementation that returns a greeting. */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class GreetingWorkflowImpl implements GreetingWorkflow {

    List<String> messageQueue = new ArrayList<>(10);
    boolean exit = false;

    @Override
    public List<String> getGreetings() {
      List<String> receivedMessages = new ArrayList<>(10);

      while (true) {
        Workflow.await(() -> !messageQueue.isEmpty() || exit);
        if (messageQueue.isEmpty() && exit) {
          return receivedMessages;
        }
        String message = messageQueue.remove(0);
        receivedMessages.add(message);
      }
    }

    @Override
    public void waitForName(String name) {
      messageQueue.add("Hello " + name + "!");
    }

    @Override
    public void exit() {
      exit = true;
    }
  }

  // FIXME
  // @EnableTemporal
  // @SpringBootApplication
  public static class GreetingWorkflowRequester implements CommandLineRunner {

    @Autowired private WorkflowFactory fact;
    @Autowired private WorkflowClient workflowClient;

    public void run(String... input) throws Exception {
      // In a real application use a business ID like customer ID or order ID
      String workflowId = "Busines_" + new Random().nextInt(5000);

      // Start a workflow execution. Usually this is done from another program.
      // Get a workflow stub using the same task queue the worker uses.
      // The newly started workflow is going to have the workflowId generated above.
      Builder optionsBuilder = fact.defaultOptionsBuilder(GreetingWorkflowImpl.class);
      optionsBuilder.setWorkflowId(workflowId);
      GreetingWorkflow workflow = fact.makeStub(GreetingWorkflow.class, optionsBuilder);

      // Start workflow asynchronously to not use another thread to signal.
      WorkflowClient.start(workflow::getGreetings);
      // After start for getGreeting returns, the workflow is guaranteed to be started.
      // So we can send a signal to it using the workflow stub.
      // This workflow keeps receiving signals until exit is called
      workflow.waitForName("World"); // sends waitForName signal

      // Create a new stub using the workflowId.
      // This is to demonstrate that to send a signal only the workflowId is required.
      GreetingWorkflow workflowById =
          workflowClient.newWorkflowStub(GreetingWorkflow.class, workflowId);
      workflowById.waitForName("Universe"); // sends waitForName signal
      workflowById.exit(); // sends exit signal
      // Calling synchronous getGreeting after workflow has started reconnects to the existing
      // workflow and blocks until a result is available. Note that this behavior assumes that
      // WorkflowOptions are not configured with WorkflowIdReusePolicy.AllowDuplicate. In that case
      // the call would fail with WorkflowExecutionAlreadyStartedException.
      List<String> greetings = workflowById.getGreetings();
      System.out.println("\n\n");
      System.out.println(greetings);
      System.out.println("\n\n");
      System.exit(0);
    }
  }

  public static void main(String[] args) {
    // Wait for workflow completion.
    SpringApplication.run(GreetingWorkflowRequester.class, args);
  }
}
