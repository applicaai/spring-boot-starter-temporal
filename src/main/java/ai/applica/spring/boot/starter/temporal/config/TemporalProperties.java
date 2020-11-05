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

package ai.applica.spring.boot.starter.temporal.config;

import java.util.Map;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@NoArgsConstructor
@ConfigurationProperties(prefix = "spring.temporal")
public class TemporalProperties {

  private String host;

  private Integer port;

  private Boolean useSsl;

  private WorkflowOption workflowDefaults;

  private Map<String, WorkflowOption> workflows;

  private ActivityStubOptions activityStubDefaults;

  private Map<String, ActivityStubOptions> activityStubs;

  private boolean addedDefaultsToWorkflows = false;

  @Data
  @NoArgsConstructor
  public static class WorkflowOption {

    private String taskQueue;

    private Long executionTimeout;

    private String executionTimeoutUnit;

    private Integer activityPoolSize;

    private Integer workflowPoolSize;
  }

  @Data
  @NoArgsConstructor
  public static class ActivityStubOptions {

    private Long scheduleToCloseTimeout;

    private String scheduleToCloseTimeoutUnit;
  }

  public Map<String, WorkflowOption> getWorkflows() {
    if (!addedDefaultsToWorkflows) {
      synchronized (this) {
        workflows.forEach(
            (key, value) -> {
              if (value.getExecutionTimeout() == null) {
                value.setExecutionTimeout(workflowDefaults.getExecutionTimeout());
              }
              if (value.getExecutionTimeoutUnit() == null) {
                value.setExecutionTimeoutUnit(workflowDefaults.getExecutionTimeoutUnit());
              }
              if (value.getActivityPoolSize() == null) {
                value.setActivityPoolSize(workflowDefaults.getActivityPoolSize());
              }
              if (value.getWorkflowPoolSize() == null) {
                value.setWorkflowPoolSize(workflowDefaults.getWorkflowPoolSize());
              }
              addedDefaultsToWorkflows = true;
            });
      }
    }
    return workflows;
  }
}
