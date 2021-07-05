/*
 *  Copyright (c) 2020 Applica.ai All Rights Reserved
 *
 *  Copyright 2012-2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"). You may not
 *  use this file except in compliance with the License. A copy of the License is
 *  located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 *  or in the "license" file accompanying this file. This file is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 *  express or implied. See the License for the specific language governing
 *  permissions and limitations under the License.
 */

package ai.applica.spring.boot.starter.temporal;

import static org.assertj.core.api.Assertions.assertThat;

import io.temporal.client.WorkflowOptions.Builder;
import org.springframework.beans.factory.annotation.Autowired;

// @RunWith(SpringRunner.class)
// @SpringBootTest()
public class AutoConfigurationTest {

  @Autowired WorkflowFactory fact;

  // @Test
  public void workflowsProcessOk() {
    // try {
    //   Thread.sleep(120000);
    // } catch (InterruptedException e) {
    //   // TODO Auto-generated catch block
    //   e.printStackTrace();
    // }
    HelloWorkflow impl = fact.makeStub(HelloWorkflow.class, HelloWorkflowImpl.class);
    assertThat(impl.process()).isNotNull();

    Builder options = fact.defaultOptionsBuilder(HelloWorkflowImplTwo.class);
    options.setWorkflowId("workflowId-1");
    HelloWorkflow impl2 = fact.makeStub(HelloWorkflow.class, options);
    assertThat(impl2.process()).isNotNull();
  }
}
