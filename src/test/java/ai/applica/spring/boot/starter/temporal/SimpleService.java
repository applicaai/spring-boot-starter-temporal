package ai.applica.spring.boot.starter.temporal;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface SimpleService {
    String  say(String sth);
}
