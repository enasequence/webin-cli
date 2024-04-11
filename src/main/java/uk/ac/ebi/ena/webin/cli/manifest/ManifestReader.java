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

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReader.Fields.INFO;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReader.ManifestReaderState.State.PARSE;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReader.ManifestReaderState.State.VALIDATE;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationOrigin;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageListener;

public abstract class ManifestReader<M extends Manifest> {

  public static final String KEY_VALUE_COMMENT_REGEX = "^[\\s]*(#|;|\\/\\/).*$";

  public abstract Collection<M> getManifests();

  protected abstract M createManifest();

  public interface Fields {
    String NAME = "NAME";
    String INFO = "INFO";
    String SUBMISSION_TOOL = "SUBMISSION_TOOL";
    String SUBMISSION_TOOL_VERSION = "SUBMISSION_TOOL_VERSION";
  }

  public interface Descriptions {
    String INFO = "Info file";
    String SUBMISSION_TOOL = "Name of third-party or developed tool used to submit to ENA";
    String SUBMISSION_TOOL_VERSION =
        "Version number of the third-party or developed tool used to submit to ENA";
  }

  private final List<ManifestFieldDefinition> infoFields =
      new ManifestFieldDefinition.Builder()
          .file()
          .optional()
          .name(Fields.INFO)
          .desc(Descriptions.INFO)
          .build();

  static class ManifestReaderState {
    ManifestReaderState(Path inputDir, String fileName) {
      this.inputDir = inputDir;
      this.fileName = fileName;
    }

    enum State {
      INIT,
      PARSE,
      VALIDATE
    }

    State state = State.INIT;
    final Path inputDir;
    String fileName;
    int lineNo = 0;
  }

  private final WebinCliParameters webinCliParameters;
  private final List<ManifestFieldDefinition> fieldDefinitions;
  private final List<ManifestFileGroup> fileGroups;
  private final List<MessageListener> listener = new ArrayList<>();

  /** The value of manifest field 'NAME' is used as a key in this map. This field is mandatory */
  protected final Map<String, M> nameFieldToManifestMap = new HashMap<>();

  private ManifestReaderResult manifestReaderResult;

  private ManifestReaderState state;

  public ManifestReader(
      WebinCliParameters webinCliParameters, List<ManifestFieldDefinition> fieldDefinitions) {
    this.webinCliParameters = webinCliParameters;
    this.fieldDefinitions = fieldDefinitions;
    this.fileGroups = null;
  }

  public ManifestReader(
      WebinCliParameters webinCliParameters,
      List<ManifestFieldDefinition> fieldDefinitions,
      List<ManifestFileGroup> fileGroups) {
    this.webinCliParameters = webinCliParameters;
    this.fieldDefinitions = fieldDefinitions;
    this.fileGroups = fileGroups;
  }

  public final Path getInputDir() {
    return state.inputDir;
  }

  public List<ManifestFieldDefinition> getFieldDefinitions() {
    return fieldDefinitions;
  }

  public List<ManifestFileGroup> getFileGroups() {
    return fileGroups;
  }

  public void addListener(MessageListener listener) {
    this.listener.add(listener);
  }

  public final ManifestReaderResult getManifestReaderResult() {
    return manifestReaderResult;
  }

  public final ValidationResult getValidationResult() {
    return manifestReaderResult.getValidationResult();
  }

  public final void readManifest(Path inputDir, File manifestFile) {
    readManifest(inputDir, manifestFile, null);
  }

  public final void readManifest(Path inputDir, File manifestFile, File reportFile) {
    state = new ManifestReaderState(inputDir, manifestFile.getPath());

    ValidationOrigin origin = new ValidationOrigin("manifest file", state.fileName);
    ValidationResult result = new ValidationResult(reportFile, origin);

    // used for testing purpose
    listener.forEach(l -> result.add(l));

    manifestReaderResult = new ManifestReaderResult(result);

    List<String> manifestLines;
    try {
      manifestLines = Files.readAllLines(manifestFile.toPath());
    } catch (IOException ex) {
      error(WebinCliMessage.MANIFEST_READER_MANIFEST_FILE_READ_ERROR, manifestFile.getPath());
      return;
    }

    // A manifest file is essentially a list of field groups. Parsing step reads all such field
    // groups from the
    // file and puts them inside a collection.
    Collection<ManifestFieldGroup> parsedFieldGroups = parseManifest(inputDir, manifestLines);

    manifestReaderResult.getManifestFieldGroups().addAll(parsedFieldGroups);

    expandInfoFields(inputDir);

    validateFields();

    processManifest();
  }

  public WebinCliParameters getWebinCliParameters() {
    return webinCliParameters;
  }

  /**
   * Get the original group of manifest file fields that were used to create the given manifest
   * object.
   */
  public ManifestFieldGroup getManifestFieldGroup(Manifest manifest) {
    String name = manifest.getName();

    return manifestReaderResult.getManifestFieldGroups().stream()
        .filter(fieldGroup -> fieldGroup.getField(Fields.NAME).getValue().equals(name))
        .findFirst()
        .orElse(null);
  }

  private Collection<ManifestFieldGroup> parseManifest(Path inputDir, List<String> lines) {
    if (isJsonBasedFormat(lines)) {
      return parseJsonManifest(inputDir, lines);
    } else {
      return Arrays.asList(parseKeyValueManifest(inputDir, lines));
    }
  }

  private boolean isJsonBasedFormat(List<String> lines) {
    if (lines.isEmpty()) {
      return false;
    }

    return lines.get(0).trim().startsWith("{") || lines.get(0).trim().startsWith("[");
  }

  /**
   * Key/Value manifest file does not support multiple field groups. This format only contains field
   * names and their values. Therefore, it will be assumed that all these fields belong to a single
   * field group.
   */
  private ManifestFieldGroup parseKeyValueManifest(Path inputDir, List<String> lines) {
    state.state = PARSE;

    ManifestFieldGroup fieldGroup = new ManifestFieldGroup();

    for (String line : lines) {
      ManifestFieldValue field = parseManifestLine(inputDir, line);
      if (null != field) {
        fieldGroup.add(field);
      }
    }

    return fieldGroup;
  }

  private ManifestFieldValue parseManifestLine(Path inputDir, String line) {

    if (line == null) return null;

    ++state.lineNo;

    line = line.trim();

    if (line.isEmpty()) return null;

    String[] tokens = line.split("\\s+", 2);

    String fieldName = StringUtils.stripEnd(tokens[0].trim().toUpperCase(), ": ");
    String fieldValue = (tokens.length == 2) ? tokens[1].trim() : null;

    if (fieldName.matches(KEY_VALUE_COMMENT_REGEX)) // Ignore comment lines.
    return null;

    try {
      ManifestFieldDefinition fieldDefinition =
          Stream.concat(infoFields.stream(), fieldDefinitions.stream())
              .filter(
                  field ->
                      matchNameCaseAndPunctuationInsensitively(field.getName(), fieldName)
                          || field.matchSynonym(fieldName))
              .findFirst()
              .get();

      if (fieldValue != null) {
        ManifestFieldValue field =
            new ManifestFieldValue(
                fieldDefinition,
                fieldValue,
                new ArrayList<>(), // attributes are not supported in the old manifest format.
                new ValidationOrigin("line number", state.lineNo));

        getValidationResult().create(field.getOrigin());

        if (field.getDefinition().getType() == ManifestFieldType.FILE) {
          // Validate file exists.
          validateFileExists(inputDir, field);
        }

        return field;
      }

    } catch (NoSuchElementException ex) {
      error(WebinCliMessage.MANIFEST_READER_UNKNOWN_FIELD_ERROR, fieldName);
    }

    return null;
  }

  private Collection<ManifestFieldGroup> parseJsonManifest(Path inputDir, List<String> lines) {
    state.state = PARSE;

    List<ManifestFieldGroup> result = new ArrayList<>();

    try {
      ObjectMapper objectMapper = new ObjectMapper();

      JsonNode manifestJson =
          objectMapper.readTree(lines.stream().collect(Collectors.joining("\n")));

      if (manifestJson.isArray()) {
        manifestJson
            .elements()
            .forEachRemaining(
                fieldGroupJson -> {
                  ManifestFieldGroup fieldGroup = new ManifestFieldGroup();
                  loadManifestFields(inputDir, fieldGroupJson, fieldGroup);

                  result.add(fieldGroup);
                });
      } else {
        ManifestFieldGroup fieldGroup = new ManifestFieldGroup();
        loadManifestFields(inputDir, manifestJson, fieldGroup);

        result.add(fieldGroup);
      }
    } catch (IOException e) {
      error(WebinCliMessage.MANIFEST_READER_MANIFEST_FILE_MALFORMED);
    }

    return result;
  }

  /**
   * @param inputDir
   * @param manifestJson - The JSON object the fields will be read from.
   * @param fieldGroup - The collection all the fields will be loaded into.
   */
  private void loadManifestFields(
      Path inputDir, JsonNode manifestJson, ManifestFieldGroup fieldGroup) {
    manifestJson
        .fields()
        .forEachRemaining(
            manifestField -> {
              String fieldName = manifestField.getKey();

              // find field's definition
              ManifestFieldDefinition fieldDefinition = getFieldDefinition(fieldName);
              if (fieldDefinition == null) {
                error(WebinCliMessage.MANIFEST_READER_UNKNOWN_FIELD_ERROR, fieldName);
                return;
              }

              JsonNode fieldValue = manifestField.getValue();
              if (fieldValue.isValueNode()) {
                addManifestField(
                    inputDir, fieldDefinition, fieldValue.asText(), new ArrayList<>(), fieldGroup);
              } else if (fieldValue.isArray()) {
                // This case means the field itself contains more fields i.e. sub-fields.
                fieldValue
                    .elements()
                    .forEachRemaining(
                        subField -> {
                          if (subField.isValueNode()) {
                            addManifestField(
                                inputDir,
                                fieldDefinition,
                                subField.asText(),
                                new ArrayList<>(),
                                fieldGroup);
                          } else {
                            // If the sub-field is not a simple value field then it must be an
                            // object field
                            // that may have attributes too.
                            handleFieldWithAttributes(
                                inputDir, fieldDefinition, subField, fieldGroup);
                          }
                        });
              } else {
                // This is an exceptional case to handle sample JSONs. It is not ideal to handle
                // fields this way.
                // This is discouraged and an alternative method should be used in future for such
                // fields.
                if (fieldName.equalsIgnoreCase("sample")) {
                  addManifestField(
                      inputDir,
                      fieldDefinition,
                      fieldValue.toString(),
                      new ArrayList<>(),
                      fieldGroup);
                } else {
                  // If the field is neither a simple value field nor an array then it must be an
                  // object field
                  // that may have attributes too.
                  handleFieldWithAttributes(inputDir, fieldDefinition, fieldValue, fieldGroup);
                }
              }
            });
  }

  private ManifestFieldDefinition getFieldDefinition(String fieldName) {
    return Stream.concat(infoFields.stream(), fieldDefinitions.stream())
        .filter(
            fieldDef ->
                matchNameCaseAndPunctuationInsensitively(fieldDef.getName(), fieldName)
                    || fieldDef.matchSynonym(fieldName))
        .findFirst()
        .orElse(null);
  }

  private void addManifestField(
      Path inputDir,
      ManifestFieldDefinition fieldDefinition,
      String fieldValue,
      List<ManifestFieldValue> fieldAttributes,
      ManifestFieldGroup fieldGroup) {

    if (fieldValue == null) {
      return;
    }

    ManifestFieldValue manifestField =
        new ManifestFieldValue(
            fieldDefinition,
            fieldValue,
            fieldAttributes,
            new ValidationOrigin("file name", state.fileName));

    getValidationResult().create(manifestField.getOrigin());

    if (manifestField.getDefinition().getType() == ManifestFieldType.FILE) {
      validateFileExists(inputDir, manifestField);
    }

    fieldGroup.add(manifestField);
  }

  private void handleFieldWithAttributes(
      Path inputDir,
      ManifestFieldDefinition fieldDefinition,
      JsonNode field,
      ManifestFieldGroup fieldGroup) {
    List<ManifestFieldValue> fieldAttributes = new ArrayList<>();

    // Check that the field has any attributes and handle them first if it does.
    // Just having attributes is not enough to load them. Fields must have listed down all the
    // supported attributes
    // in their definitions beforehand.
    if (field.has("attributes") && !fieldDefinition.getFieldAttributes().isEmpty()) {
      field
          .get("attributes")
          .fields()
          .forEachRemaining(
              fieldAttribute -> {
                String attName = fieldAttribute.getKey();

                // find attribute's definition.
                ManifestFieldDefinition attDef =
                    fieldDefinition.getFieldAttributes().stream()
                        .filter(
                            attFieldDef ->
                                matchNameCaseAndPunctuationInsensitively(
                                        attFieldDef.getName(), attName)
                                    || attFieldDef.matchSynonym(attName))
                        .findFirst()
                        .orElse(null);
                if (attDef == null) {
                  error(WebinCliMessage.MANIFEST_READER_UNKNOWN_ATTRIBUTE_FIELD_ERROR, attName);
                  return;
                }

                JsonNode att = fieldAttribute.getValue();

                if (att.isArray()) {
                  att.elements()
                      .forEachRemaining(
                          textElement -> {
                            fieldAttributes.add(
                                new ManifestFieldValue(
                                    attDef,
                                    textElement.asText(),
                                    new ArrayList<>(),
                                    new ValidationOrigin("file name", state.fileName)));
                          });
                } else {
                  fieldAttributes.add(
                      new ManifestFieldValue(
                          attDef,
                          att.asText(),
                          new ArrayList<>(),
                          new ValidationOrigin("file name", state.fileName)));
                }
              });
    }

    // Finally, tend to the actual value of the field.
    addManifestField(
        inputDir, fieldDefinition, field.get("value").asText(), fieldAttributes, fieldGroup);
  }

  private void expandInfoFields(Path inputDir) {
    manifestReaderResult.getManifestFieldGroups().stream()
        .forEach(
            fieldGroup -> {
              // Find all the info fields.
              List<ManifestFieldValue> infoFields =
                  fieldGroup.stream()
                      .filter(field -> field.getName().equalsIgnoreCase(INFO))
                      .collect(Collectors.toList());

              // Parse the manifest files the info fields point to.
              for (ManifestFieldValue infoField : infoFields) {
                List<String> infoLines;
                File infoFile = new File(infoField.getValue());
                try {
                  infoLines = readAllLines(infoFile);
                } catch (IOException ex) {
                  error(WebinCliMessage.MANIFEST_READER_INFO_FILE_READ_ERROR, infoFile.getPath());
                  return;
                }

                String savedManifestFileName = state.fileName;
                int savedManifestLineNo = state.lineNo;

                try {
                  state.fileName = infoFile.getPath();
                  state.lineNo = 0;

                  // An info manifest file only contains one field group.
                  ManifestFieldGroup parsedFieldGroup =
                      parseManifest(inputDir, infoLines).stream().findFirst().get();

                  fieldGroup.addAll(parsedFieldGroup);
                } finally {
                  state.fileName = savedManifestFileName;
                  state.lineNo = savedManifestLineNo;
                }
              }

              // As the info fields have been processed, they are no longer needed.
              List<ManifestFieldValue> withoutInfo =
                  fieldGroup.stream()
                      .filter(field -> !field.getName().equalsIgnoreCase(INFO))
                      .collect(Collectors.toList());

              fieldGroup.clear();
              fieldGroup.addAll(withoutInfo);
            });
  }

  private void validateFields() {
    state.state = VALIDATE;

    // Ensure NAME fields in all the field groups are unique.
    validateUniqueFieldGroupsNames();

    // Validate min count.
    fieldDefinitions.stream()
        .filter(field -> field.getMinCount() > 0)
        .forEach(
            minCountField -> {
              for (ManifestFieldGroup fieldGroup : manifestReaderResult.getManifestFieldGroups()) {
                if (fieldGroup.stream()
                        .filter(field -> field.getName().equals(minCountField.getName()))
                        .count()
                    < 1) {
                  error(
                      WebinCliMessage.MANIFEST_READER_MISSING_MANDATORY_FIELD_ERROR,
                      minCountField.getName());
                }
              }
            });

    // Validate max count.
    fieldDefinitions.stream()
        .filter(field -> field.getMaxCount() > 0)
        .forEach(
            maxCountField -> {
              for (ManifestFieldGroup fieldGroup : manifestReaderResult.getManifestFieldGroups()) {
                List<ManifestFieldValue> matchingFields =
                    fieldGroup.stream()
                        .filter(field -> field.getName().equals(maxCountField.getName()))
                        .collect(Collectors.toList());

                if (matchingFields.size() > maxCountField.getMaxCount()) {
                  error(
                      WebinCliMessage.MANIFEST_READER_TOO_MANY_FIELDS_ERROR,
                      maxCountField.getName(),
                      String.valueOf(maxCountField.getMaxCount()));
                }
              }
            });

    // Validate/fix fields and run their processors.
    manifestReaderResult
        .getManifestFieldGroups()
        .forEach(
            fieldGroup -> {
              for (ManifestFieldValue fieldValue : fieldGroup) {
                ManifestFieldDefinition fieldDefinition = fieldValue.getDefinition();

                for (ManifestFieldProcessor processor : fieldDefinition.getFieldProcessors()) {
                  ValidationResult result = getValidationResult().create(fieldValue.getOrigin());
                  processor.process(result, fieldGroup, fieldValue);
                  fieldValue.setValidFieldValueOrFileSuffix(result.isValid());
                }

                // iterate over field attributes and run their processors.
                for (ManifestFieldValue att : fieldValue.getAttributes()) {
                  ManifestFieldDefinition attDef = att.getDefinition();

                  for (ManifestFieldProcessor attProcessor : attDef.getFieldProcessors()) {
                    ValidationResult result = getValidationResult().create(att.getOrigin());
                    attProcessor.process(result, fieldGroup, att);
                    att.setValidFieldValueOrFileSuffix(result.isValid());
                  }
                }
              }
            });

    // Validate file count.
    validateFileCount();

    // Ensure that all file names are unique.
    validateUniqueFileNames();
  }

  /**
   * Objects that override this method take all the parsed and validated field groups and create
   * manifest objects using them. As one field group maps to a single manifest object, there should
   * be as many manifest objects as there are number of field groups.
   */
  protected abstract void processManifest();

  private void validateFileExists(Path inputDir, ManifestFieldValue field) {
    ValidationResult result = getValidationResult().create(field.getOrigin());

    String fieldValue = field.getValue();

    try {
      if (Files.isReadable(Paths.get(fieldValue)) && !Files.isDirectory(Paths.get(fieldValue))) {
        // File is readable relative to default working dir.
      } else if (Files.isReadable(inputDir.resolve(Paths.get(fieldValue)))
          && !Files.isDirectory(inputDir.resolve(Paths.get(fieldValue)))) {
        // File is readable relative to defined input dir.
        field.setValue(inputDir.resolve(Paths.get(fieldValue)).toString());
      } else {
        error(result, WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR, fieldValue);
        return;
      }

      validateFileCompression(result, field.getValue());
    } catch (Throwable ex) {
      error(result, WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR, fieldValue);
    }
  }

  private void validateUniqueFieldGroupsNames() {
    // No need to validate if there is only one field group.
    if (manifestReaderResult.getManifestFieldGroups().size() == 1) {
      return;
    }

    Map<String, Long> nameFieldOccurrence =
        manifestReaderResult.getManifestFieldGroups().stream()
            .map(fieldGroup -> fieldGroup.getField(Fields.NAME).getValue())
            .collect(Collectors.groupingBy(name -> name, Collectors.counting()));

    for (Map.Entry<String, Long> entry : nameFieldOccurrence.entrySet()) {
      if (entry.getValue() > 1) {
        error(WebinCliMessage.MANIFEST_READER_NON_UNIQUE_FIELD_GROUP_NAME, entry.getKey());
        return;
      }
    }
  }

  private void validateFileCount() {
    if (fileGroups == null || fileGroups.isEmpty()) return;

    for (ManifestFieldGroup fieldGroup : manifestReaderResult.getManifestFieldGroups()) {
      if (!validateFileCountFor(fieldGroup)) {
        break;
      }
    }
  }

  /** @return 'true' if validation was successfull. 'false' if it was not. */
  private boolean validateFileCountFor(ManifestFieldGroup fieldGroup) {
    // E.g. FASTQ -> 2, BAM -> 1 etc
    Map<String, Long> fileTypeToCountMap =
        fieldGroup.stream()
            .filter(field -> field.getDefinition().getType().equals(ManifestFieldType.FILE))
            .collect(Collectors.groupingBy(ManifestFieldValue::getName, Collectors.counting()));

    if (fileTypeToCountMap == null || fileTypeToCountMap.isEmpty()) {
      error(WebinCliMessage.MANIFEST_READER_NO_DATA_FILES_ERROR, getFileGroupText(fileGroups));
      return false;
    }

    next:
    for (ManifestFileGroup fileGroup : fileGroups) {
      for (ManifestFileCount fileCountInfo : fileGroup.getFileCounts()) {
        Integer expectedMinCount = fileCountInfo.getMinCount();
        Integer expectedMaxCount = fileCountInfo.getMaxCount();

        Long actualFileCount = fileTypeToCountMap.get(fileCountInfo.getFileType());
        if (actualFileCount == null) {
          if (expectedMinCount > 0) {
            continue next; // Invalid because min is > 0.
          }
        } else {
          if (actualFileCount < expectedMinCount
              || (expectedMaxCount != null && actualFileCount > expectedMaxCount)) {
            continue next; // Invalid because larger than max or smaller than min.
          }
        }
      }

      for (String actualFileType : fileTypeToCountMap.keySet()) {
        List<ManifestFileCount> fileCountInfoList = fileGroup.getFileCounts();

        boolean fileTypeMatchFound =
            fileCountInfoList.stream()
                .filter(
                    fileCountInfo -> {
                      String expectedFileType = fileCountInfo.getFileType();

                      return expectedFileType.equals(actualFileType);
                    })
                .findFirst()
                .isPresent();

        if (!fileTypeMatchFound) {
          continue next; // Invalid because unmatched file type.
        }
      }

      return true; // valid.
    }

    error(
        WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR, getFileGroupText(fileGroups), "");

    return false;
  }

  private void validateUniqueFileNames() {
    if (fileGroups == null || fileGroups.isEmpty()) {
      return;
    }

    for (ManifestFieldGroup fieldGroup : manifestReaderResult.getManifestFieldGroups()) {
      List<ManifestFieldValue> fileFields =
          fieldGroup.stream()
              .filter(field -> field.getDefinition().getType().equals(ManifestFieldType.FILE))
              .collect(Collectors.toList());

      HashSet<String> fileNameSet = new HashSet<>(fileFields.size());

      for (ManifestFieldValue fileField : fileFields) {
        if (!fileNameSet.add(Paths.get(fileField.getValue()).getFileName().toString())) {
          error(WebinCliMessage.MANIFEST_READER_INVALID_FILE_NON_UNIQUE_NAMES);
          return;
        }
      }
    }
  }

  private void validateFileCompression(ValidationResult result, String filePath) {
    if (filePath.endsWith(ManifestFileSuffix.GZIP_FILE_SUFFIX)) {
      try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(filePath))) {
      } catch (Exception e) {
        error(
            result,
            WebinCliMessage.MANIFEST_READER_INVALID_FILE_COMPRESSION_ERROR,
            filePath,
            "gzip");
      }
    } else if (filePath.endsWith(ManifestFileSuffix.BZIP2_FILE_SUFFIX)) {
      try (BZip2CompressorInputStream bz2 =
          new BZip2CompressorInputStream(new FileInputStream(filePath))) {
      } catch (Exception e) {
        error(
            result,
            WebinCliMessage.MANIFEST_READER_INVALID_FILE_COMPRESSION_ERROR,
            filePath,
            "bzip2");
      }
    }
  }

  protected final Integer getAndValidatePositiveInteger(ManifestFieldValue field) {
    if (field == null) {
      return null;
    }
    String fieldValue = field.getValue();
    if (fieldValue == null) {
      return null;
    }

    ValidationResult result = getValidationResult().create(field.getOrigin());

    try {
      int value = Integer.valueOf(fieldValue);
      if (value <= 0) {
        error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_INTEGER_ERROR);
        return null;
      }
      return value;
    } catch (NumberFormatException nfe) {
      error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_INTEGER_ERROR);
    }

    return null;
  }

  protected final Float getAndValidatePositiveFloat(ManifestFieldValue field) {
    if (field == null) {
      return null;
    }
    String fieldValue = field.getValue();
    if (fieldValue == null) {
      return null;
    }

    ValidationResult result = getValidationResult().create(field.getOrigin());

    try {
      float value = Float.valueOf(fieldValue);
      if (value <= 0) {
        error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_FLOAT_ERROR);
        return null;
      }
      return value;
    } catch (NumberFormatException nfe) {
      error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_FLOAT_ERROR);
    }

    return null;
  }

  protected final Boolean getAndValidateBoolean(ManifestFieldValue field) {
    if (field == null) {
      return null;
    }
    String fieldValue = field.getValue();
    if (fieldValue == null) {
      return null;
    }

    switch (field.getValue().toUpperCase()) {
      case "YES":
      case "TRUE":
      case "Y":
        return true;
      case "NO":
      case "FALSE":
      case "N":
        return false;
    }

    return null;
  }

  private static List<String> readAllLines(InputStream is) {
    return new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))
        .lines()
        .collect(Collectors.toList());
  }

  private static List<String> readAllLines(File file) throws FileNotFoundException {
    try (InputStream is = new GZIPInputStream(new FileInputStream(file))) {
      return readAllLines(is);
    } catch (IOException ioe) {
      try (InputStream is = new BZip2CompressorInputStream(new FileInputStream(file))) {
        return readAllLines(is);
      } catch (IOException ie) {
        return readAllLines(new FileInputStream(file));
      }
    }
  }

  public static String getFileGroupText(List<ManifestFileGroup> fileGroups) {
    return fileGroups.stream()
        .map(
            fileGroup -> {
              StringBuilder str = new StringBuilder();
              str.append("[");
              String fileSeparator = "";
              for (ManifestFileCount fileCount : fileGroup.getFileCounts()) {
                String fileType = fileCount.getFileType();

                str.append(fileSeparator);
                if (fileCount.getMaxCount() != null
                    && fileCount.getMinCount() == fileCount.getMaxCount()) {
                  str.append(fileCount.getMinCount());
                } else {
                  if (fileCount.getMaxCount() != null) {
                    str.append(fileCount.getMinCount());
                    str.append("-");
                    str.append(fileCount.getMaxCount());
                  } else {
                    str.append(">=");
                    str.append(fileCount.getMinCount());
                  }
                }
                str.append(" ");
                str.append(fileType);
                fileSeparator = ", ";
              }
              str.append("]");
              return str.toString();
            })
        .collect(Collectors.joining(" or "));
  }

  protected static List<File> getFiles(
      Path inputDir, ManifestFieldGroup fieldGroup, String fieldName) {
    return fieldGroup.stream()
        .filter(
            field ->
                field.getDefinition().getType() == ManifestFieldType.FILE
                    && field.getName().equals(fieldName))
        .map(field -> getFile(inputDir, field))
        .map(file -> file.toPath())
        .map(path -> path.normalize())
        .map(path -> path.toFile())
        .collect(Collectors.toList());
  }

  protected static File getFile(Path inputDir, ManifestFieldValue field) {
    if (field == null) {
      return null;
    }
    assert (field.getDefinition().getType() == ManifestFieldType.FILE);

    String fileName = field.getValue();
    if (!Paths.get(fileName).isAbsolute())
      return new File(inputDir.resolve(Paths.get(fileName)).toString());
    else return new File(fileName);
  }

  protected static List<Map.Entry<String, String>> getAttributes(ManifestFieldValue field) {
    return field.getAttributes().stream()
        .map(
            attField -> {
              Map<String, String> map = new HashMap<>();
              map.put(attField.getName(), attField.getValue());

              return map.entrySet().stream().findFirst().get();
            })
        .collect(Collectors.toList());
  }

  /**
   * Gets the manifest object against the given field group. If an existing manifest is not found
   * then a new one is first created and then returned.
   */
  protected M getManifest(ManifestFieldGroup fieldGroup) {
    String nameField = fieldGroup.getValue(Fields.NAME);

    return nameFieldToManifestMap.computeIfAbsent(
        nameField,
        key -> {
          if (key == null) {
            throw new IllegalArgumentException(
                "The manifest field group does not have a NAME field.");
          }

          return createManifest();
        });
  }

  /** Adds an error to the validation result. */
  protected final void error(
      ValidationResult result, WebinCliMessage message, Object... arguments) {
    result.add(ValidationMessage.error(message, arguments));
  }

  /** Adds an error to the manifest level validation result. */
  protected final void error(WebinCliMessage message, Object... arguments) {
    getValidationResult().add(ValidationMessage.error(message, arguments));
  }

  private boolean matchNameCaseAndPunctuationInsensitively(String name1, String name2) {
    return name1.replaceAll("[ _-]+", "").equalsIgnoreCase(name2.replaceAll("[ _-]+", ""));
  }
}
