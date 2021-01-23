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

import ai.applica.spring.boot.starter.temporal.annotations.TemporalActivity;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;

@Slf4j
@Configuration
@Profile("!temporal_test")
@RequiredArgsConstructor
public class ActivityAnnotationBeanPostProcessor
    implements BeanPostProcessor, Ordered, SmartInitializingSingleton {

  private final TemporalProperties temporalProperties;
  private final WorkerFactory workerFactory;
  private final Set<String> classes = new HashSet<>();

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName)
      throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName)
      throws BeansException {

    if (!temporalProperties.isCreateWorkers() || classes.contains(bean.getClass().getName())) {
      return bean;
    }

    Class<?> targetClass = AopUtils.getTargetClass(bean);
    TemporalActivity activityAnotation =
        AnnotationUtils.findAnnotation(targetClass, TemporalActivity.class);

    if (activityAnotation == null) {
      return bean;
    }

    log.info("Registering activiti worker for {}", targetClass);

    Map<String, WorkflowOption> activityWorkersProperties = temporalProperties.getActivityWorkers();
    String value = activityAnotation.value();
    WorkflowOption options = activityWorkersProperties.get(value);
    if (options == null) {
      throw new RuntimeException("No configuration defined for activityWorker: " + value);
    }
    Worker worker = workerFactory.newWorker(options.getTaskQueue(), getWorkerOptions(options));

    worker.registerActivitiesImplementations(bean);

    classes.add(bean.getClass().getName());
    return bean;
  }

  private WorkerOptions getWorkerOptions(WorkflowOption option) {
    return WorkerOptions.newBuilder()
        .setMaxConcurrentActivityExecutionSize(option.getActivityPoolSize())
        .setMaxConcurrentWorkflowTaskExecutionSize(option.getWorkflowPoolSize())
        .build();
  }

  @Override
  public void afterSingletonsInstantiated() {
    if (temporalProperties.isCreateWorkers()) {
      workerFactory.start();
    }
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }
}
