/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.message;

import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.message.ValidationMessage.Severity;
import uk.ac.ebi.ena.webin.cli.message.source.MessageFormatSource;

public class ValidationMessageTest {

    private static class TestMessage implements MessageFormatSource {
        private final String text;

        public TestMessage(String text) {
            this.text = text;
        }

        @Override
        public String text() {
            return text;
        }
    }

    @Test
    public void testFormat() {
        Assertions.assertThat(
                new TestMessage("Test {0}").format("TEST")).isEqualTo("Test TEST");
        Assertions.assertThat(
                new TestMessage("Test {0} {1}").format("TEST1", "TEST2")).isEqualTo("Test TEST1 TEST2");
    }

    @Test
    public void testRegex() {
        Assertions.assertThat(
                new TestMessage("Test {0}").regex()).isEqualTo("Test .*");
        Assertions.assertThat(
                new TestMessage("Test {0} {1}").regex()).isEqualTo("Test .* .*");
    }

    @Test
    public void testErrorWithValidationMessage() {
        ValidationMessage message = ValidationMessage.error(new TestMessage("Test"));
        assertThat(message.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");

        message = ValidationMessage.error(new TestMessage("Test {0}"), "TEST");
        assertThat(message.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test TEST");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");
    }

    @Test
    public void testErrorWithStringMessage() {
        ValidationMessage message = ValidationMessage.error("Test");
        assertThat(message.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");
    }

    @Test
    public void testErrorWithException() {
        Exception ex = new RuntimeException("Test");
        ValidationMessage message = ValidationMessage.error(ex);
        assertThat(message.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");
    }

    @Test
    public void testInfoWithValidationMessage() {
        ValidationMessage message = ValidationMessage.info(new TestMessage("Test"));
        assertThat(message.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");

        message = ValidationMessage.info(new TestMessage("Test {0}"), "TEST");
        assertThat(message.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test TEST");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");
    }

    @Test
    public void testInfoWithStringMessage() {
        ValidationMessage message = ValidationMessage.info("Test");
        assertThat(message.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(message.getOrigin()).hasSize(0);
        assertThat(message.getMessage()).isEqualTo("Test");
        message.appendOrigin(new ValidationOrigin("TEST", "TEST"));
        assertThat(message.getOrigin()).hasSize(1);
        assertThat(message.getOrigin().get(0)).hasToString("TEST: TEST");
    }
}