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

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import ai.applica.spring.boot.starter.temporal.samples.apps.CustomActivityAnnotationException;
import ai.applica.spring.boot.starter.temporal.samples.apps.CustomActivityConfigurationException;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation.GreetingActivities;
import io.temporal.api.enums.v1.RetryState;
import io.temporal.client.WorkflowFailedException;
import io.temporal.failure.ActivityFailure;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Unit test for {@link
 * ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation}. Doesn't use an
 * external Temporal service.
 */
@RunWith(SpringRunner.class)
@SpringBootTest
@TemporalTest
public class HelloActivityAnnotationTest {

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

  @Autowired WorkflowFactory fact;
  @Autowired GreetingActivities greetingActivity;
  @Autowired HelloActivityAnnotation.TimeoutActivity timeoutActivity;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testActivityImpl() {
    // given
    HelloActivityAnnotation.GreetingWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.GreetingWorkflow.class,
            HelloActivityAnnotation.GreetingWorkflowImpl.class,
            greetingActivity);
    testEnv.start();

    // when
    WorkflowFailedException exception =
        assertThrows(WorkflowFailedException.class, () -> workflow.getGreeting("World"));

    // then
    assertEquals(RetryState.RETRY_STATE_UNSPECIFIED, exception.getRetryState());
    ActivityFailure cause = (ActivityFailure) exception.getCause();
    // temporal throws the failure cause from the last HistoryEvent in  execution
    assertEquals(RetryState.RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED, cause.getRetryState());
  }

  @Test(timeout = 10000)
  public void shouldReachMaximumNumberOfAttempts() {
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

    // when
    WorkflowFailedException exception =
        assertThrows(WorkflowFailedException.class, () -> workflow.getGreeting("World"));

    // then
    assertEquals(RetryState.RETRY_STATE_UNSPECIFIED, exception.getRetryState());

    ActivityFailure cause = (ActivityFailure) exception.getCause();
    assertEquals(RetryState.RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED, cause.getRetryState());
    // temporal throws the failure cause from the last HistoryEvent in execution

    // and
    verify(activities, times(5)).composeGreeting(anyString(), anyString());

    // and check if diagnostics contains real cause of workflow failure (very tricky method, but in
    // this moment i can't see any alternatives)
    assertNotEquals(-1, testEnv.getDiagnostics().indexOf("RETRY_STATE_MAXIMUM_ATTEMPTS_REACHED"));
  }

  @Ignore(
      "Due to bug in io.temporal.internal.testservice.StateMachines.ensureDefaultFieldsForActivityRetryPolicy()")
  @Test(timeout = 10000)
  public void shouldFinishBecauseOfNonRetryableExceptionDefinedInAnnotation() {
    // given
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting("Hello", "World"))
        .thenThrow(
            new IllegalStateException("not yet 1"),
            new IllegalStateException("not yet 2"),
            new CustomActivityAnnotationException("finish now!"))
        .thenReturn("Should never reach here!");
    HelloActivityAnnotation.GreetingWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.GreetingWorkflow.class,
            HelloActivityAnnotation.GreetingWorkflowImpl.class,
            activities);
    testEnv.start();

    // when
    WorkflowFailedException exception =
        assertThrows(WorkflowFailedException.class, () -> workflow.getGreeting("World"));

    // then
    assertEquals(RetryState.RETRY_STATE_UNSPECIFIED, exception.getRetryState());

    ActivityFailure cause = (ActivityFailure) exception.getCause();
    assertEquals(RetryState.RETRY_STATE_TIMEOUT, cause.getRetryState());

    // and
    verify(activities, times(3)).composeGreeting(anyString(), anyString());

    // and check if diagnostics contains real cause of workflow failure (very tricky method, but in
    // this moment i can't see any alternatives)
    assertNotEquals(-1, testEnv.getDiagnostics().indexOf("RETRY_STATE_NON_RETRYABLE_FAILURE"));
  }

  @Ignore(
      "Due to bug in io.temporal.internal.testservice.StateMachines.ensureDefaultFieldsForActivityRetryPolicy()")
  @Test(timeout = 10000)
  public void shouldFinishBecauseOfNonRetryableExceptionDefinedInConfiguration() {
    // given
    GreetingActivities activities = mock(GreetingActivities.class);
    when(activities.composeGreeting("Hello", "World"))
        .thenThrow(
            new IllegalStateException("not yet 1"),
            new IllegalStateException("not yet 2"),
            new CustomActivityConfigurationException("finish now!"))
        .thenReturn("Should never reach here!");
    HelloActivityAnnotation.GreetingWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.GreetingWorkflow.class,
            HelloActivityAnnotation.GreetingWorkflowImpl.class,
            activities);
    testEnv.start();

    // when
    WorkflowFailedException exception =
        assertThrows(WorkflowFailedException.class, () -> workflow.getGreeting("World"));

    // then
    assertEquals(RetryState.RETRY_STATE_UNSPECIFIED, exception.getRetryState());

    ActivityFailure cause = (ActivityFailure) exception.getCause();
    assertEquals(RetryState.RETRY_STATE_TIMEOUT, cause.getRetryState());

    // and
    verify(activities, times(3)).composeGreeting(anyString(), anyString());

    // and check if diagnostics contains real cause of workflow failure (very tricky method, but in
    // this moment i can't see any alternatives)
    assertNotEquals(-1, testEnv.getDiagnostics().indexOf("RETRY_STATE_NON_RETRYABLE_FAILURE"));
  }

  @Test(timeout = 10000)
  public void shouldUseDeprecatedValue() {
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

  @Test(timeout = 10000)
  public void shouldUseNewValue() {
    HelloActivityAnnotation.TimeoutWorkflow workflow =
        createWorkflow(
            HelloActivityAnnotation.TimeoutWorkflow.class,
            HelloActivityAnnotation.ComplexTimeoutWorkflowImpl.class,
            timeoutActivity);
    testEnv.start();

    Map<String, Duration> timeouts = workflow.getTimeouts();
    Duration startToCloseTimeout = timeouts.get(TestConstants.START_TO_CLOSE_TIMEOUT_KEY);
    Duration scheduleToCloseTimeout = timeouts.get(TestConstants.SCHEDULE_TO_CLOSE_TIMEOUT_KEY);

    assertEquals("PT2S", startToCloseTimeout.toString());
    assertEquals("PT4S", scheduleToCloseTimeout.toString());
  }

  private <T> T createWorkflow(
      Class<T> workflowInterface, Class<? extends T> workflowClass, Object activity) {
    Worker worker = fact.makeWorker(testEnv, workflowClass);
    worker.registerActivitiesImplementations(activity);
    return fact.makeStub(workflowInterface, workflowClass, testEnv.getWorkflowClient());
  }
}
