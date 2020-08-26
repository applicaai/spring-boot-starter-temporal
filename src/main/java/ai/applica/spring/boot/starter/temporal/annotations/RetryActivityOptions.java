package ai.applica.spring.boot.starter.temporal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.temporal.ChronoUnit;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD})
public @interface RetryActivityOptions {
  long initialInterval() default DEFAULT_INITIAL_INTERVAL;

  ChronoUnit initialIntervalUnits() default ChronoUnit.SECONDS;

  double backoffCoefficient() default DEFAULT_BACKOFF_COEFFICIENT;

  int maximumAttempts() default DEFAULT_MAXIMUM_ATTEMPTS;

  long maximumInterval() default DEFAULT_MAXIMUM_INTERVAL;

  ChronoUnit maximumIntervalUnits() default ChronoUnit.SECONDS;

  String[] doNotRetry() default {};

  long DEFAULT_INITIAL_INTERVAL = -1;

  double DEFAULT_BACKOFF_COEFFICIENT = -1.0;

  int DEFAULT_MAXIMUM_ATTEMPTS = -1;

  long DEFAULT_MAXIMUM_INTERVAL = -1;
}
