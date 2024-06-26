/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.xml.conversion.json.model.sample.Sample;

public class ManifestReaderJsonTest {

  @Test
  public void testAlternativePunctuations() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("FIELD_NAME_1")
                .desc("some desc")
                .attributes(
                    new ManifestFieldDefinition.Builder()
                        .attribute()
                        .optional()
                        .name("ATT_NAME_1")
                        .desc("some desc")
                        .build())
                .and()
                .meta()
                .required()
                .name("FIELD_NAME_2")
                .desc("some desc")
                .attributes(
                    new ManifestFieldDefinition.Builder()
                        .attribute()
                        .optional()
                        .name("ATT_NAME_2")
                        .desc("some desc")
                        .build())
                .and()
                .meta()
                .required()
                .name("FIELD_NAME_3")
                .desc("some desc")
                .attributes(
                    new ManifestFieldDefinition.Builder()
                        .attribute()
                        .optional()
                        .name("ATT_NAME_3")
                        .desc("some desc")
                        .build())
                .build());

    manifestReader.readManifest(
        Paths.get("."),
        new ManifestBuilder()
            .jsonFormat()
            .field("fieldName1", "val1")
            .attribute("attName1", "attval1")
            .field("field_name_2", "val2")
            .attribute("att_name_2", "attval2")
            .field("field-name-3", "val3")
            .attribute("att-name-3", "attval3")
            .build());

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    Assert.assertEquals(fieldGroup.getField("FIELD_NAME_1").getValue(), "val1");
    Assert.assertEquals(
        fieldGroup.getField("FIELD_NAME_1").getAttributes().get(0).getValue(), "attval1");

    Assert.assertEquals(fieldGroup.getField("FIELD_NAME_2").getValue(), "val2");
    Assert.assertEquals(
        fieldGroup.getField("FIELD_NAME_2").getAttributes().get(0).getValue(), "attval2");

    Assert.assertEquals(fieldGroup.getField("FIELD_NAME_3").getValue(), "val3");
    Assert.assertEquals(
        fieldGroup.getField("FIELD_NAME_3").getAttributes().get(0).getValue(), "attval3");
  }

  @Test
  public void testMultiValueNoAttributes() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("field")
                .desc("some desc")
                .build());

    File manifestFile =
        new File(
            WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/manifests/json/valid/"),
            "multi-value-no-attributes.json");

    manifestReader.readManifest(Paths.get("."), manifestFile);

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field") && field.getValue().equals("val1"))
            .findFirst()
            .isPresent());

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field") && field.getValue().equals("val2"))
            .findFirst()
            .isPresent());
  }

  @Test
  public void testMultiValueFirstHavingAttributes() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("field")
                .desc("some desc")
                .attributes(
                    new ManifestFieldDefinition.Builder()
                        .attribute()
                        .optional()
                        .name("attField")
                        .desc("some desc")
                        .build())
                .build());

    File manifestFile =
        new File(
            WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/manifests/json/valid/"),
            "multi-value-first-attributes.json");

    manifestReader.readManifest(Paths.get("."), manifestFile);

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field") && field.getValue().equals("val1"))
            .findFirst()
            .isPresent());

    List<ManifestFieldValue> atts =
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field") && field.getValue().equals("val1"))
            .findFirst()
            .get()
            .getAttributes();

    Assert.assertTrue(
        atts.stream()
            .filter(att -> att.getName().equals("attField") && att.getValue().equals("attVal"))
            .findFirst()
            .isPresent());

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field") && field.getValue().equals("val2"))
            .findFirst()
            .isPresent());
  }

  @Test
  public void testSimpleValue() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("field1")
                .desc("some desc")
                .and()
                .meta()
                .required()
                .name("field2")
                .desc("some desc")
                .build());

    File manifestFile =
        new File(
            WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/manifests/json/valid/"),
            "simple-value.json");

    manifestReader.readManifest(Paths.get("."), manifestFile);

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field1") && field.getValue().equals("val1"))
            .findFirst()
            .isPresent());
  }

  @Test
  public void testObjectValue() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("field1")
                .desc("some desc")
                .and()
                .meta()
                .required()
                .name("field2")
                .desc("some desc")
                .build());

    File manifestFile =
        new File(
            WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/manifests/json/valid/"),
            "object-value.json");

    manifestReader.readManifest(Paths.get("."), manifestFile);

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field1") && field.getValue().equals("val1"))
            .findFirst()
            .isPresent());
  }

  @Test
  public void testSampleJsonValue() throws JsonProcessingException {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("sample")
                .desc("some desc")
                .build());

    File manifestFile =
        new File(
            WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/manifests/json/valid/"),
            "valid-sample.json");

    manifestReader.readManifest(Paths.get("."), manifestFile);

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    ManifestFieldValue sampleFieldValue =
        fieldGroup.stream().filter(field -> field.getName().equals("sample")).findFirst().get();
    Sample sample = new ObjectMapper().readValue(sampleFieldValue.getValue(), Sample.class);
    Assert.assertNotNull(sample);
  }

  /** A good example of all possible ways to format the fields. */
  @Test
  public void testAllValues() {
    TestManifestReader manifestReader =
        new TestManifestReader(
            new ManifestFieldDefinition.Builder()
                .meta()
                .required()
                .name("field1")
                .desc("some desc")
                .and()
                .meta()
                .required()
                .name("field2")
                .desc("some desc")
                .attributes(
                    new ManifestFieldDefinition.Builder()
                        .attribute()
                        .optional()
                        .name("attField1")
                        .desc("some desc")
                        .build())
                .attributes(
                    new ManifestFieldDefinition.Builder()
                        .attribute()
                        .optional()
                        .name("attField2")
                        .desc("some desc")
                        .build())
                .and()
                .meta()
                .required()
                .name("field3")
                .desc("some desc")
                .build());

    File manifestFile =
        new File(
            WebinCliTestUtils.getResourceDir("uk/ac/ebi/ena/webin/cli/manifests/json/valid/"),
            "all-possible-field-formats.json");

    manifestReader.readManifest(Paths.get("."), manifestFile);

    ManifestFieldGroup fieldGroup =
        manifestReader.getManifestReaderResult().getManifestFieldGroups().stream()
            .findFirst()
            .get();

    // field1
    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field1") && field.getValue().equals("val1"))
            .findFirst()
            .isPresent());

    // field2
    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field2") && field.getValue().equals("val21"))
            .findFirst()
            .isPresent());

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field2") && field.getValue().equals("val22"))
            .findFirst()
            .isPresent());

    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field2") && field.getValue().equals("val23"))
            .findFirst()
            .isPresent());

    // attributes
    List<ManifestFieldValue> atts =
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field2") && field.getValue().equals("val23"))
            .findFirst()
            .get()
            .getAttributes();

    Assert.assertTrue(
        atts.stream()
            .filter(att -> att.getName().equals("attField1") && att.getValue().equals("attVal11"))
            .findFirst()
            .isPresent());

    Assert.assertTrue(
        atts.stream()
            .filter(att -> att.getName().equals("attField1") && att.getValue().equals("attVal12"))
            .findFirst()
            .isPresent());

    Assert.assertTrue(
        atts.stream()
            .filter(att -> att.getName().equals("attField2") && att.getValue().equals("attVal21"))
            .findFirst()
            .isPresent());

    // field3
    Assert.assertTrue(
        fieldGroup.stream()
            .filter(field -> field.getName().equals("field3") && field.getValue().equals("val3"))
            .findFirst()
            .isPresent());
  }
}
