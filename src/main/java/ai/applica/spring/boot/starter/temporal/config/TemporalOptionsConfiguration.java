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

import io.temporal.activity.ActivityOptions;
import io.temporal.client.WorkflowClientOptions;
import io.temporal.client.WorkflowOptions;

public interface TemporalOptionsConfiguration {

  /**
   * Modify options or just pass them through.
   *
   * @param newBuilder
   * @return
   */
  WorkflowClientOptions.Builder modifyClientOptions(WorkflowClientOptions.Builder newBuilder);
  /**
   * Modify options or just pass them through.
   *
   * @param newBuilder
   * @return
   */
  WorkflowOptions.Builder modifyDefaultStubOptions(WorkflowOptions.Builder newBuilder);
  /**
   * Modify options or just pass them through.
   *
   * @param newBuilder
   * @return
   */
  ActivityOptions.Builder modifyDefaultActivityOptions(ActivityOptions.Builder newBuilder);
}
