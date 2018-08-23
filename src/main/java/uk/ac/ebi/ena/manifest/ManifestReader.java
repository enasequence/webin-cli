/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.manifest;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationResult;

import static uk.ac.ebi.ena.manifest.ManifestReader.Fields.INFO;
import static uk.ac.ebi.ena.manifest.ManifestReader.ManifestReaderState.State.PARSE;
import static uk.ac.ebi.ena.manifest.ManifestReader.ManifestReaderState.State.VALIDATE;

public abstract class ManifestReader {

    public interface
    Fields {
        String INFO = "INFO";
    }

    private List<ManifestFieldDefinition> infoFields = new ArrayList<ManifestFieldDefinition>() {{
        add(new ManifestFieldDefinition(INFO, ManifestFieldType.FILE, 0, 1));
    }};

    private static final String MESSAGE_BUNDLE = "uk.ac.ebi.ena.manifest.ManifestReaderMessages";

    static
    {
        ValidationMessageManager.addBundle(MESSAGE_BUNDLE);
    }

    static class ManifestReaderState
    {
        public ManifestReaderState(Path inputDir, String fileName) {
            this.inputDir = inputDir;
            this.fileName = fileName;
        }

        enum State {
            INIT,
            PARSE,
            VALIDATE;
        }
        State state = State.INIT;
        final Path inputDir;
        String fileName;
        int lineNo = 0;
    }

    private final List<ManifestFieldDefinition> fields;
    private final Set<List<ManifestFileCount>> files;
    private ManifestReaderResult result;
    private ManifestReaderState state;

    public ManifestReader(List<ManifestFieldDefinition> fields) {
        this.fields = fields;
        this.files = null;
    }

    public ManifestReader(List<ManifestFieldDefinition> fields,
                          Set<List<ManifestFileCount>> files) {
        this.fields = fields;
        this.files = files;
    }

    public final Path getInputDir() {
        return state.inputDir;
    }

    public final ManifestReaderResult getResult() {
        return result;
    }

    public final ValidationResult getValidationResult() {
        return result.getValidationResult();
    }

    public final boolean
    readManifest(Path inputDir, File file) {

        state = new ManifestReaderState(inputDir, file.getPath());
        result = new ManifestReaderResult();

        List<String> manifestLines;
        try {
            manifestLines = Files.readAllLines(file.toPath());
        }
        catch (IOException ex) {
            error("MANIFEST_ERROR_READING_MANIFEST_FILE", file.getPath());
            return result.isValid();
        }

        // Parse.

        parseManifest(inputDir, manifestLines);

        // Expand info fields.

        List<ManifestFieldValue> infoFields = result.getFields()
                .stream()
                .filter( field -> field.getName().equalsIgnoreCase(INFO))
                .collect(Collectors.toList());

        for (ManifestFieldValue infoField : infoFields) {

            List<String> infoLines;
            File infoFile = new File(infoField.getValue());
            try {
                infoLines = readAllLines( infoFile );
            }
            catch (IOException ex) {
                 error("MANIFEST_ERROR_READING_INFO_FILE", infoFile.getPath());
                return result.isValid();
            }

            String savedManifestFileName = state.fileName;
            int savedManifestLineNo = state.lineNo;

            try {
                state.fileName = infoFile.getPath();
                state.lineNo = 0;

                parseManifest(inputDir, infoLines);
            }
            finally {
                state.fileName = savedManifestFileName;
                state.lineNo = savedManifestLineNo;
            }
        }

        // Remove info fields.

        result.setFields(result.getFields()
                .stream()
                .filter( field -> !field.getName().equalsIgnoreCase(INFO))
                .collect(Collectors.toList()));

        // Validate.

        validateManifest();

        // Process

        processManifest();

        return result.isValid();
    }

    private void
    parseManifest(Path inputDir, List<String> lines)
    {
        state.state = PARSE;

        for( String line : lines )
        {
            parseManifestLine(inputDir, line);
        }
    }

    private ManifestFieldValue
    parseManifestLine(Path inputDir, String line) {

        if (line == null)
            return null;

        ++state.lineNo;

        line = line.trim();

        if( line.isEmpty() )
            return null;

        String tokens[] = line.split( "\\s+", 2 );

        String fieldName = StringUtils.stripEnd(tokens[ 0 ].trim().toUpperCase(), ": ");
        String fieldValue = (tokens.length == 2) ? tokens[ 1 ].trim() : null;

        if( fieldName.matches( "^[\\s]*(#|;|\\/\\/).*$" ) )
            return null;

        try {
            ManifestFieldDefinition fieldDefinition = Stream.concat(infoFields.stream(), fields.stream())
                    .filter(field -> field.getName().equalsIgnoreCase(fieldName))
                    .findFirst()
                    .get();

            if (fieldValue != null) {
                ManifestFieldValue field = new ManifestFieldValue(fieldDefinition, fieldValue, createParseOrigin());
                result.getFields().add(field);

                if (field.getDefinition().getType() == ManifestFieldType.FILE) {

                    // Validate file exists.

                    validateFileExists(inputDir, field);

                    // Validate file suffix.

                    validateFileSuffix(field);
                }

                return field;
            }

        } catch (NoSuchElementException ex) {
            error("MANIFEST_UNKNOWN_FIELD", fieldName);
        }

        return null;
    }

    private final void
    validateManifest() {

        state.state = VALIDATE;

        // Validate min count.

        fields.stream()
                .filter(field -> field.getMinCount() > 0)
                .forEach( minCountField -> {
                    if (result.getFields().stream()
                            .filter(field -> field.getName().equals(minCountField.getName()))
                            .count() < 1) {

                        if (minCountField.getFieldValueOrFileSuffix() != null) {
                            error("MANIFEST_MISSING_MANDATORY_FIELD_WITH_VALUES",
                                    minCountField.getName(),
                                    minCountField.getFieldValueOrFileSuffix().stream().collect(Collectors.joining(", ")));
                        }
                        else {
                            error("MANIFEST_MISSING_MANDATORY_FIELD", minCountField.getName());
                        }
                    }
                });

        // Validate max count.

        fields.stream()
                .filter(field -> field.getMaxCount() > 0)
                .forEach( maxCountField -> {
                    List<ManifestFieldValue> matchingFields = result.getFields().stream()
                            .filter(field -> field.getName().equals(maxCountField.getName()))
                            .collect( Collectors.toList());

                    if (matchingFields.size() > maxCountField.getMaxCount() ) {
                        error("MANIFEST_TOO_MANY_FIELDS",
                                maxCountField.getName(),
                                String.valueOf(maxCountField.getMaxCount()));
                    }
                });

        // Fix cv field casing.

        result.getFields().stream()
                .filter(field ->
                        field.getDefinition().getType() == ManifestFieldType.META &&
                                field.getDefinition().isFieldValueOrFileSuffix()
                )
                .forEach( cvField -> {
                    try {
                        String str = cvField.getDefinition().getFieldValueOrFileSuffix().stream()
                                .filter(value -> value.equalsIgnoreCase(cvField.getValue()))
                                .findFirst()
                                .get();
                        cvField.setValue(str);
                    } catch (NoSuchElementException ex) {
                    }
                });

        // Validate cv fields.

        result.getFields().stream()
                .filter(field ->
                        field.getDefinition().getType() == ManifestFieldType.META &&
                        field.getDefinition().isFieldValueOrFileSuffix()
                )
                .forEach( cvField -> {
                    if (cvField.getDefinition().getFieldValueOrFileSuffix().stream()
                            .filter(value -> value.equals(cvField.getValue()))
                            .count() == 0) {
                        error("MANIFEST_INVALID_FIELD_VALUE",
                                cvField.getOrigin(),
                                cvField.getName(),
                                cvField.getValue(),
                                cvField.getDefinition().getFieldValueOrFileSuffix().stream().collect(Collectors.joining(", ")));
                        cvField.setValidFieldValueOrFileSuffix(false);
                    }
                });

        // Validate file count.

        validateFileCount();
    }

    protected abstract void
    processManifest();


    private boolean
    validateFileExists(Path inputDir, ManifestFieldValue field) {
        String fieldName = field.getName();
        String fieldValue = field.getValue();

        try {
            if (Files.isReadable(Paths.get(fieldValue)) &&
                    !Files.isDirectory(Paths.get(fieldValue))) {
                // File is readable relative to default working dir.
            } else if (Files.isReadable(inputDir.resolve(Paths.get(fieldValue))) &&
                    !Files.isDirectory(inputDir.resolve(Paths.get(fieldValue)))) {
                // File is readable relative to defined input dir.
                field.setValue(inputDir.resolve(Paths.get(fieldValue)).toString());
            }
            else {
                //fieldError(fieldName, String.format("Invalid file path: %s", fieldValue));
                error("MANIFEST_INVALID_FILE_FIELD", field.getOrigin(), fieldName, fieldValue);
                return false;
            }
        }
        catch (Throwable ex) {
            //fieldError(fieldName, String.format("Invalid file path: %s", fieldValue));
            error("MANIFEST_INVALID_FILE_FIELD", field.getOrigin(), fieldName, fieldValue);
            return false;
        }
        return true;
    }

    private boolean
    validateFileSuffix(ManifestFieldValue field)
    {
        assert(ManifestFieldType.FILE == field.getDefinition().getType());

        List<String> suffixes = field.getDefinition().getFieldValueOrFileSuffix();

        if( null == suffixes || 0 == suffixes.size() )
            return true;

        for( String suffix : suffixes )
        {
            if( field.getValue().endsWith( suffix ) )
                return true;
        }

        error("MANIFEST_INVALID_FILE_SUFFIX",
            field.getOrigin(),
            field.getName(),
            field.getValue(),
            suffixes.stream().collect( Collectors.joining( ", " )));

        field.setValidFieldValueOrFileSuffix(false);

        return false;
    }

    private boolean
    validateFileCount() {

        if (files == null ||
            files.isEmpty()) {
            return true;
        }

        Map<String, Long> fileCountMap = result.getFields().stream()
                .filter(field -> field.getDefinition().getType().equals(ManifestFieldType.FILE))
                .collect(Collectors.groupingBy(ManifestFieldValue::getName, Collectors.counting()));

        if (fileCountMap == null || fileCountMap.isEmpty()) {
            error("MANIFEST_ERROR_NO_DATA_FILES",
                    getExpectedFileTypeList(files));
            return false;
        }

        next:
        for (List<ManifestFileCount> expectedFileCountList : files) {
            for (ManifestFileCount expectedFileCount : expectedFileCountList) {
                if (fileCountMap.get(expectedFileCount.getFileType()) == null) {
                    if (expectedFileCount.getMinCount() > 0) {
                        continue next; // Invalid because min is > 0.
                    }
                } else {
                    long manifestFileCount = fileCountMap.get(expectedFileCount.getFileType());
                    if ((expectedFileCount.getMaxCount() < manifestFileCount) ||
                            (expectedFileCount.getMinCount() > manifestFileCount)) {
                        continue next; // Invalid because larger than min or smaller than max.
                    }
                }
            }

            for (String manifestFileType : fileCountMap.keySet()) {
                if (expectedFileCountList.stream()
                        .filter(expectedFileCount -> expectedFileCount.getFileType().equals(manifestFileType))
                        .count() < 1) {
                    continue next; // Invalid because unmatched file type.
                }
            }

            return true; // Valid
        }

        error("MANIFEST_ERROR_INVALID_FILE_GROUP",
                getExpectedFileTypeList(files));

        return false;
    }

    protected final Integer
    getAndValidateNonNegativeInteger(ManifestFieldValue field) {
        if (field == null) {
            return null;
        }
        String fieldValue = field.getValue();
        if (fieldValue == null) {
            return null;
        }

        try
        {
            int value = Integer.valueOf( fieldValue );
            if( value < 0 )
                error("MANIFEST_INVALID_NEGATIVE_FIELD_VALUE", field.getOrigin(), field.getName());
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error("MANIFEST_INVALID_NEGATIVE_FIELD_VALUE", field.getOrigin(), field.getName());
        }

        return null;
    }

    private static List<String>
    readAllLines( InputStream is )
    {
        return new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ).lines().collect( Collectors.toList() );
    }

    public static List<String>
    readAllLines( File file ) throws FileNotFoundException
    {
        try( InputStream is = new GZIPInputStream( new FileInputStream( file ) ) )
        {
            return readAllLines( is );
        } catch( IOException ioe )
        {
            try( InputStream is = new BZip2CompressorInputStream( new FileInputStream( file ) ) )
            {
                return readAllLines( is );
            }catch( IOException ie )
            {
                return readAllLines( new FileInputStream( file ) );
            }
        }
    }

    /*
    private static String
    getActualFileTypeList(Map<String, Integer> manifestFileCountMap){
        StringBuilder str = new StringBuilder();
        String separator = "";
        for (String fileType : manifestFileCountMap.keySet()) {
            int fileCnt = manifestFileCountMap.get(fileType);
            str.append(separator);
            str.append(manifestFileCountMap.get(fileType));
            str.append(" \"");
            str.append(fileType);
            if (fileCnt > 1) {
                str.append("\" files");
            }
            else {
                str.append("\" file");
            }
            separator = ", ";
        }
        return str.toString();
    }
    */

    private static String
    getExpectedFileTypeList(Set<List<ManifestFileCount>> expectedFileCountSet) {
        return expectedFileCountSet.stream()
                .map( expectedFileCountList -> {
                    StringBuilder str = new StringBuilder();
                    str.append( "[" );
                    String fileSeparator = "";
                    for (ManifestFileCount expectedFileCount : expectedFileCountList) {
                        String fileType =  expectedFileCount.getFileType();
                        str.append( fileSeparator );
                        if ( expectedFileCount.getMinCount() == expectedFileCount.getMaxCount()) {
                            str.append( expectedFileCount.getMinCount() );
                        }
                        else {
                            str.append(expectedFileCount.getMinCount());
                            str.append("..");
                            str.append(expectedFileCount.getMaxCount());
                        }
                        str.append( " \"" );
                        str.append( fileType );
                        str.append( "\" file(s)" );
                        fileSeparator = ", ";
                    }
                    str.append( "]" );
                    return str.toString();
                })
                .collect(Collectors.joining(", "));
    }

    private void
    appendOrigin(ValidationMessage validationMessage) {
        switch (state.state) {
            case PARSE:
                validationMessage.append(createParseOrigin());
                break;
            case VALIDATE:
                validationMessage.append(createValidateOrigin());
                break;
        }
    }

    /*

    protected final void
    errorNoKey(String error)
    {
        ValidationMessage validationMessage = ValidationMessage.error(ValidationMessage.NO_KEY);
        validationMessage.setMessage(error);
        appendOrigin(validationMessage);
        result.getValidationResult().append(validationMessage);
    }

    protected final void
    errorNoKey(String error, Origin origin)
    {
        ValidationMessage validationMessage = ValidationMessage.error(ValidationMessage.NO_KEY);
        validationMessage.setMessage(error);
        validationMessage.append(origin);
        result.getValidationResult().append(validationMessage);
    }
    */

    protected final void
    error(String key, String ... params)
    {
        ValidationMessage validationMessage = ValidationMessage.error(key, params);
        appendOrigin(validationMessage);
        result.getValidationResult().append(validationMessage);
    }

    protected final void
    error(String key, Origin origin, String ... params)
    {
        ValidationMessage validationMessage = ValidationMessage.error(key, params);
        validationMessage.append(origin);
        result.getValidationResult().append(validationMessage);
    }



    protected final Origin
    createParseOrigin() {
        String origin = String.format("File name: " + state.fileName + ", line number: " + state.lineNo );
        return () -> origin;
    }

    protected final Origin
    createValidateOrigin() {
        String origin = String.format("File name: " + state.fileName );
        return () -> origin;
    }
}
