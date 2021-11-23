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

import ai.applica.spring.boot.starter.temporal.*;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.extensions.TemporalTestWatcher;
import ai.applica.spring.boot.starter.temporal.samples.apps.HelloActivityAnnotation;
import io.temporal.testing.TestWorkflowEnvironment;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

/** Unit test for {@link RemoteActivity}. Doesn't use an external Temporal service. */
@SpringBootTest
@TemporalTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RemoteActivityTest {

  static final String ACTIVITY_STUB_CONFIG_NAME = "RemoteActivities";

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
        temporalProperties.getActivityStubs().get(ACTIVITY_STUB_CONFIG_NAME).getTaskQueue();
    testEnv.newWorker(remoteTaskQueue).registerActivitiesImplementations(remoteActivity);
  }
}
