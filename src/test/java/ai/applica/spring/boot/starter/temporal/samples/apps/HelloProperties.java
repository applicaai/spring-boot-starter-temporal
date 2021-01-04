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

package ai.applica.spring.boot.starter.temporal.samples.apps;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import io.temporal.activity.Activity;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Hello World Temporal workflow that executes a single activity. Requires a local instance the
 * Temporal service to be running.
 */
public class HelloProperties {

  static final String TASK_QUEUE = "HelloProperties";

  /** Workflow interface has to have at least one method annotated with @WorkflowMethod. */
  @WorkflowInterface
  public interface PropertiesWorkflow {
    /** @return activity timeout value */
    @WorkflowMethod
    Duration getTimeout();
  }

  /** Workflow interface has to have at least one method annotated with @WorkflowMethod. */
  @WorkflowInterface
  public interface PropertiesDotWorkflow {
    /** @return activity timeout value */
    @WorkflowMethod
    Duration getTimeout();
  }

  /** Activity interface is just a POJI. */
  @ActivityInterface
  public interface PropertiesActivity {
    @ActivityMethod
    Duration getTimeout();
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#composeGreeting. */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class PropertiesWorkflowImpl implements PropertiesWorkflow {

    @ActivityStub private PropertiesActivity activity;

    @Override
    public Duration getTimeout() {
      return activity.getTimeout();
    }
  }

  /** GreetingWorkflow implementation that calls GreetingsActivities#composeGreeting. */
  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class PropertiesDotWorkflowImpl implements PropertiesDotWorkflow {

    @ActivityStub private PropertiesActivity activity;

    @Override
    public Duration getTimeout() {
      return activity.getTimeout();
    }
  }

  @Service
  static class PropertiesActivityImpl implements PropertiesActivity {

    @Override
    public Duration getTimeout() {
      return Activity.getExecutionContext().getInfo().getScheduleToCloseTimeout();
    }
  }
}
