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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.extensions.TemporalTestWatcher;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation.GreetingActivities;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import io.temporal.api.enums.v1.RetryState;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ActivityFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Unit test for {@link
 * ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation}. Doesn't use an
 * external Temporal service.
 */
@SpringBootTest
@TemporalTest
class HelloActivityAnnotationTest {

  @Autowired WorkflowFactory fact;
  @Autowired GreetingActivities greetingActivity;
  @Autowired HelloActivityAnnotation.TimeoutActivity timeoutActivity;
  private TestWorkflowEnvironment testEnv;
  @RegisterExtension TemporalTestWatcher temporalTestWatcher = new TemporalTestWatcher();

  @BeforeEach
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    temporalTestWatcher.setEnvironment(testEnv);
  }

  @AfterEach
  public void tearDown() {
    testEnv.close();
  }

  @Test
  void testActivityImpl() {
    // given
    HelloActivityAnnotation.GreetingWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.GreetingWorkflow.class,
            HelloActivityAnnotation.GreetingWorkflowImpl.class,
            greetingActivity);
    testEnv.start();

    // then
    assertThatThrownBy(() -> workflow.getGreeting("World"))
        .isInstanceOfSatisfying(
            WorkflowFailedException.class,
            exception ->
                assertThat(exception.getRetryState()).isEqualTo(RetryState.RETRY_STATE_UNSPECIFIED))
        .getCause()
        .isInstanceOfSatisfying(
            ActivityFailure.class,
            cause ->
                assertThat(cause.getRetryState())
                    .isEqualTo(RetryState.RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED));
  }

  @Test
  @Timeout(10)
  void shouldReachMaximumNumberOfAttempts() {
    // given
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting("Hello", "World"))
        .thenThrow(
            new IllegalStateException("not yet 1"),
            new IllegalStateException("not yet 2"),
            new IllegalStateException("not yet 3"),
            new IllegalStateException("not yet 4"),
            new IllegalStateException("not yet 5"))
        .thenReturn("Should never reach here!");
    HelloActivityAnnotation.GreetingWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.GreetingWorkflow.class,
            HelloActivityAnnotation.GreetingWorkflowImpl.class,
            activities);
    testEnv.start();

    // then

    // temporal throws the failure cause from the last HistoryEvent in execution
    assertThatThrownBy(() -> workflow.getGreeting("World"))
        .isInstanceOfSatisfying(
            WorkflowFailedException.class,
            exception ->
                assertThat(exception.getRetryState()).isEqualTo(RetryState.RETRY_STATE_UNSPECIFIED))
        .getCause()
        .isInstanceOfSatisfying(
            ActivityFailure.class,
            cause ->
                assertThat(cause.getRetryState())
                    .isEqualTo(RetryState.RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED));

    // and
    verify(activities, times(5)).composeGreeting(anyString(), anyString());

    // and check if diagnostics contains real cause of workflow failure
    assertThat(testEnv.getDiagnostics()).contains("RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED");
  }

  @Test
  @Timeout(10)
  void shouldUseDeprecatedValue() {
    HelloActivityAnnotation.TimeoutWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.TimeoutWorkflow.class,
            HelloActivityAnnotation.DeprecatedTimeoutWorkflowImpl.class,
            timeoutActivity);
    testEnv.start();

    Map<String, Duration> timeouts = workflow.getTimeouts();
    Duration scheduleToCloseTimeout = timeouts.get(TestConstants.SCHEDULE_TO_CLOSE_TIMEOUT_KEY);
    Duration startToCloseTimeout = timeouts.get(TestConstants.START_TO_CLOSE_TIMEOUT_KEY);
    assertEquals("PT3M", scheduleToCloseTimeout.toString());
    assertEquals("PT3M", startToCloseTimeout.toString());
  }

  @Test
  @Timeout(10)
  void shouldUseNewValue() {
    HelloActivityAnnotation.TimeoutWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.TimeoutWorkflow.class,
            HelloActivityAnnotation.ComplexTimeoutWorkflowImpl.class,
            timeoutActivity);
    testEnv.start();

    Map<String, Duration> timeouts = workflow.getTimeouts();
    Duration startToCloseTimeout = timeouts.get(TestConstants.START_TO_CLOSE_TIMEOUT_KEY);
    Duration scheduleToCloseTimeout = timeouts.get(TestConstants.SCHEDULE_TO_CLOSE_TIMEOUT_KEY);
    Duration heartbeatTimeout = timeouts.get(TestConstants.HEARTBEAT_TIMEOUT_KEY);

    assertEquals("PT8S", startToCloseTimeout.toString());
    assertEquals("PT30S", scheduleToCloseTimeout.toString());
    assertEquals("PT2S", heartbeatTimeout.toString());
  }

  private <T> T createWorkflow(
      Class<T> workflowInterface, Class<? extends T> workflowClass, Object activity) {
    Worker worker = fact.makeWorker(testEnv, workflowClass);
    worker.registerActivitiesImplementations(activity);
    return fact.makeStub(workflowInterface, workflowClass, testEnv.getWorkflowClient());
  }
}
