package ai.applica.spring.boot.starter.temporal;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
public class AutoConfigurationTest {

  @Autowired
  HelloWorkflowImpl impl;
  @Autowired
  HelloWorkflowImplTwo impl2;

  @Test
  public void workflowsProcessOk() {
    // try {
    //   Thread.sleep(120000);
    // } catch (InterruptedException e) {
    //   // TODO Auto-generated catch block
    //   e.printStackTrace();
    // }
    assertNotNull(impl.process());

    assertNotNull(impl2.process());

  }
}
