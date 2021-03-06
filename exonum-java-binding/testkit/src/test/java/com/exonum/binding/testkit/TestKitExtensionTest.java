/*
 * Copyright 2019 The Exonum Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.exonum.binding.testkit;

import static com.exonum.binding.testkit.TestKitTestUtils.ARTIFACT_FILENAME;
import static com.exonum.binding.testkit.TestKitTestUtils.ARTIFACT_ID;
import static com.exonum.binding.testkit.TestKitTestUtils.SERVICE_CONFIGURATION;
import static com.exonum.binding.testkit.TestKitTestUtils.SERVICE_ID;
import static com.exonum.binding.testkit.TestKitTestUtils.SERVICE_NAME;
import static com.exonum.binding.testkit.TestKitTestUtils.checkIfServiceEnabled;
import static com.exonum.binding.testkit.TestKitTestUtils.createTestServiceArtifact;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.testkit.engine.EngineExecutionResults;
import org.junit.platform.testkit.engine.EngineTestKit;
import org.junit.platform.testkit.engine.Event;
import org.junit.platform.testkit.engine.Events;

@SuppressWarnings("EmptyMethod")
class TestKitExtensionTest {

  @TempDir
  @SuppressWarnings("WeakerAccess") // @TempDir can't be private
  static Path artifactsDirectory;

  @BeforeAll
  static void setUp() throws IOException {
    createTestServiceArtifact(artifactsDirectory);
  }

  @Test
  void testKitInstantiationTestCase() {
    Events testEvents = getTestCaseEvents(TestKitInstantiationTestCase.class);

    assertThat(testEvents.executions().count()).isEqualTo(1);
    assertThat(testEvents.executions().succeeded().count()).isEqualTo(1);
  }

  @Test
  void beforeEachInstantiationTestCase() {
    Events testEvents = getAllTestCaseEvents(BeforeEachInstantiationTestCase.class);

    String expectedMessage = "TestKit was parameterized with annotations after being instantiated";
    checkFailedEvents(testEvents, expectedMessage);
  }

  @Test
  void afterEachInstantiationTestCase() {
    Events testEvents = getAllTestCaseEvents(AfterEachInstantiationTestCase.class);

    String expectedMessage = "TestKit was parameterized with annotations after being instantiated";
    checkFailedEvents(testEvents, expectedMessage);
  }

  @Test
  void beforeAllInstantiationTestCase() {
    Events testEvents = getAllTestCaseEvents(BeforeAllInstantiationTestCase.class);

    String expectedMessage = "TestKit can't be injected in @BeforeAll or @AfterAll";
    checkFailedEvents(testEvents, expectedMessage);
  }

  @Test
  void afterAllInstantiationTestCase() {
    Events testEvents = getAllTestCaseEvents(AfterAllInstantiationTestCase.class);

    String expectedMessage = "TestKit can't be injected in @BeforeAll or @AfterAll";
    checkFailedEvents(testEvents, expectedMessage);
  }

  private void checkFailedEvents(Events testEvents, String expectedMessage) {
    Events failedEvents = testEvents.failed();
    assertThat(failedEvents.count()).isEqualTo(1);
    String exceptionMessage = getFailedEventExceptionMessage(failedEvents.list().get(0));
    assertThat(exceptionMessage).contains(expectedMessage);
  }

  private String getFailedEventExceptionMessage(Event event) {
    return event.getPayload(TestExecutionResult.class)
        .flatMap(TestExecutionResult::getThrowable)
        .map(Throwable::getMessage)
        .orElseThrow();
  }

  private Events getTestCaseEvents(Class<?> testCaseClass) {
    return getTestCaseEngineExecutionResults(testCaseClass)
        .testEvents();
  }

  private Events getAllTestCaseEvents(Class<?> testCaseClass) {
    return getTestCaseEngineExecutionResults(testCaseClass)
        .allEvents();
  }

  private EngineExecutionResults getTestCaseEngineExecutionResults(Class<?> testCaseClass) {
    return EngineTestKit.engine("junit-jupiter")
        .selectors(selectClass(testCaseClass))
        .execute();
  }

  static class TestKitInstantiationTestCase {

    @RegisterExtension
    TestKitExtension testKitExtension = new TestKitExtension(TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory));

    static TestKit instantiatedTestKit;

    @BeforeEach
    void beforeEachTestKitInject(TestKit testKit) {
      // Creates TestKit based on default builder
      instantiatedTestKit = testKit;

      // Check that TestKit was instantiated with a correct service
      checkIfServiceEnabled(testKit, SERVICE_NAME, SERVICE_ID);

      // Check that main TestKit node is a validator
      assertThat(testKit.getEmulatedNode().getNodeType()).isEqualTo(EmulatedNodeType.VALIDATOR);
    }

    @Test
    void testDefaultTestKit(TestKit testKit) {
      assertThat(testKit).isEqualTo(instantiatedTestKit);
    }

    @AfterEach
    void afterEachTestKitInject(TestKit testKit) {
      assertThat(testKit).isEqualTo(instantiatedTestKit);
    }
  }

  static class BeforeEachInstantiationTestCase {

    @RegisterExtension
    TestKitExtension testKitExtension = new TestKitExtension(TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory));

    @BeforeEach
    void beforeEach(TestKit testKit) {
      assertNotNull(testKit);
    }

    @Test
    void testTestKitReconfiguration(@Auditor TestKit testKit) {
      fail("Shouldn't be executed");
    }
  }

  static class AfterEachInstantiationTestCase {

    @RegisterExtension
    TestKitExtension testKitExtension = new TestKitExtension(TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory));

    @AfterEach
    void afterEach(@ValidatorCount(8) TestKit testKit) {
      fail("Shouldn't be executed");
    }

    @Test
    void testTestKitReconfiguration(TestKit testKit) {
      assertNotNull(testKit);
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  static class BeforeAllInstantiationTestCase {

    @RegisterExtension
    TestKitExtension testKitExtension = new TestKitExtension(TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory));

    @BeforeAll
    void beforeAll(TestKit testKit) {
      // Should fail
    }

    @Test
    void test(TestKit testKit) {
      fail("Shouldn't be executed");
    }
  }

  @TestInstance(TestInstance.Lifecycle.PER_CLASS)
  static class AfterAllInstantiationTestCase {

    @RegisterExtension
    TestKitExtension testKitExtension = new TestKitExtension(TestKit.builder()
        .withDeployedArtifact(ARTIFACT_ID, ARTIFACT_FILENAME)
        .withService(ARTIFACT_ID, SERVICE_NAME, SERVICE_ID, SERVICE_CONFIGURATION)
        .withArtifactsDirectory(artifactsDirectory));

    @AfterAll
    void afterAll(TestKit testKit) {
      // Should fail
    }

    @Test
    @SuppressWarnings("squid:S2699")
    void test() {
      // Should pass
    }
  }
}
