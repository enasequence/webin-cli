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

import uk.ac.ebi.embl.api.validation.*;

import static uk.ac.ebi.ena.manifest.ManifestReader.Fields.INFO;
import static uk.ac.ebi.ena.manifest.ManifestReader.ManifestReaderState.State.PARSE;
import static uk.ac.ebi.ena.manifest.ManifestReader.ManifestReaderState.State.VALIDATE;

public abstract class 
ManifestReader 
{

    public interface
    Fields {
        String INFO = "INFO";
    }

    private final List<ManifestFieldDefinition> infoFields = new ArrayList<ManifestFieldDefinition>()
    {
        {
            add( new ManifestFieldDefinition( INFO, ManifestFieldType.FILE, 0, 1 ) );
        }
    };

    private static final String MESSAGE_BUNDLE = "uk.ac.ebi.ena.manifest.ManifestReaderMessages";


    static
    {
        ValidationMessageManager.addBundle( MESSAGE_BUNDLE );
    }


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

    private final List<ManifestFieldDefinition> fields;
    private final Set<List<ManifestFileCount>> files;
    private ManifestReaderResult result;
    private ManifestReaderState state;

    public
    ManifestReader( List<ManifestFieldDefinition> fields )
    {
        this.fields = fields;
        this.files = null;
    }


    public
    ManifestReader( List<ManifestFieldDefinition> fields,
                    Set<List<ManifestFileCount>> files )
    {
        this.fields = fields;
        this.files = files;
    }


    public final Path
    getInputDir()
    {
        return state.inputDir;
    }

    
    public abstract String getName();
    public abstract String getDescription();

    
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
    readManifest( Path inputDir, File file )
    {
        state = new ManifestReaderState( inputDir, file.getPath() );
        result = new ManifestReaderResult();

        List<String> manifestLines;
        try
        {
            manifestLines = Files.readAllLines( file.toPath() );
        } catch( IOException ex )
        {
            error( "MANIFEST_ERROR_READING_MANIFEST_FILE", file.getPath() );
            return;
        }

        // Parse.

        parseManifest( inputDir, manifestLines );

        // Expand info fields.

        List<ManifestFieldValue> infoFields = result.getFields()
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
                error( "MANIFEST_ERROR_READING_INFO_FILE", infoFile.getPath() );
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

        result.setFields( result.getFields()
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
        state.state = PARSE;

        for( String line : lines )
        {
            ManifestFieldValue field = parseManifestLine( inputDir, line );
            if( null != field )
                result.getFields().add( field );
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
            error( "MANIFEST_UNKNOWN_FIELD", fieldName );
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
                      error( "MANIFEST_MISSING_MANDATORY_FIELD", minCountField.getName() );
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
                        error( "MANIFEST_TOO_MANY_FIELDS",
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
                    validationResult.append( m );
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
                error("MANIFEST_INVALID_FILE_FIELD", field.getOrigin(), fieldName, fieldValue);
                return;
            }

            validateFileCompression(field.getValue());
        }
        catch (Throwable ex) {
            error("MANIFEST_INVALID_FILE_FIELD", field.getOrigin(), fieldName, fieldValue);
        }
    }

    private void
    validateFileCount()
    {
        if( files == null || files.isEmpty() )
            return;

        Map<String, Long> fileCountMap = result.getFields()
                                               .stream()
                                               .filter( field -> field.getDefinition().getType().equals( ManifestFieldType.FILE ) )
                                               .collect( Collectors.groupingBy( ManifestFieldValue::getName, Collectors.counting() ) );

        if( fileCountMap == null || fileCountMap.isEmpty() )
        {
            error( "MANIFEST_ERROR_NO_DATA_FILES", getExpectedFileTypeList( files ) );
            return;
        }

        next:
        for (List<ManifestFileCount> expectedFileCountList : files) {
            for (ManifestFileCount expectedFileCount : expectedFileCountList) {
                if (fileCountMap.get(expectedFileCount.getFileType()) == null) {
                    if (expectedFileCount.getMinCount() != null && expectedFileCount.getMinCount() > 0) {
                        continue next; // Invalid because min is > 0.
                    }
                } else {
                    long manifestFileCount = fileCountMap.get(expectedFileCount.getFileType());
                    if ((expectedFileCount.getMaxCount() != null && expectedFileCount.getMaxCount() < manifestFileCount) ||
                        (expectedFileCount.getMinCount() != null && expectedFileCount.getMinCount() > manifestFileCount)) {
                        continue next; // Invalid because larger than max or smaller than min.
                    }
                }
            }

            for( String manifestFileType : fileCountMap.keySet() )
            {
                if( expectedFileCountList.stream()
                                         .filter( expectedFileCount -> expectedFileCount.getFileType().equals( manifestFileType ) )
                                         .count() < 1 )
                {
                    continue next; // Invalid because unmatched file type.
                }
            }

            return; // Valid
        }

        error( "MANIFEST_ERROR_INVALID_FILE_GROUP", getExpectedFileTypeList( files ), "" );

    }

    private void validateFileCompression(String filePath) {
        if (filePath.endsWith(ManifestFileSuffix.GZIP_FILE_SUFFIX)) {
            try (GZIPInputStream gz = new GZIPInputStream(new FileInputStream(filePath))) {
            } catch (Exception e) {
                error("MANIFEST_INVALID_FILE_COMPRESSION", filePath, "gzip");
            }
        }
        else if (filePath.endsWith(ManifestFileSuffix.BZIP2_FILE_SUFFIX)) {
            try( BZip2CompressorInputStream bz2 = new BZip2CompressorInputStream(new FileInputStream(filePath))) {
            }
            catch (Exception e) {
                error("MANIFEST_INVALID_FILE_COMPRESSION", filePath, "bzip2");
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
                error("MANIFEST_INVALID_POSITIVE_INTEGER", field.getOrigin(), field.getName(), fieldValue);
                return null;
            }
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error("MANIFEST_INVALID_POSITIVE_INTEGER", field.getOrigin(), field.getName(), fieldValue);
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
                error("MANIFEST_INVALID_POSITIVE_FLOAT", field.getOrigin(), field.getName(), fieldValue);
                return null;
            }
            return value;
        }
        catch( NumberFormatException nfe )
        {
            error("MANIFEST_INVALID_POSITIVE_FLOAT", field.getOrigin(), field.getName(), fieldValue);
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

    protected String
    getExpectedFileTypeList( Set<List<ManifestFileCount>> expectedFileCountSet )
    {
        return expectedFileCountSet.stream()
                                   .map( expectedFileCountList -> {
                                       StringBuilder str = new StringBuilder();
                                       str.append( "[" );
                                       String fileSeparator = "";
                                       for( ManifestFileCount expectedFileCount : expectedFileCountList )
                                       {
                                           String fileType = expectedFileCount.getFileType();
                                           str.append( fileSeparator );
                                           if( expectedFileCount.getMinCount() == expectedFileCount.getMaxCount() )
                                           {
                                               str.append( expectedFileCount.getMinCount() != null ? expectedFileCount.getMinCount() : 0);
                                           } else
                                           {
                                               if ( expectedFileCount.getMaxCount() != null ) {
                                                   str.append( expectedFileCount.getMinCount() != null ? expectedFileCount.getMinCount() : 0);
                                                   str.append( ".." );
                                                   str.append( expectedFileCount.getMaxCount());
                                               }
                                               else {
                                                   str.append( ">= " );
                                                   str.append( expectedFileCount.getMinCount() != null ? expectedFileCount.getMinCount() : 0);
                                               }
                                           }
                                           str.append( " \"" );
                                           str.append( fileType );
                                           str.append( "\" file(s)" );
                                           fileSeparator = ", ";
                                       }
                                       str.append( "]" );
                                       return str.toString();
                                   } )
                                   .collect( Collectors.joining( ", " ) );
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
    error( String key, String... params )
    {
        Origin origin = null;

        switch( state.state )
        {
            case PARSE:
                origin = createParseOrigin();
                break;
            case VALIDATE:
                origin = createValidateOrigin();
                break;
        }

        error( key, origin, params);
    }


    private void
    error(String key, Origin origin, String... params)
    {
        ValidationMessage<Origin> validationMessage = ValidationMessage.error( key, (Object[]) params );
        validationMessage.append( origin );
        result.getValidationResult().append( validationMessage );
    }


    private Origin
    createParseOrigin()
    {
        return new DefaultOrigin("File name: " + state.fileName + ", line number: " + state.lineNo);
    }


    private Origin
    createValidateOrigin()
    {
        return new DefaultOrigin("File name: " + state.fileName);
    }
}
