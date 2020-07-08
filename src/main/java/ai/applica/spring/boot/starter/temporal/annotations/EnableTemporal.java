package ai.applica.spring.boot.starter.temporal.annotations;


import ai.applica.spring.boot.starter.temporal.config.TemporalBootstrapConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

/**
 * Indicates that temporal auto-configuration should be applied
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Import(TemporalBootstrapConfiguration.class)
public @interface EnableTemporal {
}
