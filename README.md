# Spring Boot starter for Temporal.io
## About
This is the driver making it convenient to use Temporal with Spring Boot. It is intended especially on making code that uses Services to start workflows, send signals to them or use queries. To make Activities be Services as well and use all the benefits of Spring to communicate with outside world.

## Configuration

### Gradle
```gradle
implementation 'com.github.applicaai:spring-boot-starter-temporal:0.5.4-SNAPSHOT'
```

### Maven
```maven
<dependency>
    <groupId>com.github.applicaai</groupId>
    <artifactId>spring-boot-starter-temporal</artifactId>
    <version>0.5.4-SNAPSHOT</version>
</dependency>
```
## Usage

Instead of reading this README you can see examples in test directory `samples` folder.
## First steps
### First things first

For anything to work you will have to have application annotated with
proper annotation:
```java
@EnableTemporal
@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
```
You will also need the source of application properties with something like this:
```yaml
spring.temporal:
  # host: localhost
  # port: 7233
  # useSsl: false
  # createWorkers: true
  workflowDefaults:
    executiontimeout: 1000
    executiontimeoutUnit: SECONDS
    activityPoolSize: 100
    workflowPoolSize: 50
  workflows:
    one:
      taskQueue: one
    two:
      taskQueue: one
```
For the docker running locally you do not need any configuration of the host. For the workflows it is convenient to define workflowDefaults as 
not to repeat same configuration on every workflow.

### Defining workflow 
Than for the workflow implementation there are some little changes from 
original Temporal:
```java
@Component
@TemporalWorkflow("myflow")
public class WorkflowImpl implements HelloWorkflow {
  ...
}
```

As you can see you need to add `@Componet` and `@TemporalWorkflow` annotations for the implementation class. This will create worker for you so you do not need to start it anywhere. The parameter for `@TemporalWorkflow` is the name in configuration that specify
options both for worker and its stubs:

```yaml
 spring.temporal:
   workflows:
    myflow:
      executiontimeout: 1000
      executiontimeoutUnit: SECONDS
      activityPoolSize: 100
      workflowPoolSize: 50
```

Then for every activity in workflow implementation class instead of 
Activity stub instantiation you annotate it with `@ActivityStub`:

```java
@ActivityStub(duration = 10, durationUnits = "SECONDS")
public SomeActivity someActivity;
```

There is a duration parameter that's setting `ScheduleToCloseTimeout` on stub.
Or you can use:
```yaml
  activityStubDefaults:
    scheduleToCloseTimeout: 10
    scheduleToCloseTimeoutUnit: SECONDS
  activityStubs:
    #values for GreetingActivities used in any workflow (default)
    GreetingActivities:
      scheduleToCloseTimeout: 20
      scheduleToCloseTimeoutUnit: SECONDS
    #value for GreetingActivities used in HelloWorkflow interface implementation; it has higher precedence than the default
    #please notice, how to escape dot character in yaml keys 
    "[HelloWorkflow.GreetingActivities]":
      scheduleToCloseTimeout: 10
      scheduleToCloseTimeoutUnit: SECONDS
```

### Defining activities
The only thing you need is for the activity to be a Spring Boot `@Service`.
```java
@Service
public class SomeActivityImpl implements SomeActivity {
  public String say(String string) {
    ...
  }
}
```

### Calling your workflow

To call your workflow you will need `WorkflowFactory` an one of its many
`makeStub` methods:

```java
  @Autowired private WorkflowFactory fact;
public void callWorkflowMethod() {
  MyWorkflow workflow = 
        fact.makeStub(MyWorkflow.class, MyWorkflow.class);
  workflow.process();
}

```
### Adding child workflow

You can use child workflows only with dedicated queue `ChildWorkflowOptions.newBuilder().setTaskQueue(TASK_QUEUE).build();`.
Spring Boot starter for Temporal.io do not support multi-workflow queues as for now so no workers without task queue can be called.

## Advanced stuff

### Adding options to the activity stubs

To add some additional options to activity stubs you use
`@ActivityOptionsModifier` annotation on the method in 
service implementation you need it. You need to specify
the class of your activity as first argument for the system
to find your method and you will receive activity options 
builder as second argument.

```java
@ActivityStub(duration = 10)
private GreetingActivities activities;

@ActivityOptionsModifier
private ActivityOptions.Builder modifyOptions(
    Class<GreetingActivities> cls, ActivityOptions.Builder options) {
  options.setHeartbeatTimeout(Duration.ofSeconds(2)).build());
  return options;
}
```

### Adding options to workflow stubs

Use `WorkflowFactory#defaultOptionsBuilder` and pass options builder to 
`WorkflowFactory#makeStub`.

### Adding default options to stubs and to workers

Use `TemporalOptionsConfiguration` class instantiated as a bean in your configuration.
Like this:
```java
public class TestTemporalOptionsConfiguration implements TemporalOptionsConfiguration {

  @Override
  public WorkflowClientOptions.Builder modifyClientOptions(
      WorkflowClientOptions.Builder newBuilder) {
    return newBuilder;
  }

  @Override
  public WorkflowOptions.Builder modifyDefalutStubOptions(
      WorkflowOptions.Builder newBuilder) {
    return newBuilder;
  }
}
```
### Disable automatic worker creation

Spring boot starter for Temporal will usually create workers for all implemented workflows.
If for some reasons you do not wont it to happen. You can add to configuration:
```yaml
spring.temporal:
  createWorkers: false
```

You can also disable creation of workflows for given test when testing using annotation `@TemporalTest`
### Writing tests

Pleas look into test directory `samples` folder in the sources.

