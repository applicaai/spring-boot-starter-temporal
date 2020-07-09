package ai.applica.spring.boot.starter.temporal.processors;

import java.lang.reflect.Field;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import org.springframework.util.ReflectionUtils;

import ai.applica.spring.boot.starter.temporal.annotations.ActivityStub;
import io.temporal.activity.ActivityOptions;
import io.temporal.workflow.Workflow;
import net.bytebuddy.implementation.bind.annotation.RuntimeType;
import net.bytebuddy.implementation.bind.annotation.SuperCall;
import net.bytebuddy.implementation.bind.annotation.This;

public class ActivityStubIntercepter {
    private List<Field> activitieFields;
    private boolean activated = false;

    public ActivityStubIntercepter(List<Field> activitieFields) {
        this.activitieFields = activitieFields;
    }

    @RuntimeType
    public Object process(@This Object obj, @SuperCall Callable<Object> call) throws Exception {
        if (!activated) {
            activitieFields.forEach(field -> {
                ActivityStub asc = field.getAnnotation(ActivityStub.class);
                Object was = Workflow.newActivityStub(field.getType(),
                        ActivityOptions.newBuilder().setScheduleToCloseTimeout(Duration.ofSeconds(asc.durationInSeconds())).build());
                try {
                    ReflectionUtils.makeAccessible(field);
                    field.set(obj, was);
                } catch (IllegalArgumentException | IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            });
            activated = true;
        }
        return call.call();
    }
}
