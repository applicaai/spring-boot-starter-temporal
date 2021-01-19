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

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloProperties;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloProperties.*;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/** Unit test for temporal properties usage */
@RunWith(SpringRunner.class)
@SpringBootTest
@TemporalTest
public class HelloPropertiesTest {

  private TestWorkflowEnvironment testEnv;

  @Autowired WorkflowFactory fact;
  @Autowired PropertiesActivity propertiesActivity;
  @Autowired TimeoutPropertiesActivity timeoutPropertiesActivity;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void shouldAssignProperties() {
    PropertiesWorkflow workflow =
        createWorkflow(PropertiesWorkflow.class, PropertiesWorkflowImpl.class, propertiesActivity);

    testEnv.start();

    Map<String, Duration> workflowTimeouts = workflow.getTimeouts();
    assertThat(workflowTimeouts.get(HelloProperties.SCHEDULE_TO_CLOSE_TIMEOUT_KEY))
        .isEqualTo(Duration.ofSeconds(1000)); // workflow timeout
    assertThat(workflowTimeouts.get(HelloProperties.START_TO_CLOSE_TIMEOUT_KEY))
        .isEqualTo(Duration.ofSeconds(15));
  }

  @Test
  public void shouldSetStartToCloseValueToScheduleToCloseTimeout() {
    PropertiesDotWorkflow workflow =
        createWorkflow(
            PropertiesDotWorkflow.class, PropertiesDotWorkflowImpl.class, propertiesActivity);

    testEnv.start();

    Duration threeMinutes = Duration.ofMinutes(3);
    Map<String, Duration> workflowStartToCloseTimeouts = workflow.getTimeouts();
    assertThat(workflowStartToCloseTimeouts.get(HelloProperties.SCHEDULE_TO_CLOSE_TIMEOUT_KEY))
        .isEqualTo(threeMinutes);
    assertThat(workflowStartToCloseTimeouts.get(HelloProperties.START_TO_CLOSE_TIMEOUT_KEY))
        .isEqualTo(threeMinutes);
  }

  @Test
  public void shouldAssignWorkflowTimeouts() {
    PropertiesTimeoutWorkflow workflow =
        createWorkflow(
            PropertiesTimeoutWorkflow.class,
            PropertiesTimeoutWorkflowImpl.class,
            timeoutPropertiesActivity);
    testEnv.start();

    Map<String, Duration> workflowStartToCloseTimeouts = workflow.getTimeouts();

    int workflowTimeout = 1000;
    assertThat(workflowStartToCloseTimeouts.get(HelloProperties.SCHEDULE_TO_CLOSE_TIMEOUT_KEY))
        .isEqualTo(Duration.ofSeconds(workflowTimeout));
    assertThat(workflowStartToCloseTimeouts.get(HelloProperties.START_TO_CLOSE_TIMEOUT_KEY))
        .isEqualTo(Duration.ofSeconds(workflowTimeout));
  }

  private <T> T createWorkflow(
      Class<T> workflowInterface, Class<? extends T> workflowClass, Object activity) {
    Worker worker = fact.makeWorker(testEnv, workflowClass);
    worker.registerActivitiesImplementations(activity);
    return fact.makeStub(workflowInterface, workflowClass, testEnv.getWorkflowClient());
  }
}
