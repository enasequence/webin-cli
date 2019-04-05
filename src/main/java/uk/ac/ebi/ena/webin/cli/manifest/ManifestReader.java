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
package uk.ac.ebi.ena.webin.cli.manifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReader.Fields.INFO;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReader.ManifestReaderState.State.PARSE;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReader.ManifestReaderState.State.VALIDATE;

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

import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public abstract class 
ManifestReader 
{

    public static final int MAX_SPREADSHEET_COLUMNS = 100;

    public interface Fields {
        String INFO = "INFO";
    }

    public interface Descriptions {
        String INFO = "Info file";
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
        int lineNumber = 0;
    }

    private final List<ManifestFieldDefinition> fields;
    private final List<ManifestFileGroup> fileGroups;
    private ManifestReaderResult result;
    private ManifestReaderState state;
    private Integer spreadSheetLineNumber;

    public
    ManifestReader( List<ManifestFieldDefinition> fields )
    {
        this.fields = fields;
        this.fileGroups = null;
    }


    public
    ManifestReader( List<ManifestFieldDefinition> fields,
                    List<ManifestFileGroup> fileGroups)
    {
        this.fields = fields;
        this.fileGroups = fileGroups;
    }


    public final Path
    getInputDir()
    {
        return state.inputDir;
    }


    public abstract String getName();
    public abstract String getDescription();

    public List<ManifestFieldDefinition> getFields() {
        return fields;
    }

    public List<ManifestFileGroup> getFileGroups() {
        return fileGroups;
    }

    public final ManifestReaderResult
    getResult()
    {
        return result;
    }

    public final ValidationResult
    getValidationResult()
    {
        return result.getValidationResult();
    }

    public final void
    readManifest(Path inputDir, File manifestFile ) {
        readTextManifest(inputDir, new ManifestSource(manifestFile));
    }

    public final void
    readManifest(Path inputDir, ManifestSource manifestSource ) {
        if (manifestSource.isSpreadsheet()) {
            readSpreadsheetManifest(inputDir, manifestSource);
        }
        else {
            readTextManifest(inputDir, manifestSource);
        }
    }

    private void
    readSpreadsheetManifest(Path inputDir, ManifestSource manifestSource )
    {
        Sheet dataSheet = manifestSource.getDataSheet();
        int dataSheetRowNumber = manifestSource.getDataSheetRowNumber();

        Row headerRow = dataSheet.getRow(0);
        Row dataRow = dataSheet.getRow(dataSheetRowNumber);

        ArrayList<String> manifestLines = new ArrayList<>();
        for (int columnNumber = 0; columnNumber < MAX_SPREADSHEET_COLUMNS; columnNumber++) {
            DataFormatter dataFormatter = new DataFormatter();
            String key = dataFormatter.formatCellValue(headerRow.getCell(columnNumber));
            if (key == null || key.trim().isEmpty()) {
                continue;
            }
            String value = dataFormatter.formatCellValue(dataRow.getCell(columnNumber));
            manifestLines.add(key + "\t" + value );
        }

        spreadSheetLineNumber = dataSheetRowNumber;  // We are parsing a spreadsheet.

        state = new ManifestReaderState( inputDir, manifestSource.getManifestFile().getPath() );
        result = new ManifestReaderResult();

        parseManifest( inputDir, manifestLines );
        validateManifest();
        processManifest();

    }

    private void
    readTextManifest(Path inputDir, ManifestSource manifestSource )
    {
        spreadSheetLineNumber = null; // We are not parsing a spreadsheet.

        state = new ManifestReaderState( inputDir, manifestSource.getManifestFile().getPath() );
        result = new ManifestReaderResult();

        parseManifest( inputDir, readManifestLines(manifestSource.getManifestFile()) );
        expandInfoField(inputDir);
        validateManifest();
        processManifest();
    }



    private void
    parseManifest( Path inputDir, List<String> lines )
    {
        state.state = PARSE;

        for( String line : lines )
        {
            ManifestFieldValue field = parseManifestLine( inputDir, line );
            if( null != field )
                result.getFields().add( field );
        }
    }

    private void expandInfoField(Path inputDir) {
        List<ManifestFieldValue> infoFields = result.getFields()
                                                    .stream()
                                                    .filter( field -> field.getName().equalsIgnoreCase( INFO ) )
                                                    .collect( Collectors.toList() );

        for( ManifestFieldValue infoField : infoFields )
        {
            File infoFile = new File( infoField.getValue() );
            String savedManifestFileName = state.fileName;
            int savedManifestLineNumber = state.lineNumber;
            state.fileName = infoFile.getPath();
            state.lineNumber = 0;
            parseManifest( inputDir, readInfoLines( infoFile ) );
            state.fileName = savedManifestFileName;
            state.lineNumber = savedManifestLineNumber;
        }

        // Remove info fields.

        result.setFields( result.getFields()
                                .stream()
                                .filter( field -> !field.getName().equalsIgnoreCase( INFO ) )
                                .collect( Collectors.toList() ) );
    }

    private ManifestFieldValue
    parseManifestLine( Path inputDir, String line )
    {

        if( line == null )
            return null;

        ++state.lineNumber;

        line = line.trim();

        if( line.isEmpty() )
            return null;

        String[] tokens = line.split("\\s+", 2);

        String fieldName = StringUtils.stripEnd( tokens[ 0 ].trim().toUpperCase(), ": " );
        String fieldValue = ( tokens.length == 2 ) ? tokens[ 1 ].trim() : null;

        if( fieldName.matches( "^[\\s]*(#|;|\\/\\/).*$" ) )
            return null;

        try
        {
            ManifestFieldDefinition fieldDefinition = Stream.concat( infoFields.stream(), fields.stream() )
                                                            .filter( field -> field.getName().equalsIgnoreCase( fieldName ) )
                                                            .findFirst()
                                                            .get();

            if( fieldValue != null )
            {
                ManifestFieldValue field = new ManifestFieldValue( fieldDefinition, fieldValue, createParseOrigin() );

                if( field.getDefinition().getType() == ManifestFieldType.FILE )
                {

                    // Validate file exists.

                    validateFileExists( inputDir, field );
                }

                return field;
            }

        } catch( NoSuchElementException ex )
        {
            error( WebinCliMessage.Manifest.UNKNOWN_FIELD_ERROR, fieldName );
        }

        return null;
    }


    private void
    validateManifest()
    {

        state.state = VALIDATE;

        // Validate min count.

        fields.stream()
              .filter( field -> field.getMinCount() > 0 )
              .forEach( minCountField -> {
                  if( result.getFields()
                            .stream()
                            .filter( field -> field.getName().equals( minCountField.getName() ) )
                            .count() < 1 )
                  {
                      error( WebinCliMessage.Manifest.MISSING_MANDATORY_FIELD_ERROR, minCountField.getName() );
                  }
              } );

        // Validate max count.

        fields.stream()
              .filter( field -> field.getMaxCount() > 0 )
              .forEach( maxCountField -> {
                  List<ManifestFieldValue> matchingFields = result.getFields()
                                                                  .stream()
                                                                  .filter( field -> field.getName().equals( maxCountField.getName() ) )
                                                                  .collect( Collectors.toList() );

                    if( matchingFields.size() > maxCountField.getMaxCount() )
                    {
                        error( WebinCliMessage.Manifest.TOO_MANY_FIELDS_ERROR,
                                maxCountField.getName(),
                                String.valueOf( maxCountField.getMaxCount() ) );
                    }
                } );


       // Validate and fix fields.

        for( ManifestFieldValue fieldValue : result.getFields() )
        {
            ManifestFieldDefinition field = fieldValue.getDefinition();
            ValidationResult validationResult = new ValidationResult( fieldValue.getOrigin() );

            for( ManifestFieldProcessor v : field.getFieldProcessors() )
            {
                ValidationMessage<Origin> m = v.process( fieldValue );
                if( null != m )
                {
                    validationResult.append( m );
                    
                    if( Severity.ERROR.equals( m.getSeverity() ) )
                        fieldValue.setValidFieldValueOrFileSuffix( false );
                }
            }

            result.getValidationResult().append( validationResult );
        }

        // Validate file count.

        validateFileCount();
    }


    protected abstract void
    processManifest();


    private void
    validateFileExists( Path inputDir, ManifestFieldValue field )
    {
        String fieldName = field.getName();
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
                error(WebinCliMessage.Manifest.INVALID_FILE_FIELD_ERROR, field.getOrigin(), fieldName, fieldValue);
                return;
            }

            validateFileCompression(field.getValue());
        }
        catch (Throwable ex) {
            error(WebinCliMessage.Manifest.INVALID_FILE_FIELD_ERROR, field.getOrigin(), fieldName, fieldValue);
        }
    }

    private void
    validateFileCount()
    {
        if( fileGroups == null || fileGroups.isEmpty() )
            return;

        Map<String, Long> fileCountMap = result.getFields()
                                               .stream()
                                               .filter( field -> field.getDefinition().getType().equals( ManifestFieldType.FILE ) )
                                               .collect( Collectors.groupingBy( ManifestFieldValue::getName, Collectors.counting() ) );

        if( fileCountMap == null || fileCountMap.isEmpty() )
        {
            error( WebinCliMessage.Manifest.NO_DATA_FILES_ERROR, getFileGroupText(fileGroups) );
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

        error( WebinCliMessage.Manifest.INVALID_FILE_GROUP_ERROR, getFileGroupText(fileGroups), "" );

    }

    private void validateFileCompression(String filePath) {
        if (filePath.endsWith(ManifestFileSuffix.GZIP_FILE_SUFFIX)) {
            try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(filePath))) {
            } catch (Exception e) {
                error(WebinCliMessage.Manifest.INVALID_FILE_COMPRESSION_ERROR, filePath, "gzip");
            }
        }
        else if (filePath.endsWith(ManifestFileSuffix.BZIP2_FILE_SUFFIX)) {
            try( BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(new FileInputStream(filePath))) {
            }
            catch (Exception e) {
                error(WebinCliMessage.Manifest.INVALID_FILE_COMPRESSION_ERROR, filePath, "bzip2");
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

        try
        {
            int value = Integer.valueOf( fieldValue );
            if( value <= 0 ) {
                error(WebinCliMessage.Manifest.INVALID_POSITIVE_INTEGER_ERROR, field.getOrigin(), field.getName(), fieldValue);
                return null;
            }
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error(WebinCliMessage.Manifest.INVALID_POSITIVE_INTEGER_ERROR, field.getOrigin(), field.getName(), fieldValue);
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

        try
        {
            float value = Float.valueOf( fieldValue );
            if( value <= 0 ) {
                error(WebinCliMessage.Manifest.INVALID_POSITIVE_FLOAT_ERROR, field.getOrigin(), field.getName(), fieldValue);
                return null;
            }
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error(WebinCliMessage.Manifest.INVALID_POSITIVE_FLOAT_ERROR, field.getOrigin(), field.getName(), fieldValue);
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


    public static List<String>
    readManifestLines(File file) {
        try {
            return Files.readAllLines(file.toPath());
        } catch (IOException ex) {
            throw WebinCliException.systemError(ex, WebinCliMessage.Manifest.READING_MANIFEST_FILE_ERROR.format(file.getPath()));
        }
    }

    private static List<String>
    readInfoLines(File file)
    {
        try {
            try (InputStream is = new GZIPInputStream(new FileInputStream(file))) {
                return readLines(is);
            } catch (IOException ioe) {
                try (InputStream is = new BZip2CompressorInputStream(new FileInputStream(file))) {
                    return readLines(is);
                } catch (IOException ie) {
                    return readLines(new FileInputStream(file));
                }
            }
        }
        catch (Exception ex) {
            throw WebinCliException.systemError(ex, WebinCliMessage.Manifest.READING_INFO_FILE_ERROR.format(file.getPath()));
        }
    }

    private static List<String>
    readLines(InputStream is )
    {
        return new BufferedReader( new InputStreamReader( is, StandardCharsets.UTF_8 ) ).lines().collect( Collectors.toList() );
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

    protected final void
    error(WebinCliMessage message, Object... arguments )
    {
        Origin origin = null;
        switch (state.state) {
            case PARSE:
                origin = createParseOrigin();
                break;
            case VALIDATE:
                origin = createValidateOrigin();
                break;
        }
        error(message, origin, arguments);
    }


    private void
    error(WebinCliMessage message, Origin origin, Object... arguments)
    {
        result.getValidationResult().append(WebinCliMessage.error(message, origin, arguments));
    }

    private Origin
    createParseOrigin()
    {
        return new DefaultOrigin("File name: " + state.fileName + ", line number: " + (spreadSheetLineNumber != null ? spreadSheetLineNumber : state.lineNumber));
    }


    private Origin
    createValidateOrigin()
    {
        return new DefaultOrigin("File name: " + state.fileName);
    }
}
