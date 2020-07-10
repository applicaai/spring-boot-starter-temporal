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
import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.WorkflowMethod;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.MethodInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WorkflowAnnotationBeanPostProcessor
    implements BeanPostProcessor, Ordered, BeanFactoryAware, SmartInitializingSingleton {

  private final WorkflowClient workflowClient;
  private final TemporalProperties temporalProperties;
  private final WorkerFactory workerFactory;
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
    if (classes.contains(bean.getClass().getName())) {
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

      WorkflowOption option = temporalProperties.getWorkflows().get(workflow.value());

      Worker worker = workerFactory.newWorker(option.getTaskList(), getWorkerOptions(option));

      // Here we register client to call the process
      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(bean.getClass());
      enhancer.setCallback(
          (MethodInterceptor)
              (obj, method, args, proxy) -> {
                if (methods.contains(method)) {

                  WorkflowOptions options =
                      WorkflowOptions.newBuilder()
                          .setTaskQueue(workflow.value())
                          .setWorkflowExecutionTimeout(
                              Duration.ofSeconds(option.getExecutionTimeout()))
                          .build();

                  Object stub =
                      workflowClient.newWorkflowStub(targetClass.getInterfaces()[0], options);

                  return stub.getClass().getMethod(method.getName()).invoke(stub, args);
                } else {
                  return proxy.invokeSuper(obj, args);
                }
              });

      Object o = enhancer.create();
      // We add activities instantions to worker
      List<Object> activities = new ArrayList<Object>();
      List<Field> activitieFields = new ArrayList<Field>();
      try {
        for (Field field : targetClass.getDeclaredFields()) {
          ActivityStub[] annotations = field.getAnnotationsByType(ActivityStub.class);
          if (annotations.length > 0) {
            DependencyDescriptor desc = new DependencyDescriptor(field, true);
            Object dep =
                ((DefaultListableBeanFactory) beanFactory).resolveDependency(desc, beanName);
            activities.add(dep);
            activitieFields.add(field);
          }
        }
        if (activities.size() > 0) {
          worker.registerActivitiesImplementations(activities.toArray());
        }
      } catch (IllegalArgumentException | SecurityException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
      }

      // we add method stubs
      Method method = (Method) methods.toArray()[0];
      Unloaded<?> beanU =
          new ByteBuddy()
              .subclass(bean.getClass())
              .implement(bean.getClass().getInterfaces()[0])
              .method(ElementMatchers.named(method.getName()))
              .intercept(MethodDelegation.to(new ActivityStubIntercepter(activitieFields)))
              .make();
      Loaded<?> beanL = beanU.load(bean.getClass().getClassLoader());

      // we create the worker
      worker.registerWorkflowImplementationTypes(beanL.getLoaded());

      // we register the client
      ((DefaultListableBeanFactory) beanFactory).registerSingleton(beanName, o);

      classes.add(bean.getClass().getName());
    }
    return bean;
  }

  private WorkerOptions getWorkerOptions(WorkflowOption option) {
    return WorkerOptions.newBuilder()
        .setMaxConcurrentActivityExecutionSize(option.getActivityPoolSize())
        .setMaxConcurrentWorkflowTaskExecutionSize(option.getWorkflowPoolSize())
        .build();
  }

  @Override
  public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
    this.beanFactory = beanFactory;
  }

  @Override
  public void afterSingletonsInstantiated() {
    workerFactory.start();
  }

  @Override
  public int getOrder() {
    return LOWEST_PRECEDENCE;
  }
}
