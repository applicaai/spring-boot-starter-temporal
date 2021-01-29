# Spring Boot starter for Temporal.io
## About
This is the driver making it convenient to use Temporal with Spring Boot. It is intended especially on making code that uses Services to start workflows, send signals to them or use queries. To make Activities be Services as well and use all the benefits of Spring to communicate with outside world.

## Configuration

### Gradle
```gradle
implementation 'com.github.applicaai:spring-boot-starter-temporal:0.6.0-SNAPSHOT'
```

### Maven
```maven
<dependency>
    <groupId>com.github.applicaai</groupId>
    <artifactId>spring-boot-starter-temporal</artifactId>
    <version>0.6.0-SNAPSHOT</version>
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
Then for the workflow implementation there are some little changes from 
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
Activity stub instantiation you annotate it with `@ActivityStub`.

Or you can use:
```yaml
  activityStubDefaults:
    scheduleToCloseTimeout: PT10S
  activityStubs:
    #values for GreetingActivities used in any workflow (default)
    GreetingActivities:
      scheduleToCloseTimeout: PT20S
    PropertiesActivity:
      scheduleToStartTimeout: PT1S
      startToCloseTimeout: PT15S
    #value for GreetingActivities used in HelloWorkflow interface implementation; it has higher precedence than the default
    #please notice, how to escape dot character in yaml keys 
    "[PropertiesDotWorkflow.PropertiesActivity]":
      scheduleToCloseTimeout: PT3M
    TimeoutPropertiesActivity:
      startToCloseTimeout: PT100H
      scheduleToCloseTimeout: PT1M
```

### @ActivityStub

```java
@ActivityStub(startToClose = "PT10S")
public SomeActivity someActivity;
```

Since `0.7.0-SNAPSHOT` `duration` and `durationUnits` properties are deprecated.

Instead, please use one of:
- `scheduleToClose`
- `scheduleToStart`
- `startToClose`
- `heartbeat`

These are temporal equivalents of timeout properties. Their values are in Java duration format.

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

If you would like to have the activity on separate worker add `@TemporalActivity` annotation:
```java
@Service
@TemporalActivity("SomeName")
public class SomeActivityImpl implements SomeActivity {
  public String say(String string) {
    ...
  }
}
```

And add parameters to set task queue and optionally worker parameters:

```yaml
  activityWorkerDefaults:
    activityPoolSize: 9
   activityWorkers:
    SomeName:
      activityPoolSize: 10
      taskQueue: someActivityQueue

```

If you do so remember to add `taskQueue` parameter to annotation of the activity stub on the workflow as well.
You can find example of such a process in `samples/app` folder in the tests `HelloActivitySepareteWorker.java`.

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
@ActivityStub(duration = "PT10S")
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

Please look into test directory `samples` folder in the sources.

