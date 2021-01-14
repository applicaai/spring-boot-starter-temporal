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
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloProperties.*;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import java.time.Duration;
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
  private Worker worker;
  private Worker dotWorker;
  PropertiesWorkflow workflow;
  PropertiesDotWorkflow dotWorkflow;

  @Autowired WorkflowFactory fact;
  @Autowired PropertiesActivity timeoutActivity;

  @Before
  public void setUp() {
    testEnv = TestWorkflowEnvironment.newInstance();
    worker = fact.makeWorker(testEnv, PropertiesWorkflowImpl.class);
    dotWorker = fact.makeWorker(testEnv, PropertiesDotWorkflowImpl.class);

    // Get a workflow stub using the same task queue the worker uses.
    workflow =
        fact.makeStub(
            PropertiesWorkflow.class, PropertiesWorkflowImpl.class, testEnv.getWorkflowClient());
    dotWorkflow =
        fact.makeStub(
            PropertiesDotWorkflow.class,
            PropertiesDotWorkflowImpl.class,
            testEnv.getWorkflowClient());
  }

  @After
  public void tearDown() {
    testEnv.close();
  }

  @Test
  public void testTimeoutsFromPropertiesAreReadCorrectly() {
    worker.registerActivitiesImplementations(timeoutActivity);
    dotWorker.registerActivitiesImplementations(timeoutActivity);
    testEnv.start();

    //    Duration scheduleToCloseTimeout = workflow.getScheduleToCloseTimeout();
    //    assertThat(scheduleToCloseTimeout).isEqualTo(Duration.ofSeconds(1));

    Duration dotWorkflowStartToCloseTimeout = dotWorkflow.getStartToCloseTimeout();
    assertThat(dotWorkflowStartToCloseTimeout).isEqualTo(Duration.ofSeconds(1));
  }
}
