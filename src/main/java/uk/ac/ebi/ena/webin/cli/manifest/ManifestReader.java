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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationOrigin;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.validator.message.listener.MessageListener;

public abstract class
ManifestReader<M extends Manifest> {

    public static final String KEY_VALUE_COMMENT_REGEX = "^[\\s]*(#|;|\\/\\/).*$";

    public abstract M getManifest();

    public interface Fields {
        String INFO = "INFO";
        String SUBMISSION_TOOL = "SUBMISSION_TOOL";
        String SUBMISSION_TOOL_VERSION = "SUBMISSION_TOOL_VERSION";
    }

    public interface Descriptions {
        String INFO = "Info file";
        String SUBMISSION_TOOL = "Name of third-party or developed tool used to submit to ENA";
        String SUBMISSION_TOOL_VERSION = "Version number of the third-party or developed tool used to submit to ENA";
    }

    private final List<ManifestFieldDefinition> infoFields = new ManifestFieldDefinition.Builder()
        .file().optional().name(Fields.INFO).desc(Descriptions.INFO).build();

    static class ManifestReaderState
    {
        ManifestReaderState(Path inputDir, String fileName)
        {
            this.inputDir = inputDir;
            this.fileName = fileName;
        }

        enum
        State
        {
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
    private final List<ManifestFieldDefinition> fields;
    private final List<ManifestFileGroup> fileGroups;
    private List<MessageListener> listener = new ArrayList<>();
    private ManifestReaderResult manifestReaderResult;
    private ManifestReaderState state;

    public
    ManifestReader( WebinCliParameters webinCliParameters,
                    List<ManifestFieldDefinition> fields )
    {
        this.webinCliParameters = webinCliParameters;
        this.fields = fields;
        this.fileGroups = null;
    }

    public
    ManifestReader( WebinCliParameters webinCliParameters,
                    List<ManifestFieldDefinition> fields,
                    List<ManifestFileGroup> fileGroups)
    {
        this.webinCliParameters = webinCliParameters;
        this.fields = fields;
        this.fileGroups = fileGroups;
    }

    public final Path
    getInputDir()
    {
        return state.inputDir;
    }

    public List<ManifestFieldDefinition> getFields() {
        return fields;
    }

    public List<ManifestFileGroup> getFileGroups() {
        return fileGroups;
    }

    public void addListener(MessageListener listener) {
        this.listener.add(listener);
    }

    public final ManifestReaderResult
    getManifestReaderResult()
    {
        return manifestReaderResult;
    }

    public final ValidationResult
    getValidationResult()
    {
        return manifestReaderResult.getValidationResult();
    }

    public final void
    readManifest( Path inputDir, File manifestFile ) {
        readManifest(inputDir, manifestFile, null);
    }

    public final void
    readManifest( Path inputDir, File manifestFile, File reportFile )
    {
        state = new ManifestReaderState( inputDir, manifestFile.getPath() );

        ValidationOrigin origin = new ValidationOrigin("manifest file", state.fileName);
        ValidationResult result = new ValidationResult(reportFile, origin);

        //used for testing purpose
        listener.forEach(l -> result.add(l));

        manifestReaderResult = new ManifestReaderResult(result);

        List<String> manifestLines;
        try
        {
            manifestLines = Files.readAllLines( manifestFile.toPath() );
        } catch( IOException ex )
        {
            error( WebinCliMessage.MANIFEST_READER_MANIFEST_FILE_READ_ERROR, manifestFile.getPath() );
            return;
        }

        // Parse.

        parseManifest( inputDir, manifestLines );

        // Expand info fields.

        List<ManifestFieldValue> infoFields = manifestReaderResult.getFields()
                                                    .stream()
                                                    .filter( field -> field.getName().equalsIgnoreCase( INFO ) )
                                                    .collect( Collectors.toList() );

        for( ManifestFieldValue infoField : infoFields )
        {

            List<String> infoLines;
            File infoFile = new File( infoField.getValue() );
            try
            {
                infoLines = readAllLines( infoFile );
            } catch( IOException ex )
            {
                error( WebinCliMessage.MANIFEST_READER_INFO_FILE_READ_ERROR, infoFile.getPath() );
                return;
            }

            String savedManifestFileName = state.fileName;
            int savedManifestLineNo = state.lineNo;

            try
            {
                state.fileName = infoFile.getPath();
                state.lineNo = 0;

                parseManifest( inputDir, infoLines );
            } finally
            {
                state.fileName = savedManifestFileName;
                state.lineNo = savedManifestLineNo;
            }
        }

        // Remove info fields.

        manifestReaderResult.setFields( manifestReaderResult.getFields()
                                .stream()
                                .filter( field -> !field.getName().equalsIgnoreCase( INFO ) )
                                .collect( Collectors.toList() ) );

        // Validate.
        validateManifest();

        // Process
        processManifest();
    }


    private void
    parseManifest( Path inputDir, List<String> lines )
    {
        if (isJsonBasedFormat(lines)) {
            parseJsonManifest(inputDir, lines);
        } else {
            parseKeyValueManifest(inputDir, lines);
        }
    }

    private boolean isJsonBasedFormat(List<String> lines) {
        if (lines.isEmpty()) {
            return false;
        }

        return lines.get(0).trim().startsWith("{");
    }


    private void parseKeyValueManifest(Path inputDir, List<String> lines )
    {
        state.state = PARSE;

        for( String line : lines )
        {
            ManifestFieldValue field = parseManifestLine( inputDir, line );
            if( null != field )
                manifestReaderResult.getFields().add( field );
        }
    }


    private ManifestFieldValue
    parseManifestLine( Path inputDir, String line )
    {

        if( line == null )
            return null;

        ++state.lineNo;

        line = line.trim();

        if( line.isEmpty() )
            return null;

        String[] tokens = line.split("\\s+", 2);

        String fieldName = StringUtils.stripEnd( tokens[ 0 ].trim().toUpperCase(), ": " );
        String fieldValue = ( tokens.length == 2 ) ? tokens[ 1 ].trim() : null;

        if( fieldName.matches(KEY_VALUE_COMMENT_REGEX) ) // Ignore comment lines.
            return null;

        try
        {
            ManifestFieldDefinition fieldDefinition = Stream
                    .concat( infoFields.stream(), fields.stream() )
                    .filter(
                            field -> matchNameCaseAndPunctuationInsensitively(field.getName(), fieldName) ||
                                     field.matchSynonym( fieldName ))
                    .findFirst()
                    .get();

            if( fieldValue != null )
            {
                ManifestFieldValue field = new ManifestFieldValue(
                        fieldDefinition,
                        fieldValue,
                        new ArrayList<>(), //attributes are not supported in the old manifest format.
                        new ValidationOrigin("line number", state.lineNo));

                getValidationResult().create(field.getOrigin());

                if( field.getDefinition().getType() == ManifestFieldType.FILE )
                {
                    // Validate file exists.
                    validateFileExists( inputDir, field );
                }

                return field;
            }

        } catch( NoSuchElementException ex )
        {
            error( WebinCliMessage.MANIFEST_READER_UNKNOWN_FIELD_ERROR, fieldName );
        }

        return null;
    }

    private void parseJsonManifest(Path inputDir, List<String> lines ) {
        state.state = PARSE;

        try {
            ObjectMapper objectMapper = new ObjectMapper();

            JsonNode jsonNode = objectMapper.readTree(lines.stream().collect(Collectors.joining("\n")));

            jsonNode.fields().forEachRemaining(fieldEntry -> {
                String fieldName = fieldEntry.getKey();

                //find field definition
                ManifestFieldDefinition fieldDefinition = Stream.concat( infoFields.stream(), fields.stream() )
                        .filter(fieldDef -> matchNameCaseAndPunctuationInsensitively(fieldDef.getName(), fieldName) ||
                                        fieldDef.matchSynonym( fieldName ))
                        .findFirst().orElse(null);
                if (fieldDefinition == null) {
                    error( WebinCliMessage.MANIFEST_READER_UNKNOWN_FIELD_ERROR, fieldName );
                    return;
                }

                JsonNode field = fieldEntry.getValue();
                if (field.isValueNode()) {
                    addManifestField(inputDir, fieldDefinition, field.asText(), new ArrayList<>());
                } else if (field.isArray()) {
                    field.elements().forEachRemaining(subField -> {
                        if (subField.isValueNode()) {
                            addManifestField(inputDir, fieldDefinition, subField.asText(), new ArrayList<>());
                        } else {
                            handleFieldWithAttributes(inputDir, fieldDefinition, subField);
                        }
                    });
                } else {
                    // ValueNode is JsonObject
                    if(fieldName.equalsIgnoreCase("sample")){
                        // Check if sample alias is present in sample JSON.
                        if(null == field.get("alias")){
                            throw WebinCliException.userError(WebinCliMessage.MANIFEST_READER_MISSING_SAMPLE_ALIAS.text());
                        }
                        // Add json as manifest field
                        addManifestField(inputDir, fieldDefinition, field.toString(), new ArrayList<>());
                    }else {
                        handleFieldWithAttributes(inputDir, fieldDefinition, field);
                    }
                }
            });
        } catch (IOException e) {
            error( WebinCliMessage.MANIFEST_READER_MANIFEST_FILE_MALFORMED);
        }
    }

    private void addManifestField(Path inputDir, ManifestFieldDefinition fieldDefinition,
                                  String fieldValue, List<ManifestFieldValue> fieldAttributes) {

        if( fieldValue != null ) {
            ManifestFieldValue manifestField = new ManifestFieldValue(fieldDefinition, fieldValue, fieldAttributes,
                new ValidationOrigin("file name", state.fileName));

            getValidationResult().create(manifestField.getOrigin());

            if( manifestField.getDefinition().getType() == ManifestFieldType.FILE ) {
                validateFileExists( inputDir, manifestField );
            }

            manifestReaderResult.getFields().add( manifestField );
        }
    }

    private void handleFieldWithAttributes(Path inputDir, ManifestFieldDefinition fieldDefinition, JsonNode field) {
        List<ManifestFieldValue> fieldAttributes = new ArrayList<>();

        //Presence of attributes for a field in the JSON is not enough to load them.
        //The field must have provided the expected attributes in the definition first.
        if (field.has("attributes") && !fieldDefinition.getFieldAttributes().isEmpty()) {
            field.get("attributes").fields().forEachRemaining(attEntry -> {
                String attName = attEntry.getKey();

                //find attribute definition in field's attributes definitions.
                ManifestFieldDefinition attDef = fieldDefinition.getFieldAttributes().stream()
                    .filter(attFieldDef -> matchNameCaseAndPunctuationInsensitively(attFieldDef.getName(), attName) ||
                        attFieldDef.matchSynonym( attName ))
                    .findFirst().orElse(null);
                if (attDef == null) {
                    error( WebinCliMessage.MANIFEST_READER_UNKNOWN_ATTRIBUTE_FIELD_ERROR, attName );
                    return;
                }

                JsonNode att = attEntry.getValue();

                if (att.isArray()) {
                    att.elements().forEachRemaining(textElement -> {
                        fieldAttributes.add(new ManifestFieldValue(attDef, textElement.asText(), new ArrayList<>(),
                            new ValidationOrigin("file name", state.fileName)));
                    });
                } else {
                    fieldAttributes.add(new ManifestFieldValue(attDef, att.asText(), new ArrayList<>(),
                        new ValidationOrigin("file name", state.fileName)));
                }
            });
        }

        addManifestField(inputDir, fieldDefinition, field.get("value").asText(), fieldAttributes);
    }

    private void
    validateManifest()
    {

        state.state = VALIDATE;

        // Validate min count.

        fields.stream()
              .filter( field -> field.getMinCount() > 0 )
              .forEach( minCountField -> {
                  if( manifestReaderResult.getFields()
                            .stream()
                            .filter( field -> field.getName().equals( minCountField.getName() ) )
                            .count() < 1 )
                  {
                      error( WebinCliMessage.MANIFEST_READER_MISSING_MANDATORY_FIELD_ERROR, minCountField.getName() );
                  }
              } );

        // Validate max count.

        fields.stream()
              .filter( field -> field.getMaxCount() > 0 )
              .forEach( maxCountField -> {
                  List<ManifestFieldValue> matchingFields = manifestReaderResult.getFields()
                                                                  .stream()
                                                                  .filter( field -> field.getName().equals( maxCountField.getName() ) )
                                                                  .collect( Collectors.toList() );

                    if( matchingFields.size() > maxCountField.getMaxCount() )
                    {
                        error( WebinCliMessage.MANIFEST_READER_TOO_MANY_FIELDS_ERROR,
                                maxCountField.getName(),
                                String.valueOf( maxCountField.getMaxCount() ) );
                    }
                } );

       // Validate and fix fields.

        for( ManifestFieldValue fieldValue : manifestReaderResult.getFields() )
        {
            ManifestFieldDefinition field = fieldValue.getDefinition();

            for( ManifestFieldProcessor processor : field.getFieldProcessors() )
            {
                ValidationResult result = getValidationResult().create(fieldValue.getOrigin());
                processor.process( result, fieldValue );
                fieldValue.setValidFieldValueOrFileSuffix( result.isValid() );
            }

            //iterate over field attributes and run their processors.
            for (ManifestFieldValue att : fieldValue.getAttributes()) {
                ManifestFieldDefinition attDef = att.getDefinition();

                for( ManifestFieldProcessor attProcessor : attDef.getFieldProcessors() )
                {
                    ValidationResult result = getValidationResult().create(att.getOrigin());
                    attProcessor.process( result, att );
                    att.setValidFieldValueOrFileSuffix( result.isValid() );
                }
            }
        }

        // Validate file count.
        validateFileCount();

        // Ensure that all file names are unique.
        validateUniqueFileNames();
    }


    protected abstract void
    processManifest();


    private void
    validateFileExists( Path inputDir, ManifestFieldValue field )
    {
        ValidationResult result = getValidationResult().create(field.getOrigin());

        String fieldValue = field.getValue();

        try
        {
            if( Files.isReadable( Paths.get( fieldValue ) )
                && !Files.isDirectory( Paths.get( fieldValue ) ) )
            {
                // File is readable relative to default working dir.
            } else if( Files.isReadable( inputDir.resolve( Paths.get( fieldValue ) ) )
                       && !Files.isDirectory( inputDir.resolve( Paths.get( fieldValue ) ) ) )
            {
                // File is readable relative to defined input dir.
                field.setValue(inputDir.resolve(Paths.get(fieldValue)).toString());
            }
            else {
                error(result, WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR, fieldValue);
                return;
            }

            validateFileCompression(result, field.getValue());
        }
        catch (Throwable ex) {
            error(result, WebinCliMessage.MANIFEST_READER_INVALID_FILE_FIELD_ERROR, fieldValue);
        }
    }

    private void
    validateFileCount()
    {
        if( fileGroups == null || fileGroups.isEmpty() )
            return;

        Map<String, Long> fileCountMap = manifestReaderResult.getFields()
                                               .stream()
                                               .filter( field -> field.getDefinition().getType().equals( ManifestFieldType.FILE ) )
                                               .collect( Collectors.groupingBy( ManifestFieldValue::getName, Collectors.counting() ) );

        if( fileCountMap == null || fileCountMap.isEmpty() )
        {
            error(WebinCliMessage.MANIFEST_READER_NO_DATA_FILES_ERROR, getFileGroupText(fileGroups));
            return;
        }

        next:
        for (ManifestFileGroup fileGroup : fileGroups) {
            for (ManifestFileCount fileCount : fileGroup.getFileCounts()) {
                if (fileCountMap.get(fileCount.getFileType()) == null) {
                    if (fileCount.getMinCount() > 0) {
                        continue next; // Invalid because min is > 0.
                    }
                } else {
                    long manifestFileCount = fileCountMap.get(fileCount.getFileType());
                    if ((fileCount.getMaxCount() != null && fileCount.getMaxCount() < manifestFileCount) ||
                        (fileCount.getMinCount() > manifestFileCount)) {
                        continue next; // Invalid because larger than max or smaller than min.
                    }
                }
            }

            for( String manifestFileType : fileCountMap.keySet() )
            {
                if( fileGroup.getFileCounts().stream()
                     .filter( fileCount -> fileCount.getFileType().equals( manifestFileType ) )
                     .count() < 1 )
                {
                    continue next; // Invalid because unmatched file type.
                }
            }

            return; // Valid
        }

        error(WebinCliMessage.MANIFEST_READER_INVALID_FILE_GROUP_ERROR, getFileGroupText(fileGroups), "" );
    }

    private void validateUniqueFileNames() {
        if ( fileGroups == null || fileGroups.isEmpty() ) {
            return;
        }

        List<ManifestFieldValue> fileFields = manifestReaderResult.getFields()
            .stream()
            .filter( field -> field.getDefinition().getType().equals( ManifestFieldType.FILE ) )
            .collect(Collectors.toList());

        HashSet<String> fileNameSet = new HashSet<>(fileFields.size());

        for (ManifestFieldValue fileField : fileFields) {
            if (!fileNameSet.add(Paths.get(fileField.getValue()).getFileName().toString())) {
                error(WebinCliMessage.MANIFEST_READER_INVALID_FILE_NON_UNIQUE_NAMES);
                break;
            }
        }
    }

    private void validateFileCompression(ValidationResult result, String filePath) {
        if (filePath.endsWith(ManifestFileSuffix.GZIP_FILE_SUFFIX)) {
            try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(filePath))) {
            } catch (Exception e) {
                error(result, WebinCliMessage.MANIFEST_READER_INVALID_FILE_COMPRESSION_ERROR, filePath, "gzip");
            }
        }
        else if (filePath.endsWith(ManifestFileSuffix.BZIP2_FILE_SUFFIX)) {
            try( BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(new FileInputStream(filePath))) {
            }
            catch (Exception e) {
                error(result, WebinCliMessage.MANIFEST_READER_INVALID_FILE_COMPRESSION_ERROR, filePath, "bzip2");
            }
        }
    }

    protected final Integer
    getAndValidatePositiveInteger(ManifestFieldValue field) {
        if (field == null) {
            return null;
        }
        String fieldValue = field.getValue();
        if (fieldValue == null) {
            return null;
        }

        ValidationResult result = getValidationResult().create(field.getOrigin());

        try
        {
            int value = Integer.valueOf( fieldValue );
            if( value <= 0 ) {
                error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_INTEGER_ERROR);
                return null;
            }
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_INTEGER_ERROR);
        }

        return null;
    }

    protected final Float
    getAndValidatePositiveFloat(ManifestFieldValue field) {
        if (field == null) {
            return null;
        }
        String fieldValue = field.getValue();
        if (fieldValue == null) {
            return null;
        }

        ValidationResult result = getValidationResult().create(field.getOrigin());

        try
        {
            float value = Float.valueOf( fieldValue );
            if( value <= 0 ) {
                error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_FLOAT_ERROR);
                return null;
            }
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error(result, WebinCliMessage.MANIFEST_READER_INVALID_POSITIVE_FLOAT_ERROR);
        }

        return null;
    }

    protected final Boolean
    getAndValidateBoolean(ManifestFieldValue field) {
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


    private static List<String>
    readAllLines( InputStream is )
    {
        return new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ).lines().collect( Collectors.toList() );
    }


    private static List<String>
    readAllLines(File file) throws FileNotFoundException
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

    public static String
    getFileGroupText(List<ManifestFileGroup> fileGroups )
    {
        return fileGroups.stream().map( fileGroup -> {
            StringBuilder str = new StringBuilder();
            str.append( "[" );
            String fileSeparator = "";
               for( ManifestFileCount fileCount : fileGroup.getFileCounts() )
               {
                   String fileType = fileCount.getFileType();

                   str.append( fileSeparator );
                   if( fileCount.getMaxCount() != null &&
                       fileCount.getMinCount() == fileCount.getMaxCount() )
                   {
                       str.append(fileCount.getMinCount());
                   }
                   else {
                       if ( fileCount.getMaxCount() != null ) {
                           str.append( fileCount.getMinCount());
                           str.append( "-" );
                           str.append( fileCount.getMaxCount());
                       }
                       else {
                           str.append( ">=" );
                           str.append( fileCount.getMinCount());
                       }
                   }
                   str.append( " " );
                   str.append( fileType );
                   fileSeparator = ", ";
               }
               str.append( "]" );
               return str.toString();
           } )
           .collect( Collectors.joining( " or " ) );
    }

    protected static List<File>
    getFiles(Path inputDir, ManifestReaderResult result, String fieldName) {
        return result.getFields().stream()
                .filter(field -> field.getDefinition().getType() == ManifestFieldType.FILE &&
                        field.getName().equals(fieldName))
                .map(field -> getFile(inputDir, field))
                .map(file -> file.toPath())
                .map(path -> path.normalize())
                .map(path -> path.toFile())
                .collect(Collectors.toList());
    }

    protected static File
    getFile(Path inputDir, ManifestFieldValue field) {
        if (field == null) {
            return null;
        }
        assert (field.getDefinition().getType() == ManifestFieldType.FILE);

        String fileName = field.getValue();
        if( !Paths.get( fileName ).isAbsolute() )
            return new File( inputDir.resolve( Paths.get( fileName ) ).toString() );
        else
            return new File(fileName);
    }

    protected static List<Map.Entry<String, String>> getAttributes(ManifestFieldValue field) {
        return field.getAttributes().stream()
            .map(attField -> {
                Map<String, String> map = new HashMap<>();
                map.put(attField.getName(), attField.getValue());

                return map.entrySet().stream().findFirst().get();
            }).collect(Collectors.toList());
    }

    /** Adds an error to the validation result.
     */
    protected final void
    error(ValidationResult result, WebinCliMessage message, Object... arguments )
    {
        result.add(ValidationMessage.error(message, arguments));
    }

    /** Adds an error to the manifest level validation result.
     */
    protected final void
    error(WebinCliMessage message, Object... arguments )
    {
        getValidationResult().add(ValidationMessage.error(message, arguments));
    }

    public WebinCliParameters getWebinCliParameters() {
        return webinCliParameters;
    }

    private boolean matchNameCaseAndPunctuationInsensitively(String name1, String name2) {
        return name1.replaceAll("[ _-]+", "")
                .equalsIgnoreCase(name2.replaceAll("[ _-]+", ""));
    }
}
