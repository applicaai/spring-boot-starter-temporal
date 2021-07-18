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

import java.time.Duration;
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

  private boolean createWorkers = true;

  private WorkflowOption workflowDefaults;

  private WorkflowOption activityWorkerDefaults;

  private Map<String, WorkflowOption> workflows;

  private Map<String, WorkflowOption> activityWorkers;

  private ActivityStubOptions activityStubDefaults;

  private Map<String, ActivityStubOptions> activityStubs;

  private WorkflowServiceStubOptions workflowServiceStubOptions;

  private boolean addedDefaultsToWorkflows = false;

  private boolean addedDefaultsToActivities = false;

  @Data
  @NoArgsConstructor
  public static class WorkflowOption {

    private String taskQueue;

    private Long executionTimeout;

    private String executionTimeoutUnit;

    private Integer activityPoolSize;

    private Integer workflowPoolSize;

    private Integer activityPollThreadPoolSize;

    private Integer workflowPollThreadPoolSize;
  }

  /**
   * Options passed to WorkflowServiceStubsOptions Please notice, not all of the options are exposed
   * here
   */
  @Data
  @NoArgsConstructor
  public static class WorkflowServiceStubOptions {
    private Boolean disableHealthCheck;
    private Duration healthCheckAttemptTimeout;
    private Duration healthCheckTimeout;
    private Boolean enableKeepAlive;
    private Duration keepAliveTime;
    private Duration keepAliveTimeout;
    private Boolean keepAlivePermitWithoutStream;
    private Duration rpcLongPollTimeout;
    private Duration rpcQueryTimeout;
    private Duration rpcTimeout;
    private Duration connectionBackoffResetFrequency;
    private Duration grpcReconnectFrequency;
  }

  @Data
  @NoArgsConstructor
  public static class ActivityStubOptions {
    private String taskQueue;
    private Duration scheduleToCloseTimeout;
    private Duration scheduleToStartTimeout;
    private Duration startToCloseTimeout;
    private Duration heartbeatTimeout;
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
              if (value.getWorkflowPollThreadPoolSize() == null) {
                value.setWorkflowPollThreadPoolSize(
                    workflowDefaults.getWorkflowPollThreadPoolSize());
              }
              if (value.getActivityPollThreadPoolSize() == null) {
                value.setActivityPollThreadPoolSize(
                    workflowDefaults.getActivityPollThreadPoolSize());
              }
              addedDefaultsToWorkflows = true;
            });
      }
    }
    return workflows;
  }

  public Map<String, WorkflowOption> getActivityWorkers() {
    if (!addedDefaultsToActivities) {
      synchronized (this) {
        activityWorkers.forEach(
            (key, value) -> {
              if (value.getExecutionTimeout() == null) {
                value.setExecutionTimeout(activityWorkerDefaults.getExecutionTimeout());
              }
              if (value.getExecutionTimeoutUnit() == null) {
                value.setExecutionTimeoutUnit(activityWorkerDefaults.getExecutionTimeoutUnit());
              }
              if (value.getActivityPoolSize() == null) {
                value.setActivityPoolSize(activityWorkerDefaults.getActivityPoolSize());
              }
              if (value.getWorkflowPoolSize() == null) {
                value.setWorkflowPoolSize(activityWorkerDefaults.getWorkflowPoolSize());
              }
              if (value.getWorkflowPollThreadPoolSize() == null) {
                value.setWorkflowPollThreadPoolSize(workflowDefaults.getWorkflowPollThreadPoolSize());
              }
              if (value.getActivityPollThreadPoolSize() == null) {
                value.setActivityPollThreadPoolSize(workflowDefaults.getActivityPollThreadPoolSize());
              }
              addedDefaultsToActivities = true;
            });
      }
    }
    return activityWorkers;
  }
}
