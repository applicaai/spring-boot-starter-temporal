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

package ai.applica.spring.boot.starter.temporal;

import ai.applica.spring.boot.starter.temporal.annotations.TemporalWorkflow;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import ai.applica.spring.boot.starter.temporal.processors.ActivityStubIntercepter;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowOptions.Builder;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.WorkflowMethod;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * E - workflow interface I - workflow implementation Two parameters so to autowire both by
 * interface and implementation
 */
@RequiredArgsConstructor
public class WorkflowFactory {

  private final TemporalProperties temporalProperties;
  private final WorkflowClient workflowClient;

  public <T> T makeStub(Class<T> workflowInterface, Class<? extends T> workflowClass) {

    Builder optionsBuilder = defaultOptionsBuilder(workflowClass);
    return makeStub(workflowInterface, optionsBuilder);
  }

  public <T> T makeStub(Class<T> workflowInterface, Builder optionsBuilder) {
    return makeStub(workflowInterface, optionsBuilder, null);
  }

  public Builder defaultOptionsBuilder(Class<?> workflowClass) {
    TemporalWorkflow workflow =
        AnnotationUtils.findAnnotation(workflowClass, TemporalWorkflow.class);

    WorkflowOption option = temporalProperties.getWorkflows().get(workflow.value());
    return WorkflowOptions.newBuilder()
        .setTaskQueue(option.getTaskQueue())
        .setWorkflowExecutionTimeout(Duration.ofSeconds(option.getExecutionTimeout()));
  }

  public <T> T makeStub(
      Class<T> workflowInterface,
      Class<? extends T> workflowClass,
      WorkflowClient testWorkflowClient) {
    Builder optionsBuilder = defaultOptionsBuilder(workflowClass);
    return makeStub(workflowInterface, optionsBuilder, testWorkflowClient);
  }

  public <T> T makeStub(
      Class<T> workflowInterface, Builder optionsBuilder, WorkflowClient testWorkflowClient) {
    WorkflowClient lwc = workflowClient;
    if (testWorkflowClient != null) {
      lwc = testWorkflowClient;
    }
    T stub = lwc.newWorkflowStub(workflowInterface, optionsBuilder.build());
    return stub;
  }

  public Worker makeWorker(TestWorkflowEnvironment testEnv, Class<?>... targetClasses) {
    String taskQueue = null;
    Class<?>[] types = new Class<?>[targetClasses.length];
    int i = 0;
    for (Class<?> targetClass : targetClasses) {
      TemporalWorkflow workflowAnotation =
          AnnotationUtils.findAnnotation(targetClass, TemporalWorkflow.class);
      WorkflowOption option = temporalProperties.getWorkflows().get(workflowAnotation.value());
      taskQueue = option.getTaskQueue();
      types[i++] = makeWorkflowClass(targetClass);
    }
    Worker worker = testEnv.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(types);
    return worker;
  }

  public Class<?> makeWorkflowClass(Class<?> targetClass) {
    Set<Method> methods =
        MethodIntrospector.selectMethods(
            targetClass,
            (ReflectionUtils.MethodFilter)
                method -> AnnotationUtils.findAnnotation(method, WorkflowMethod.class) != null);

    Method method = (Method) methods.toArray()[0];

    Unloaded<?> beanU =
        new ByteBuddy()
            .subclass(targetClass)
            .implement(targetClass.getInterfaces()[0])
            .method(ElementMatchers.named(method.getName()))
            .intercept(MethodDelegation.to(new ActivityStubIntercepter(targetClass)))
            .make();
    Loaded<?> beanL = beanU.load(targetClass.getClassLoader());
    return beanL.getLoaded();
  }
}
