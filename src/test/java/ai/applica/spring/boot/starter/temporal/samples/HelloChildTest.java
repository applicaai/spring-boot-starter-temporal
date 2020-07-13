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
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloChild.GreetingChild;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloChild.GreetingChildImpl;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloChild.GreetingWorkflow;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloChild.GreetingWorkflowImpl;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.atomic.AtomicReference;
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

/** Unit test for {@link HelloChild}. Doesn't use an external Temporal service. */
@RunWith(SpringRunner.class)
@SpringBootTest
public class HelloChildTest {

  /** Prints workflow histories under test in case of a test failure. */
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
  GreetingWorkflow workflow;

  @Autowired WorkflowFactory fact;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();

    // Get a workflow stub using the same task queue the worker uses.
    workflow =
        fact.makeStub(
            GreetingWorkflow.class, GreetingWorkflowImpl.class, testEnv.getWorkflowClient());
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testChild() {
    fact.makeWorker(testEnv, GreetingWorkflowImpl.class, GreetingChildImpl.class);

    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
  }

  @Test
  public void testMockedChild() {
    Worker worker = fact.makeWorker(testEnv, GreetingWorkflowImpl.class);
    // As new mock is created on each decision the only last one is useful to verify calls.
    AtomicReference<GreetingChild> lastChildMock = new AtomicReference<>();
    // Factory is called to create a new workflow object on each decision.
    worker.addWorkflowImplementationFactory(
        GreetingChild.class,
        () -> {
          GreetingChild child = mock(GreetingChild.class);
          when(child.composeGreeting("Hello", "World")).thenReturn("Hello World!");
          lastChildMock.set(child);
          return child;
        });
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");
    assertEquals("Hello World!", greeting);
    GreetingChild mock = lastChildMock.get();
    verify(mock).composeGreeting(eq("Hello"), eq("World"));
  }
}
