
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
package uk.ac.ebi.ena.webin.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Modifier;

import org.junit.Test;

import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;

public class WebinCliMessageTest {

    @Test
    public void testFormat() {
        assertThat(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format()).isEqualTo(
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        assertThat(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.format("TEST")).isEqualTo(
                WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text.replace("{0}", "TEST"));
    }

    @Test
    public void testError() {
        ValidationMessage<Origin> validationMessage = WebinCliMessage.error(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR);
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(validationMessage.getOrigins()).hasSize(0);
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // Writer writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("ERROR: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + "\n");

        validationMessage = WebinCliMessage.error(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR, () -> "TEST");
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.ERROR);
        assertThat(validationMessage.getOrigins()).hasSize(1);
        assertThat(validationMessage.getOrigins().get(0).getOriginText()).isEqualTo("TEST");
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("ERROR: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + " [TEST]\n");
    }

    @Test
    public void testInfo() {
        ValidationMessage<Origin> validationMessage = WebinCliMessage.info(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR);
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(validationMessage.getOrigins()).hasSize(0);
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // Writer writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("INFO: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + "\n");

        validationMessage = WebinCliMessage.info(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR, () -> "TEST");
        assertThat(validationMessage.getSeverity()).isEqualTo(Severity.INFO);
        assertThat(validationMessage.getOrigins()).hasSize(1);
        assertThat(validationMessage.getOrigins().get(0).getOriginText()).isEqualTo("TEST");
        assertThat(validationMessage.getMessage()).isEqualTo(WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text);

        // writer = new StringWriter();
        // validationMessage.writeMessage(writer);
        // assertThat(writer.toString()).isEqualTo("INFO: " + WebinCliMessage.Service.SAMPLE_SERVICE_SYSTEM_ERROR.text + " [TEST]\n");
    }

    @Test
    public void testEnum() throws NoSuchFieldException {
        for (Class cls : WebinCliMessage.class.getClasses()){
            assertThat(cls.isEnum()).isTrue();
            assertThat(WebinCliMessage.class).isAssignableFrom(cls);
            assertThat(cls.getField("text").getType().equals(String.class));
            assertThat(Modifier.isPublic(cls.getField("text").getModifiers())).isTrue();
        }
    }
}
