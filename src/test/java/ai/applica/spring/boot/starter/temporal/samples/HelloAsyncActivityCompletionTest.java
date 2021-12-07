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
import ai.applica.spring.boot.starter.temporal.extensions.TemporalTestWatcher;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloAsyncActivityCompletion;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloAsyncActivityCompletion.GreetingActivitiesImpl;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloAsyncActivityCompletion.GreetingWorkflow;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloAsyncActivityCompletion.GreetingWorkflowImpl;
import io.temporal.client.ActivityCompletionClient;
import io.temporal.client.WorkflowClient;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

/** Unit test for {@link HelloAsyncActivityCompletion}. Doesn't use an external Temporal service. */
class HelloAsyncActivityCompletionTest extends BaseTest {

  /** Prints a history of the workflow under test in case of a test failure. */
  @RegisterExtension TemporalTestWatcher temporalTestWatcher = new TemporalTestWatcher();

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  private GreetingWorkflow workflow;
  private WorkflowClient client;

  private @Autowired WorkflowFactory fact;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    temporalTestWatcher.setEnvironment(testEnv);
    worker = fact.makeWorker(testEnv, GreetingWorkflowImpl.class);
    client = testEnv.getWorkflowClient();

    workflow = fact.makeStub(GreetingWorkflow.class, GreetingWorkflowImpl.class, client);
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  @Timeout(2)
  void testActivityImpl() throws ExecutionException, InterruptedException {
    ActivityCompletionClient completionClient = client.newActivityCompletionClient();
    GreetingActivitiesImpl greetingActivitiesImpl = new GreetingActivitiesImpl();
    greetingActivitiesImpl.setCompletionClient(completionClient);
    worker.registerActivitiesImplementations(greetingActivitiesImpl);
    testEnv.start();

    // Execute a workflow asynchronously.
    CompletableFuture<String> greeting = WorkflowClient.execute(workflow::getGreeting, "World");
    // Wait for workflow completion.
    assertEquals("Hello World!", greeting.get());
  }
}
