package ai.applica.spring.boot.starter.temporal;

public class InvalidOptionsModifierArgumentException extends RuntimeException {
  public InvalidOptionsModifierArgumentException(Class expectedArgumentType) {
    super(
        "Expected Class<"
            + expectedArgumentType.getSimpleName()
            + "> as first argument of method annotated with @ActivityOptionsModifier");
  }
}
