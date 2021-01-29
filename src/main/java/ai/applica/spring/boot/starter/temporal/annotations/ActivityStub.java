/*
 *  Copyright (c) 2020 Applica.ai All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

package ai.applica.spring.boot.starter.temporal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * To mark activity on a Workflow to make it a stub. It tells how long can activity take from start
 * to close. If activity takes longer, then timeout exception is thrown.
 *
 * <p>One must specify duration string according to java.time.Duration format.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ActivityStub {

  /**
   * Sets temporal scheduleToCloseTimeout
   *
   * @deprecated as of 1.7.0 - will be removed in one of next minor version
   *     <p>Use {@link ActivityStub(scheduleToClose)} or {@link ActivityStub(startToClose)} instead.
   */
  @Deprecated
  long duration() default -1;

  @Deprecated
  String durationUnits() default "SECONDS";

  /** Equivalent to Temporal's scheduleToClose */
  String scheduleToClose() default "-PT1S";

  /** Equivalent to Temporal's scheduleToStart */
  String scheduleToStart() default "-PT1S";

  /** Equivalent to Temporal's scheduleToClose */
  String startToClose() default "-PT1S";

  /** Equivalent to Temporal's heartbeat timeout */
  String heartbeat() default "-PT1S";

  RetryActivityOptions retryOptions() default @RetryActivityOptions;

  String taskQueue() default "";
}
