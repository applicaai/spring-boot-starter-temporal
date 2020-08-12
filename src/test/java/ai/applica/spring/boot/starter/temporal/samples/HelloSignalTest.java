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

import static org.junit.Assert.assertEquals;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloSignal.GreetingWorkflow;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloSignal.GreetingWorkflowImpl;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.enums.v1.WorkflowIdReusePolicy;
import io.temporal.testing.TestWorkflowEnvironment;
import java.time.Duration;
import java.util.List;
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

/** Unit test for {@link HelloSignal}. Doesn't use an external Temporal service. */
@RunWith(SpringRunner.class)
@SpringBootTest
@TemporalTest
public class HelloSignalTest {

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
  private WorkflowClient client;

  @Autowired WorkflowFactory fact;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    fact.makeWorker(testEnv, GreetingWorkflowImpl.class);
    testEnv.start();
    client = testEnv.getWorkflowClient();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test(timeout = 5000)
  public void testSignal() {
    WorkflowOptions.Builder workflowOptions =
        fact.defaultOptionsBuilder(GreetingWorkflowImpl.class);
    // Get a workflow stub using the same task queue the worker uses.
    workflowOptions.setWorkflowIdReusePolicy(
        WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE);

    GreetingWorkflow workflow = fact.makeStub(GreetingWorkflow.class, workflowOptions, client);

    // Start workflow asynchronously to not use another thread to signal.
    WorkflowClient.start(workflow::getGreetings);

    // After start for getGreeting returns, the workflow is guaranteed to be started.
    // So we can send a signal to it using workflow stub immediately.
    // But just to demonstrate the unit testing of a long running workflow adding a long sleep here.
    testEnv.sleep(Duration.ofDays(1));
    // This workflow keeps receiving signals until exit is called
    workflow.waitForName("World");
    workflow.waitForName("Universe");
    workflow.exit();
    // Calling synchronous getGreeting after workflow has started reconnects to the existing
    // workflow and
    // blocks until result is available. Note that this behavior assumes that WorkflowOptions are
    // not configured
    // with WorkflowIdReusePolicy.AllowDuplicate. In that case the call would fail with
    // WorkflowExecutionAlreadyStartedException.
    List<String> greetings = workflow.getGreetings();
    assertEquals(2, greetings.size());
    assertEquals("Hello World!", greetings.get(0));
    assertEquals("Hello Universe!", greetings.get(1));
  }
}
