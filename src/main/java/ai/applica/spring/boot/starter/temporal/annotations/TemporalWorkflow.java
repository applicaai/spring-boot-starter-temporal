package ai.applica.spring.boot.starter.temporal.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that service is an appropriate temporal workflow implementation
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface TemporalWorkflow {
  /**
   * Link to workflow properties to be loaded from config
   */
  String value();
}
