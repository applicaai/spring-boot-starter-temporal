package ai.applica.spring.boot.starter.temporal;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface RemoteActivities {
  String echo(String word);
}
