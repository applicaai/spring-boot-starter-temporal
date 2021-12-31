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

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivitySepareteWorker;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivitySepareteWorker.GreetingActivities;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivitySepareteWorker.GreetingSeparateWorkflow;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivitySepareteWorker.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Unit test for {@link HelloActivitySepareteWorker}. Doesn't use an external Temporal service. */
class HelloActivitySeparateWorkerTest extends BaseTest {

  static final String ACTIVITY_TASK_QUEUE = "HelloActivitySepareteWorkerActivity";

  private TestWorkflowEnvironment testEnv;
  GreetingSeparateWorkflow workflow;

  @Autowired WorkflowFactory fact;
  @Autowired GreetingActivities greatActivity;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    fact.makeWorker(testEnv, GreetingWorkflowImpl.class);

    // Get a workflow stub using the same task queue the worker uses.
    workflow =
        fact.makeStub(
            GreetingSeparateWorkflow.class,
            GreetingWorkflowImpl.class,
            testEnv.getWorkflowClient());
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  void testActivityImpl() {
    Worker activityWorker = testEnv.newWorker(ACTIVITY_TASK_QUEUE);
    activityWorker.registerActivitiesImplementations(greatActivity);
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
  }
}
