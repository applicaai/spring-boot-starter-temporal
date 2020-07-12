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

import ai.applica.spring.boot.starter.temporal.annotations.ActivityOptionsModifier;
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import io.temporal.activity.ActivityOptions;
import io.temporal.activity.ActivityOptions.Builder;
import io.temporal.workflow.Workflow;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
public class ActivityStubIntercepter {
  private Class<?> targetClass;

  public ActivityStubIntercepter(Class<?> targetClass) {
    this.targetClass = targetClass;
  }

  @RuntimeType
  public Object process(@This Object obj, @SuperCall Callable<Object> call) throws Exception {

    for (Field field : targetClass.getDeclaredFields()) {
      // Check for our annotation
      ActivityStub[] annotations = field.getAnnotationsByType(ActivityStub.class);
      if (annotations.length > 0) {
        ReflectionUtils.makeAccessible(field);
        try {
          if (field.get(obj) == null) {
            ActivityStub activitiStubAnnotation = field.getAnnotation(ActivityStub.class);
            Object was =
                Workflow.newActivityStub(
                    field.getType(), buildOptions(obj, activitiStubAnnotation, field));
            field.set(obj, was);
            log.debug(
                "ActivityStub created for activiti "
                    + field.getType()
                    + " on workflow "
                    + obj.getClass().getSimpleName());
          } else {
            log.debug(
                "ActivityStub not created for activiti "
                    + field.getType()
                    + " on workflow "
                    + obj.getClass().getSimpleName()
                    + " field not null");
          }
        } catch (IllegalArgumentException | IllegalAccessException e) {
          throw new RuntimeException(e);
        }
      }
    }
    return call.call();
  }

  private ActivityOptions buildOptions(
      Object target, ActivityStub activitiStubAnnotation, Field field) {
    // Build default options
    Builder options = ActivityOptions.newBuilder();
    options.setScheduleToCloseTimeout(
        Duration.ofSeconds(activitiStubAnnotation.durationInSeconds()));
    // chk for modifier
    Set<Method> methods =
        MethodIntrospector.selectMethods(
            targetClass,
            (ReflectionUtils.MethodFilter)
                method ->
                    AnnotationUtils.findAnnotation(method, ActivityOptionsModifier.class) != null
                        && method.getParameterTypes()[0] == field.getType());
    if (methods.size() > 0) {
      Method method = (Method) methods.toArray()[0];
      log.debug("Found options modifier by name " + method.getName() + " on object " + targetClass);
      ReflectionUtils.makeAccessible(method);
      try {
        options = (Builder) method.invoke(target, field.getType(), options);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    } else {
      log.debug(
          "Not options modifier method found for " + field.getName() + " on object " + targetClass);
    }
    return options.build();
  }
}
