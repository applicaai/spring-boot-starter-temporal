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

package ai.applica.spring.boot.starter.temporal.processors;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.springframework.util.ReflectionUtils;

public class ActivityStubIntercepter {
  private List<Field> activitieFields;
  private boolean activated = false;

  public ActivityStubIntercepter(List<Field> activitieFields) {
    this.activitieFields = activitieFields;
  }

  @RuntimeType
  public Object process(@This Object obj, @SuperCall Callable<Object> call) throws Exception {
    if (!activated) {
      activitieFields.forEach(
          field -> {
            ActivityStub asc = field.getAnnotation(ActivityStub.class);
            Object was =
                Workflow.newActivityStub(
                    field.getType(),
                    ActivityOptions.newBuilder()
                        .setScheduleToCloseTimeout(Duration.ofSeconds(asc.durationInSeconds()))
                        .build());
            try {
              ReflectionUtils.makeAccessible(field);
              field.set(obj, was);
            } catch (IllegalArgumentException | IllegalAccessException e) {
              throw new RuntimeException(e);
            }
          });
      activated = true;
    }
    return call.call();
  }
}
