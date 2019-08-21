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
package uk.ac.ebi.ena.webin.cli.rawreads;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.cram.CRAMException;

import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.BamScanner;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.FastqScanner;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.ScannerMessage;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.ScannerMessage.ScannerErrorMessage;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.refs.CramReferenceInfo;
import uk.ac.ebi.ena.webin.cli.*;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.reporter.ValidationMessageReporter;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.xml.XmlCreator;

public class 
RawReadsWebinCli extends AbstractWebinCli<ReadsManifest>
{   
    private static final Logger log = LoggerFactory.getLogger(RawReadsWebinCli.class);

    public RawReadsWebinCli(WebinCliParameters parameters) {
        this(parameters, RawReadsManifestReader.create(ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory( parameters )), new RawReadsXmlCreator());
    }

    public RawReadsWebinCli(WebinCliParameters parameters, ManifestReader<ReadsManifest> manifestReader, XmlCreator<ReadsManifest> xmlCreator) {
        super(WebinCliContext.reads, parameters, manifestReader, xmlCreator);
    }

    @Override
    public void validateSubmissionForContext()
    {
        boolean valid = true;
        AtomicBoolean paired = new AtomicBoolean();

        List<RawReadsFile> files = createReadFiles();

        for (RawReadsFile rf : files) {
            if (Filetype.fastq.equals(rf.getFiletype())) {
                valid = readFastqFile(files, paired);
            } else if (Filetype.bam.equals(rf.getFiletype())) {
                valid = readBamFile(files, paired);
            } else if (Filetype.cram.equals(rf.getFiletype())) {
                valid = readCramFile(files, paired);
            } else {
                throw WebinCliException.systemError(WebinCliMessage.Cli.UNSUPPORTED_FILETYPE_ERROR.format(rf.getFiletype().name()));
            }
            break;
        }

        getManifestReader().getManifest().setPaired(paired.get());
        
        if( !valid )
            throw WebinCliException.validationError("");
    }

    
    private boolean
    readCramFile( List<RawReadsFile> files, AtomicBoolean paired )
    {
        boolean valid = true;
        CramReferenceInfo cri = new CramReferenceInfo();
        for( RawReadsFile rf : files )
        {
            try (ValidationMessageReporter reporter = new ValidationMessageReporter( getReportFile( rf.getFilename() )))
            {
                try {
                    Map<String, Boolean> ref_set = cri.confirmFileReferences( new File( rf.getFilename() ) );
                    if( !ref_set.isEmpty() && ref_set.containsValue( Boolean.FALSE ) )
                    {
                        reporter.write(Severity.ERROR, "Unable to find reference sequence(s) from the CRAM reference registry: " +
                                ref_set.entrySet()
                                        .stream()
                                        .filter(e -> !e.getValue())
                                        .map(e -> e.getKey())
                                        .collect(Collectors.toList()));
                        valid = false;
                    }

                } catch( IOException ioe )
                {
                    reporter.write( Severity.ERROR, ioe.getMessage() );
                    valid = false;
                }
            }
        }

        return valid && readBamFile( files, paired );
    }


    private boolean
    readFastqFile( List<RawReadsFile> files, AtomicBoolean paired )
    {
        try
        {
            if( files.size() > 2 )
            {
                String msg = "Unable to validate unusual amount of files: " + files;
                reportToFileList( files, msg );
                throw WebinCliException.validationError( msg );
            }
            
            
            FastqScanner fs = new FastqScanner(  getManifestReader().getManifest().getPairingHorizon() )
            {
                @Override protected void 
                logProcessedReadNumber( long count )
                {
                    RawReadsWebinCli.this.logProcessedReadNumber( count );
                }
                
                
                @Override protected void 
                logFlushMsg( String msg )
                {
                    RawReadsWebinCli.this.logFlushMsg( msg );
                    
                }
            };            
            
            List<ScannerMessage> sm_list = fs.checkFiles( files.toArray( new RawReadsFile[ files.size() ] ) );
            ValidationResult vr = new ValidationResult();
            vr.append( sm_list.stream().map( e -> fMsg( e ) ).collect( Collectors.toList() ) );
            paired.set( fs.getPaired() );
            files.forEach(rf -> {
                try (ValidationMessageReporter reporter = new ValidationMessageReporter( getReportFile( rf.getFilename() ))) {
                    reporter.write(vr);
                }
            });
            return vr.isValid();

        } catch( Throwable ex )
        {
            throw WebinCliException.systemError( ex, "Unable to validate file(s): " + files );
        }
    }
    
    
    private ValidationMessage<Origin>
    fMsg( ScannerMessage sm )
    {   
        Severity severity = sm instanceof ScannerErrorMessage ? Severity.ERROR : Severity.INFO;
        
        ValidationMessage<Origin> result = new ValidationMessage<>( severity, ValidationMessage.NO_KEY );
        result.setMessage( sm.getMessage() );
        if( null != sm.getOrigin() )
            result.append( new DefaultOrigin(  sm.getOrigin() ) );
        
        return result;
    }
    
    
    private void 
    reportToFileList( List<RawReadsFile> files, String msg )
    {
        for( RawReadsFile rf : files )
        {
            try (ValidationMessageReporter reporter = new ValidationMessageReporter( getReportFile( rf.getFilename() ))) {
                reporter.write( Severity.ERROR, msg );
            }
        }
    }


    private boolean
    readBamFile( List<RawReadsFile> files, AtomicBoolean paired ) throws WebinCliException
    {
        BamScanner scanner = new BamScanner() 
        {
            @Override protected void 
            logProcessedReadNumber( long count )
            {
                RawReadsWebinCli.this.logProcessedReadNumber( count );
            }
        };
        
        
        boolean valid = true;
        for( RawReadsFile rf : files )
        {
            try( ValidationMessageReporter reporter = new ValidationMessageReporter( getReportFile( rf.getFilename() ) ) ) 
            {
                try 
                {
                    String msg = String.format( "Processing file %s\n", rf.getFilename() );
                    log.info( msg );
                    
                    List<ScannerMessage> list = Filetype.cram == rf.getFiletype() ? scanner.readCramFile( rf, paired ) : scanner.readBamFile( rf, paired );
                    List<ValidationMessage<Origin>> mv_list = list.stream().sequential().map( sm ->fMsg( sm ) ).collect( Collectors.toList() );
                    mv_list.stream().forEachOrdered( m -> reporter.write( m ) );
                    valid = new ValidationResult().append( mv_list ).isValid();
                } catch( SAMFormatException | CRAMException e )
                {
                    reporter.write( Severity.ERROR, e.getMessage() );
                    valid = false;

                } catch( IOException ex )
                {
                    throw WebinCliException.systemError( ex );
                }
            }
        }
        return valid;
    }

    private List<RawReadsFile> createReadFiles()
    {
        ReadsManifest manifest = getManifestReader().getManifest();

        RawReadsFile.AsciiOffset asciiOffset = null;
        RawReadsFile.QualityScoringSystem qualityScoringSystem = null;

        if( manifest.getQualityScore() != null )
        {
            switch( manifest.getQualityScore() )
            {
                case RawReadsManifestReader.QUALITY_SCORE_PHRED_33:
                    asciiOffset = RawReadsFile.AsciiOffset.FROM33;
                    qualityScoringSystem = RawReadsFile.QualityScoringSystem.phred;
                    break;
                case RawReadsManifestReader.QUALITY_SCORE_PHRED_64:
                    asciiOffset = RawReadsFile.AsciiOffset.FROM64;
                    qualityScoringSystem = RawReadsFile.QualityScoringSystem.phred;
                    break;
                case RawReadsManifestReader.QUALITY_SCORE_LOGODDS:
                    asciiOffset = null;
                    qualityScoringSystem = RawReadsFile.QualityScoringSystem.log_odds;
                    break;
            }
        }
        List<RawReadsFile> files = getManifestReader().getManifest().files().get()
                .stream()
                .map( file -> createReadFile( getParameters().getInputDir().toPath(), file) )
                .collect( Collectors.toList() );

        // Set FASTQ quality scoring system and ascii offset.

        for( RawReadsFile f : files )
        {
            if( f.getFiletype().equals( Filetype.fastq ) )
            {
                if( qualityScoringSystem != null )
                    f.setQualityScoringSystem( qualityScoringSystem );
                if( asciiOffset != null )
                    f.setAsciiOffset( asciiOffset );
            }
        }

        return files;
    }

    public static RawReadsFile
    createReadFile( Path inputDir, SubmissionFile<ReadsManifest.FileType> file )
    {
        RawReadsFile f = new RawReadsFile();
        f.setInputDir( inputDir );
        f.setFiletype( Filetype.valueOf( file.getFileType().name().toLowerCase() ) );

        String fileName = file.getFile().getPath();

        if( !Paths.get( fileName ).isAbsolute() )
            f.setFilename( inputDir.resolve( Paths.get( fileName ) ).toString() );
        else
            f.setFilename( fileName );

        return f;
    }



    
    private void 
    logProcessedReadNumber( long count )
    {
        String msg = String.format( "\rProcessed %16d read(s)", count );
        logFlushMsg( msg );
    }

    
    private void
    logFlushMsg( String msg )
    {
        System.out.print( msg );
        System.out.flush();
    }
}
