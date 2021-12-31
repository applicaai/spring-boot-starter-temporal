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

import static ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityOptionsModifier.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import ai.applica.spring.boot.starter.temporal.extensions.TemporalTestWatcher;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityOptionsModifier;
import io.temporal.api.enums.v1.RetryState;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ActivityFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/** Unit test for {@link HelloActivityOptionsModifier}. Doesn't use an external Temporal service. */
@SpringBootTest
@TemporalTest
class HelloActivityOptionsModifierTest {

  private TestWorkflowEnvironment testEnv;
  private Worker worker;
  GreetingWorkflow workflow;

  @RegisterExtension TemporalTestWatcher temporalTestWatcher = new TemporalTestWatcher();
  @Autowired WorkflowFactory fact;
  @Autowired GreetingActivities greatActivity;

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    temporalTestWatcher.setEnvironment(testEnv);
    worker = fact.makeWorker(testEnv, GreetingWorkflowImpl.class);

    // Get a workflow stub using the same task queue the worker uses.
    workflow =
        fact.makeStub(
            GreetingWorkflow.class, GreetingWorkflowImpl.class, testEnv.getWorkflowClient());
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  @Timeout(1)
  void testHappyPath() {
    worker.registerActivitiesImplementations(greatActivity);
    testEnv.start();

    // Execute a workflow waiting for it to complete.
    String greeting = workflow.getGreeting("World");

    assertThat(greeting).isEqualTo("Hello World!");
  }

  @Test
  @Timeout(1)
  void testRetryWithFailure() {
    // given
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting("Hello", "World"))
        .thenThrow(new IllegalStateException("not yet1"), new IllegalStateException("not yet2"))
        .thenReturn("Hello World!");
    worker.registerActivitiesImplementations(activities);
    testEnv.start();

    // when
    WorkflowFailedException exception =
        assertThrows(WorkflowFailedException.class, () -> workflow.getGreeting("World"));

    // then
    Throwable cause = exception.getCause();
    assertEquals(cause.getClass(), ActivityFailure.class);
    assertEquals(
        ((ActivityFailure) cause).getRetryState(), RetryState.RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED);
  }
}
