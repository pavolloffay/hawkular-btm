/*
 * Copyright 2015-2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.hawkular.apm.tests.dockerized;

import java.io.IOException;
import java.util.List;

import org.hawkular.apm.tests.dockerized.ProjectVersion;
import org.hawkular.apm.tests.dockerized.TestScenarioRunner;
import org.hawkular.apm.tests.dockerized.TestScenariosFinder;
import org.hawkular.apm.tests.dockerized.model.TestScenario;
import org.junit.Test;

import com.spotify.docker.client.exceptions.DockerCertificateException;

/**
 * This test runs test scenarios defined in test/resources.
 *
 * @author Pavol Loffay
 */
public class InstrumentationTest {

    @Test
    public void testScenarios() throws IOException, DockerCertificateException {

        String testResourcesPath = getClass().getClassLoader().getResource(".").getPath();

        TestScenariosFinder testScenariosFinder = new TestScenariosFinder(testResourcesPath);
        List<TestScenario> testScenarios = testScenariosFinder.getScenarios();

        TestScenarioRunner caseRunner = new TestScenarioRunner(ProjectVersion.currentVersion(), 9080);

        int successfulScenarios = 0;

        for (TestScenario testScenario: testScenarios) {
            int successfulTests = caseRunner.run(testScenario);

            if (successfulTests == testScenario.enabledTests()) {
                System.out.println("\nScenario success: " + testScenario + "\n");
                successfulScenarios++;
            }
        }

        System.out.println("\n\nSuccessful scenarios: " + successfulScenarios +
                ", failed: " + (testScenarios.size() - successfulScenarios));

        caseRunner.close();
    }
}
