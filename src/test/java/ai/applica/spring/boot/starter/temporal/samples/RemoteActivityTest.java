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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ai.applica.spring.boot.starter.temporal.HelloRemoteWorkflowImpl;
import ai.applica.spring.boot.starter.temporal.HelloWorkflow;
import ai.applica.spring.boot.starter.temporal.RemoteActivities;
import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.extensions.TemporalTestWatcher;
import ai.applica.spring.boot.starter.temporal.samples.apps.RemoteActivity;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Unit test for {@link RemoteActivity}. Creates mock activities and verifies that stub
 * configuration is taken from the YAML file ({@code spring.temporal.activityStubs}) when absent
 * from {@link @ActivityStub} annotation.
 */
class RemoteActivityTest extends BaseTest {

  @Autowired TemporalProperties temporalProperties;
  @Autowired WorkflowFactory fact;

  @RegisterExtension TemporalTestWatcher temporalTestWatcher = new TemporalTestWatcher();
  private TestWorkflowEnvironment testEnv;

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
  void testRemoteActivityConfiguration() {
    String phrase = "Hello remote!";
    RemoteActivities remoteActivity = mock(RemoteActivities.class);
    when(remoteActivity.echo(phrase)).thenReturn(phrase);

    createWorkerForRemoteActivity(remoteActivity);
    createWorkerForLocalWorkflow(HelloRemoteWorkflowImpl.class);

    HelloWorkflow workflowStub =
        fact.makeStub(
            HelloWorkflow.class, HelloRemoteWorkflowImpl.class, testEnv.getWorkflowClient());
    testEnv.start();

    String echoResult = workflowStub.process();
    assertEquals(phrase, echoResult);
  }

  private void createWorkerForLocalWorkflow(Class clazz) {
    fact.makeWorker(testEnv, clazz);
  }

  private void createWorkerForRemoteActivity(RemoteActivities remoteActivity) {
    String remoteTaskQueue =
        temporalProperties
            .getActivityStubs()
            .get(RemoteActivities.class.getSimpleName())
            .getTaskQueue();
    testEnv.newWorker(remoteTaskQueue).registerActivitiesImplementations(remoteActivity);
  }
}
