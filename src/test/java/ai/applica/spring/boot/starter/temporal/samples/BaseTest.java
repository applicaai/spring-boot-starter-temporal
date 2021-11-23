package ai.applica.spring.boot.starter.temporal.samples;

import ai.applica.spring.boot.starter.temporal.annotations.TemporalTest;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;

@TemporalTest
@SpringBootTest(classes = TestApplicationConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
// @TestMethodOrder(MethodOrderer.MethodName.class)
public abstract class BaseTest {}
