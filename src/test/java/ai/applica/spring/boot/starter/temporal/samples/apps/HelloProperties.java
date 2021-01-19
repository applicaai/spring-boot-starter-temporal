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
import io.temporal.activity.ActivityInfo;
import io.temporal.activity.ActivityInterface;
import io.temporal.activity.ActivityMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;
import java.time.Duration;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

/**
 * Hello World Temporal workflow that executes a single activity. Requires a local instance the
 * Temporal service to be running.
 */
public class HelloProperties {

  public static final String SCHEDULE_TO_CLOSE_TIMEOUT_KEY = "scheduleToCloseTimeout";
  public static final String START_TO_CLOSE_TIMEOUT_KEY = "startToCloseTimeout";
  static final String TASK_QUEUE = "HelloProperties";

  @WorkflowInterface
  public interface PropertiesWorkflow {
    @WorkflowMethod
    Map<String, Duration> getTimeouts();
  }

  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class PropertiesWorkflowImpl implements PropertiesWorkflow {

    @ActivityStub private PropertiesActivity activity;

    @Override
    public Map<String, Duration> getTimeouts() {
      return activity.getTimeouts();
    }
  }

  @WorkflowInterface
  public interface PropertiesDotWorkflow {

    @WorkflowMethod
    Map<String, Duration> getTimeouts();
  }

  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class PropertiesDotWorkflowImpl implements PropertiesDotWorkflow {

    @ActivityStub private PropertiesActivity activity;

    @Override
    public Map<String, Duration> getTimeouts() {
      return activity.getTimeouts();
    }
  }

  @ActivityInterface
  public interface PropertiesActivity {
    @ActivityMethod
    Map<String, Duration> getTimeouts();
  }

  @Service
  static class PropertiesActivityImpl implements PropertiesActivity {

    @Override
    public Map<String, Duration> getTimeouts() {
      ActivityInfo info = Activity.getExecutionContext().getInfo();
      Duration startToCloseTimeout = info.getStartToCloseTimeout();
      Duration scheduleToCloseTimeout = info.getScheduleToCloseTimeout();
      return Map.of(
          START_TO_CLOSE_TIMEOUT_KEY,
          startToCloseTimeout,
          SCHEDULE_TO_CLOSE_TIMEOUT_KEY,
          scheduleToCloseTimeout);
    }
  }

  @WorkflowInterface
  public interface PropertiesTimeoutWorkflow {
    @WorkflowMethod
    Map<String, Duration> getTimeouts();
  }

  @Component
  @TemporalWorkflow(TASK_QUEUE)
  public static class PropertiesTimeoutWorkflowImpl implements PropertiesTimeoutWorkflow {

    @ActivityStub private TimeoutPropertiesActivity activity;

    @Override
    public Map<String, Duration> getTimeouts() {
      return activity.getTimeouts();
    }
  }

  @ActivityInterface
  public interface TimeoutPropertiesActivity {
    @ActivityMethod
    Map<String, Duration> getTimeouts();
  }

  @Service
  static class TimeoutPropertiesActivityImpl implements TimeoutPropertiesActivity {

    @Override
    public Map<String, Duration> getTimeouts() {
      ActivityInfo info = Activity.getExecutionContext().getInfo();
      Duration startToCloseTimeout = info.getStartToCloseTimeout();
      Duration scheduleToCloseTimeout = info.getScheduleToCloseTimeout();
      return Map.of(
          START_TO_CLOSE_TIMEOUT_KEY,
          startToCloseTimeout,
          SCHEDULE_TO_CLOSE_TIMEOUT_KEY,
          scheduleToCloseTimeout);
    }
  }
}
