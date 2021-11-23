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

import ai.applica.spring.boot.starter.temporal.WorkflowFactory;
import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import ai.applica.spring.boot.starter.temporal.config.TemporalOptionsConfiguration;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import io.temporal.client.WorkflowClient;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.WorkflowMethod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Configuration
@Profile("!temporal_test")
@RequiredArgsConstructor
public class WorkflowAnnotationBeanPostProcessor
    implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

  private final TemporalProperties temporalProperties;
  private final TemporalOptionsConfiguration temporalOptionsConfiguration;
  private final WorkerFactory workerFactory;
  private final WorkflowClient workflowClient;
  private final Set<String> classes = new HashSet<>();

  private BeanFactory beanFactory;

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
    TemporalWorkflow workflow = AnnotationUtils.findAnnotation(targetClass, TemporalWorkflow.class);

    if (workflow == null) {
      return bean;
    }

    Set<Method> methods =
        MethodIntrospector.selectMethods(
            targetClass,
            (ReflectionUtils.MethodFilter)
                method -> AnnotationUtils.findAnnotation(method, WorkflowMethod.class) != null);

    if (methods.isEmpty()) {

      log.info("No @WorkflowMethod found on bean {}", bean.getClass());
      return bean;

    } else {

      log.info("Registering worker for {}", targetClass);

      Map<String, WorkflowOption> workflows = temporalProperties.getWorkflows();
      String value = workflow.value();
      WorkflowOption options = workflows.get(value);
      if (options == null) {
        throw new RuntimeException("No configuration defined for workflow: " + value);
      }
      Worker worker = workerFactory.newWorker(options.getTaskQueue(), getWorkerOptions(options));

      WorkflowFactory workflowFactory =
          new WorkflowFactory(temporalProperties, workflowClient, temporalOptionsConfiguration);

      try {
        List<Object> activities = getBeansForInnerWorker(beanName, targetClass);

        if (!activities.isEmpty()) {
          worker.registerActivitiesImplementations(activities.toArray());
        }
      } catch (IllegalArgumentException | SecurityException e) {
        new RuntimeException(e);
      }

      // we create the worker
      worker.registerWorkflowImplementationTypes(workflowFactory.makeWorkflowClass(targetClass));

      classes.add(bean.getClass().getName());
    }
    return bean;
  }
  /** List beans annotated with ActivitiStub with no taskQueue to call. */
  private List<Object> getBeansForInnerWorker(final String beanName, Class<?> targetClass) {
    List<Object> activities = new ArrayList<Object>();
    for (Field field : targetClass.getDeclaredFields()) {
      ActivityStub[] annotations = field.getAnnotationsByType(ActivityStub.class);
      if (annotations.length > 0
          && "".equals(annotations[0].taskQueue())
          && !hasActivityStubConfiguration(field)) {
        DependencyDescriptor desc = new DependencyDescriptor(field, true);
        Object dep = ((DefaultListableBeanFactory) beanFactory).resolveDependency(desc, beanName);
        activities.add(dep);
      }
    }
    return activities;
  }

  private boolean hasActivityStubConfiguration(Field field) {
    Map stubsMap = temporalProperties.getActivityStubs();
    if (stubsMap != null) {
      String simpleStubName = field.getType().getSimpleName();
      String fullStubName =
          field.getDeclaringClass().getInterfaces()[0].getSimpleName() + "." + simpleStubName;
      return stubsMap.containsKey(simpleStubName) || stubsMap.containsKey(fullStubName);
    } else {
      return false;
    }
  }

  private WorkerOptions getWorkerOptions(WorkflowOption option) {
    WorkerOptions.Builder workerOptions =
        WorkerOptions.newBuilder()
            .setMaxConcurrentWorkflowTaskExecutionSize(option.getWorkflowPoolSize());

    if (option.getWorkflowPollThreadPoolSize() != null) {
      workerOptions.setWorkflowPollThreadCount(option.getWorkflowPollThreadPoolSize());
    }
    if (option.getActivityPoolSize() != null) {
      workerOptions.setMaxConcurrentActivityExecutionSize(option.getActivityPoolSize());
    }
    if (option.getActivityPollThreadPoolSize() != null) {
      workerOptions.setActivityPollThreadCount(option.getActivityPollThreadPoolSize());
    }
    return workerOptions.build();
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
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
