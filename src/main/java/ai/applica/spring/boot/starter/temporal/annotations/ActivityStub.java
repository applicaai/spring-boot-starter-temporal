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
 * To mark activiti on a Workflow to make it a stub. One must specify duration and can specify
 * duration unit being ChronoUnit string equivalent.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface ActivityStub {

  long duration() default -1;

  String durationUnits() default "SECONDS";

  RetryActivityOptions retryOptions() default @RetryActivityOptions;

  String taskQueue() default "";
}
