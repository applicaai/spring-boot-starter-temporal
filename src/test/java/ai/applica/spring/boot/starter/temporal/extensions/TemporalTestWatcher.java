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

package ai.applica.spring.boot.starter.temporal.extensions;

import io.temporal.testing.TestWorkflowEnvironment;
import lombok.Setter;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestWatcher;

/** Prints a history of the workflow under test in case of a test failure. */
public class TemporalTestWatcher implements TestWatcher {

  @Setter private TestWorkflowEnvironment environment;

  @Override
  public void testSuccessful(ExtensionContext context) {
    if (environment == null) {
      printErrorOnNullEnvironment();
    }
  }

  @Override
  public void testFailed(ExtensionContext context, Throwable cause) {
    if (environment != null) {
      System.err.println(environment.getDiagnostics());
    } else {
      printErrorOnNullEnvironment();
    }
  }

  private void printErrorOnNullEnvironment() {
    System.err.println("Environment not set in TemporalTestWatcher");
  }
}
