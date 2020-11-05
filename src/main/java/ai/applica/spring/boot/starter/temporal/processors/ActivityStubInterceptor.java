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
import ai.applica.spring.boot.starter.temporal.annotations.RetryActivityOptions;
import ai.applica.spring.boot.starter.temporal.config.TemporalOptionsConfiguration;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.ActivityStubOptions;
import com.google.common.collect.ObjectArrays;
import io.temporal.activity.ActivityOptions;
import io.temporal.activity.ActivityOptions.Builder;
import io.temporal.common.RetryOptions;
import io.temporal.workflow.Workflow;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
@RequiredArgsConstructor
public class ActivityStubInterceptor {
  private Class<?> targetClass;

  private TemporalOptionsConfiguration temporalOptionsConfiguration;
  private TemporalProperties temporalProperties;

  public ActivityStubInterceptor(
      Class<?> targetClass,
      TemporalOptionsConfiguration temporalOptionsConfiguration,
      TemporalProperties temporalProperties) {
    this.targetClass = targetClass;
    this.temporalOptionsConfiguration = temporalOptionsConfiguration;
    this.temporalProperties = temporalProperties;
  }

  @RuntimeType
  public Object process(@This Object obj, @SuperCall Callable<Object> call) throws Exception {
    for (Field field : targetClass.getDeclaredFields()) {
      createActivityStubs(obj, field);
    }
    return call.call();
  }

  private void createActivityStubs(Object obj, Field field) {
    ActivityStub[] annotations = field.getAnnotationsByType(ActivityStub.class);
    if (annotations.length > 0) {
      ReflectionUtils.makeAccessible(field);
      try {
        if (field.get(obj) == null) {
          ActivityStub activityStubAnnotation = field.getAnnotation(ActivityStub.class);
          Object was =
              Workflow.newActivityStub(
                  field.getType(), buildOptions(obj, activityStubAnnotation, field));
          field.set(obj, was);
          log.debug(
              "ActivityStub created for activity {} on workflow {}",
              field.getType(),
              obj.getClass().getSimpleName());
        } else {
          log.debug(
              "ActivityStub not created for activity {} on workflow {} field not null",
              field.getType(),
              obj.getClass().getSimpleName());
        }
      } catch (IllegalArgumentException | IllegalAccessException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private ActivityOptions buildOptions(
      Object target, ActivityStub activityStubAnnotation, Field field) {

    // Build default options
    Builder options = ActivityOptions.newBuilder();

    // from configuration
    if (temporalProperties.getActivityStubDefaults() != null) {
      ActivityStubOptions activityStubDefaults = temporalProperties.getActivityStubDefaults();
      options.setScheduleToCloseTimeout(
          Duration.of(
              activityStubDefaults.getScheduleToCloseTimeout(),
              ChronoUnit.valueOf(activityStubDefaults.getScheduleToCloseTimeoutUnit())));
    }

    // from default method
    if (temporalOptionsConfiguration != null) {
      options = temporalOptionsConfiguration.modifyDefaultActivityOptions(options);
    }
    if (activityStubAnnotation.duration() != -1) {
      options.setScheduleToCloseTimeout(
          Duration.of(
              activityStubAnnotation.duration(),
              ChronoUnit.valueOf(activityStubAnnotation.durationUnits())));
    }

    if (nonDefaultRetryOptions(activityStubAnnotation.retryOptions())) {
      options.setRetryOptions(mergeRetryOptions(activityStubAnnotation.retryOptions(), options));
    }
    // from configuration of particular activity stub
    if (temporalProperties.getActivityStubs() != null) {
      String stubName =
          field.getDeclaringClass().getSimpleName() + "." + field.getType().getSimpleName();
      for (Map.Entry<String, ActivityStubOptions> entry :
          temporalProperties.getActivityStubs().entrySet()) {
        if (stubName.contains(entry.getKey())) {
          options.setScheduleToCloseTimeout(
              Duration.of(
                  entry.getValue().getScheduleToCloseTimeout(),
                  ChronoUnit.valueOf(entry.getValue().getScheduleToCloseTimeoutUnit())));
        }
      }
    }
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
      log.debug("Found options modifier by name {} on object {}", method.getName(), targetClass);
      ReflectionUtils.makeAccessible(method);
      try {
        options = (Builder) method.invoke(target, field.getType(), options);
      } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
        throw new RuntimeException(e);
      }
    } else {
      log.debug(
          "Not options modifier method found for {} on object {}", field.getName(), targetClass);
    }
    return options.build();
  }

  private RetryOptions mergeRetryOptions(
      RetryActivityOptions retryActivityOptions, Builder options) {
    log.debug("Overriding retry option with values from annotation");
    RetryOptions defaultRetryOptions = options.validateAndBuildWithDefaults().getRetryOptions();
    RetryOptions.Builder mergedRetryOptions = RetryOptions.newBuilder(defaultRetryOptions);
    // initialInterval
    if (retryActivityOptions.initialInterval() != RetryActivityOptions.DEFAULT_INITIAL_INTERVAL) {
      log.debug(
          "Overriding retry option 'initialInterval' with value {}{}",
          retryActivityOptions.initialInterval(),
          retryActivityOptions.initialIntervalUnits());
      mergedRetryOptions.setInitialInterval(
          Duration.of(
              retryActivityOptions.initialInterval(), retryActivityOptions.initialIntervalUnits()));
    }
    // backoffCoefficient
    if (retryActivityOptions.backoffCoefficient()
        != RetryActivityOptions.DEFAULT_BACKOFF_COEFFICIENT) {
      log.debug(
          "Overriding retry option 'backoffCoefficient' with value {}",
          retryActivityOptions.backoffCoefficient());
      mergedRetryOptions.setBackoffCoefficient(retryActivityOptions.backoffCoefficient());
    }
    // maximumAttempts
    if (retryActivityOptions.maximumAttempts() != RetryActivityOptions.DEFAULT_MAXIMUM_ATTEMPTS) {
      log.debug(
          "Overriding retry option 'maximumAttempts' with value {}",
          retryActivityOptions.maximumAttempts());
      mergedRetryOptions.setMaximumAttempts(retryActivityOptions.maximumAttempts());
    }
    // maximumInterval
    if (retryActivityOptions.maximumInterval() != RetryActivityOptions.DEFAULT_MAXIMUM_INTERVAL) {
      log.debug(
          "Overriding retry option 'maximumInterval' with value {}{}",
          retryActivityOptions.maximumInterval(),
          retryActivityOptions.maximumIntervalUnits());
      mergedRetryOptions.setMaximumInterval(
          Duration.of(
              retryActivityOptions.maximumInterval(), retryActivityOptions.maximumIntervalUnits()));
    }
    // doNotRetry
    if (retryActivityOptions.doNotRetry().length > 0) {
      String[] defaultDoNotRetry =
          defaultRetryOptions != null && defaultRetryOptions.getDoNotRetry() != null
              ? defaultRetryOptions.getDoNotRetry()
              : new String[] {};
      log.debug(
          "Merging retry option 'doNotRetry' original value {} with value {}",
          Arrays.toString(defaultDoNotRetry),
          Arrays.toString(retryActivityOptions.doNotRetry()));
      mergedRetryOptions.setDoNotRetry(
          ObjectArrays.concat(retryActivityOptions.doNotRetry(), defaultDoNotRetry, String.class));
    }
    return mergedRetryOptions.build();
  }

  private boolean nonDefaultRetryOptions(RetryActivityOptions retryOptions) {
    return retryOptions.initialInterval() != RetryActivityOptions.DEFAULT_INITIAL_INTERVAL
        || retryOptions.backoffCoefficient() != RetryActivityOptions.DEFAULT_BACKOFF_COEFFICIENT
        || retryOptions.maximumAttempts() != RetryActivityOptions.DEFAULT_MAXIMUM_ATTEMPTS
        || retryOptions.maximumInterval() != RetryActivityOptions.DEFAULT_MAXIMUM_INTERVAL
        || retryOptions.doNotRetry().length > 0;
  }
}
