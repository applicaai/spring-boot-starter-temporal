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
import ai.applica.spring.boot.starter.temporal.config.TemporalOptionsConfiguration;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import ai.applica.spring.boot.starter.temporal.processors.ActivityStubInterceptor;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import io.temporal.client.WorkflowOptions.Builder;
import io.temporal.testing.TestWorkflowEnvironment;
import io.temporal.worker.Worker;
import io.temporal.workflow.WorkflowMethod;
import java.lang.reflect.Method;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType.Loaded;
import net.bytebuddy.dynamic.DynamicType.Unloaded;
import net.bytebuddy.implementation.MethodDelegation;
import net.bytebuddy.matcher.ElementMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodIntrospector;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Factory to instantiate workers based on bean versions of workflows. Plus conviniet methods to
 * build stubs for workflows based on configuration.
 */
@RequiredArgsConstructor
public class WorkflowFactory {

  private final TemporalProperties temporalProperties;
  private final WorkflowClient workflowClient;

  @Autowired(required = false)
  private TemporalOptionsConfiguration temporalOptionsConfiguration;

  /**
   * Builds workflow stub similary to <code>WorkflowClient#newWorkflowStub</code> but with options
   * taken from properties based on annotation of implementation class.
   *
   * @param <T>
   * @param workflowInterface
   * @param workflowClass
   * @return
   */
  public <T> T makeStub(Class<T> workflowInterface, Class<? extends T> workflowClass) {

    Builder optionsBuilder = defaultOptionsBuilder(workflowClass);
    return makeStub(workflowInterface, optionsBuilder);
  }
  /**
   * Builds workflow stub similary to <code>WorkflowClient#newWorkflowStub</code> but with options
   * taken from properties by name given.
   *
   * @param <T>
   * @param workflowInterface
   * @param workflowName
   * @return
   */
  public <T> T makeStub(Class<T> workflowInterface, String workflowName) {

    Builder optionsBuilder = defaultOptionsBuilder(workflowName);
    return makeStub(workflowInterface, optionsBuilder);
  }
  /**
   * Builds workflow stub similary to <code>WorkflowClient#newWorkflowStub</code>.
   *
   * @param <T>
   * @param workflowInterface
   * @param optionsBuilder
   * @return
   */
  public <T> T makeStub(Class<T> workflowInterface, Builder optionsBuilder) {
    return makeStub(workflowInterface, optionsBuilder, null);
  }
  /**
   * Prepares option builder based on workflow class annotation and spring properties.
   *
   * @param workflowClass
   * @return
   */
  public Builder defaultOptionsBuilder(Class<?> workflowClass) {
    TemporalWorkflow workflowAnnotation =
        AnnotationUtils.findAnnotation(workflowClass, TemporalWorkflow.class);

    return defaultOptionsBuilder(workflowAnnotation.value());
  }
  /**
   * Prepares option builder based on workflow name from spring properties.
   *
   * @param workflowName
   * @return
   */
  public Builder defaultOptionsBuilder(String workflowName) {
    WorkflowOption option = temporalProperties.getWorkflows().get(workflowName);
    Builder builder =
        WorkflowOptions.newBuilder()
            .setTaskQueue(option.getTaskQueue())
            .setWorkflowExecutionTimeout(
                Duration.of(
                    option.getExecutionTimeout(),
                    ChronoUnit.valueOf(option.getExecutionTimeoutUnit())));
    if (temporalOptionsConfiguration != null) {
      builder = temporalOptionsConfiguration.modifyDefaultStubOptions(builder);
    }
    return builder;
  }
  /**
   * Prepares option builder based on workflow class annotation and spring properties. Test version
   * of makeStub method.
   *
   * @param <T>
   * @param workflowInterface
   * @param workflowClass
   * @param testWorkflowClient
   * @return
   */
  public <T> T makeStub(
      Class<T> workflowInterface,
      Class<? extends T> workflowClass,
      WorkflowClient testWorkflowClient) {
    Builder optionsBuilder = defaultOptionsBuilder(workflowClass);
    return makeStub(workflowInterface, optionsBuilder, testWorkflowClient);
  }
  /**
   * Builds workflow stub similary to <code>WorkflowClient#newWorkflowStub</code> but with options
   * taken from properties by name given. Test version of makeStub method.
   *
   * @param <T>
   * @param workflowInterface
   * @param workflowName
   * @param testWorkflowClient
   * @return
   */
  public <T> T makeStub(
      Class<T> workflowInterface, String workflowName, WorkflowClient testWorkflowClient) {
    Builder optionsBuilder = defaultOptionsBuilder(workflowName);
    return makeStub(workflowInterface, optionsBuilder, testWorkflowClient);
  }
  /**
   * Builds workflow stub similary to <code>WorkflowClient#newWorkflowStub</code>. Test version of
   * makeStub method.
   *
   * @param <T>
   * @param workflowInterface
   * @param optionsBuilder
   * @param testWorkflowClient
   * @return
   */
  public <T> T makeStub(
      Class<T> workflowInterface, Builder optionsBuilder, WorkflowClient testWorkflowClient) {
    WorkflowClient lwc = workflowClient;
    if (testWorkflowClient != null) {
      lwc = testWorkflowClient;
    }
    T stub = lwc.newWorkflowStub(workflowInterface, optionsBuilder.build());
    return stub;
  }
  /**
   * Test version of making workers. On production it is done automaticly. Remember not to use
   * uprocessed classes that are not beans and will not work or not work properly.
   *
   * @param testEnv
   * @param targetClasses
   * @return
   */
  public Worker makeWorker(TestWorkflowEnvironment testEnv, Class<?>... targetClasses) {
    String taskQueue = null;
    Class<?>[] types = new Class<?>[targetClasses.length];
    int i = 0;
    for (Class<?> targetClass : targetClasses) {
      TemporalWorkflow workflowAnotation =
          AnnotationUtils.findAnnotation(targetClass, TemporalWorkflow.class);
      WorkflowOption option = temporalProperties.getWorkflows().get(workflowAnotation.value());
      if(option == null){
        throw new RuntimeException("No properties specitied for " + workflowAnotation.value());
      }
      taskQueue = option.getTaskQueue();
      types[i++] = makeWorkflowClass(targetClass);
    }
    Worker worker = testEnv.newWorker(taskQueue);
    worker.registerWorkflowImplementationTypes(types);
    return worker;
  }
  /**
   * Test version of making worker classes to be used with <code>
   * Worker#addWorkflowImplementationFactory</code>. As to provide some partial mocking or somthing
   * similar. Remember not to use uprocessed classes that are not beans and will not work or not
   * work properly.
   *
   * @param targetClass
   * @return
   */
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
            .intercept(
                MethodDelegation.to(
                    new ActivityStubInterceptor(targetClass, temporalOptionsConfiguration)))
            .make();
    Loaded<?> beanL = beanU.load(targetClass.getClassLoader());
    return beanL.getLoaded();
  }
}
