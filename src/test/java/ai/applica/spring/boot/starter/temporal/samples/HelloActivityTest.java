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

package ai.applica.spring.boot.starter.temporal.samples;

import static ai.applica.spring.boot.starter.temporal.samples.HelloActivity.TASK_QUEUE;
import static org.junit.Assert.assertEquals;

import ai.applica.spring.boot.starter.temporal.samples.HelloActivity.GreetingActivitiesImpl;
import ai.applica.spring.boot.starter.temporal.samples.HelloActivity.GreetingWorkflow;
import ai.applica.spring.boot.starter.temporal.samples.HelloActivity.GreetingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit test for {@link HelloActivity}. Doesn't use an external Temporal service. */
public class HelloActivityTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private WorkflowClient client;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = testEnv.newWorker(TASK_QUEUE);
    worker.registerWorkflowImplementationTypes(GreetingWorkflowImpl.class);

    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testActivityImpl() {
    worker.registerActivitiesImplementations(new GreetingActivitiesImpl());
    testEnv.start();

    // Get a workflow stub using the same task queue the worker uses.
    GreetingWorkflow workflow =
        client.newWorkflowStub(
            GreetingWorkflow.class, WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
  }

  @Test
  public void testMockedActivity() {
    // FIXME
    // GreetingActivities activities = mock(GreetingActivities.class);
    // when(activities.composeGreeting("Hello", "World")).thenReturn("Hello World!");
    // worker.registerActivitiesImplementations(activities);
    // testEnv.start();

    // // Get a workflow stub using the same task queue the worker uses.
    // GreetingWorkflow workflow =
    //     client.newWorkflowStub(
    //         GreetingWorkflow.class,
    // WorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build());
    // // Execute a workflow waiting for it to complete.
    // String greeting = workflow.getGreeting("World");
    // assertEquals("Hello World!", greeting);
  }
}
