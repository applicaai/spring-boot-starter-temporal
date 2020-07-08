package ai.applica.spring.boot.starter.temporal.processors;

import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowOptions;
import ai.applica.spring.boot.starter.temporal.annotations.Workflow;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties;
import ai.applica.spring.boot.starter.temporal.config.TemporalProperties.WorkflowOption;
import io.temporal.worker.Worker;
import io.temporal.worker.WorkerFactory;
import io.temporal.worker.WorkerOptions;
import io.temporal.workflow.WorkflowMethod;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.HashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.BeanPostProcessor;
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
  private final TemporalProperties  temporalProperties;
  private final WorkerFactory workerFactory;
  private final Set<String> classes = new HashSet<>();

  private BeanFactory beanFactory;

  @Override
  public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
    return bean;
  }

  @Override
  public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException {
    if (classes.contains(bean.getClass().getName())) {
      return bean;
    }

    Class<?> targetClass = AopUtils.getTargetClass(bean);
    Workflow workflow = AnnotationUtils.findAnnotation(targetClass, Workflow.class);

    if (workflow == null) {
      return bean;
    }

    Set<Method> methods = MethodIntrospector.selectMethods(targetClass,
        (ReflectionUtils.MethodFilter) method ->
            AnnotationUtils.findAnnotation(method, WorkflowMethod.class) != null);

    if (methods.isEmpty()) {

      log.info("No @WorkflowMethod found on bean {}", bean.getClass());
      return bean;

    } else {

      log.info("Registering worker for {}", targetClass);

      // Регистрируем воркера с проксей от бина имплементации

      WorkflowOption option = temporalProperties.getWorkflows().get(workflow.value());

      Worker worker = workerFactory
          .newWorker(option.getTaskList(), getWorkerOptions(option));

      Enhancer enhancer = new Enhancer();
      enhancer.setSuperclass(bean.getClass());
      enhancer.setCallback((MethodInterceptor) (obj, method, args, proxy) -> {

        if (methods.contains(method)) {
          WorkflowOptions options = WorkflowOptions.newBuilder()
              .setTaskQueue(workflow.value())
              .setWorkflowExecutionTimeout(
                  Duration.ofSeconds(option.getExecutionTimeout()))
              .build();

          Object stub = workflowClient.newWorkflowStub(targetClass.getInterfaces()[0], options);

          return stub.getClass().getMethod(method.getName()).invoke(stub, args);
        } else {
          return proxy.invokeSuper(obj, args);
        }
      });

      Object o = enhancer.create();

      worker.addWorkflowImplementationFactory(
          (Class<Object>) targetClass.getInterfaces()[0],
          () -> ((DefaultListableBeanFactory) beanFactory).configureBean(bean, beanName));

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
