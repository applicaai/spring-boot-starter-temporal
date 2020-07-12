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

package ai.applica.spring.boot.starter.temporal.samples;

import static org.junit.Assert.assertEquals;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloQuery.GreetingWorkflow;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloQuery.GreetingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/** Unit test for {@link HelloQuery}. Doesn't use an external Temporal service. */
@RunWith(SpringRunner.class)
@SpringBootTest()
public class HelloQueryTest {

  /** Prints a history of the workflow under test in case of a test failure. */
  @Rule
  public TestWatcher watchman =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          if (testEnv != null) {
            System.err.println(testEnv.getDiagnostics());
            testEnv.close();
          }
        }
      };

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  GreetingWorkflow workflow;

  @Autowired WorkflowFactory fact;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = fact.makeWorker(testEnv, GreetingWorkflowImpl.class);

    // Get a workflow stub using the same task queue the worker uses.
    workflow =
        fact.makeClient(
            GreetingWorkflow.class, GreetingWorkflowImpl.class, testEnv.getWorkflowClient());
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test(timeout = 5000)
  public void testQuery() {
    testEnv.start();
    // Start workflow asynchronously to not use another thread to query.
    WorkflowClient.start(workflow::createGreeting, "World");

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub.
    assertEquals("Hello World!", workflow.queryGreeting());

    // Unit tests should call TestWorkflowEnvironment.sleep.
    // It allows skipping the time if workflow is just waiting on a timer
    // and executing tests of long running workflows very fast.
    // Note that this unit test executes under a second and not
    // over 3 as it would if Thread.sleep(3000) was called.
    testEnv.sleep(Duration.ofSeconds(3));

    assertEquals("Bye World!", workflow.queryGreeting());
  }
}
