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
import java.time.temporal.ChronoUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface RetryActivityOptions {
  long initialInterval() default DEFAULT_INITIAL_INTERVAL;

  ChronoUnit initialIntervalUnits() default ChronoUnit.SECONDS;

  double backoffCoefficient() default DEFAULT_BACKOFF_COEFFICIENT;

  int maximumAttempts() default DEFAULT_MAXIMUM_ATTEMPTS;

  long maximumInterval() default DEFAULT_MAXIMUM_INTERVAL;

  ChronoUnit maximumIntervalUnits() default ChronoUnit.SECONDS;

  String[] doNotRetry() default {};

  long DEFAULT_INITIAL_INTERVAL = -1;

  double DEFAULT_BACKOFF_COEFFICIENT = -1.0;

  int DEFAULT_MAXIMUM_ATTEMPTS = -1;

  long DEFAULT_MAXIMUM_INTERVAL = -1;
}
