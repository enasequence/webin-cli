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
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import htsjdk.samtools.DefaultSAMRecordFactory;
import htsjdk.samtools.SAMFormatException;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamFiles;
import htsjdk.samtools.SamInputResource;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;
import htsjdk.samtools.cram.CRAMException;
import htsjdk.samtools.util.Log;
import htsjdk.samtools.util.Log.LogLevel;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.readtools.cram.ref.ENAReferenceSource;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.entity.Sample;
import uk.ac.ebi.ena.webin.cli.entity.Study;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsFile.ChecksumMethod;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.webin.cli.rawreads.refs.CramReferenceInfo;
import uk.ac.ebi.ena.webin.cli.reporter.ValidationMessageReporter;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

public class 
RawReadsWebinCli extends AbstractWebinCli<RawReadsManifest>
{   
    static 
    {
        System.setProperty( "samjdk.use_cram_ref_download", Boolean.TRUE.toString() );
    }
    
    private static final String RUN_XML = "run.xml";
    private static final String EXPERIMENT_XML = "experiment.xml";
    private static final String BAM_STAR = "*";

    private String studyId;
    private String sampleId;

    //TODO value should be estimated via validation
    private boolean is_paired;

    private static final Logger log = LoggerFactory.getLogger(RawReadsWebinCli.class);

    @Override
    public WebinCliContext getContext() {
        return WebinCliContext.reads;
    }

    @Override
    protected RawReadsManifest createManifestReader() {

        // Create manifest parser which will also set the sample and study fields.

        return new RawReadsManifest(
                isMetadataServiceActive(MetadataService.SAMPLE) ? new SampleProcessor(getParameters(), (Sample sample) -> this.sampleId = sample.getBiosampleId()) : null,
                isMetadataServiceActive(MetadataService.STUDY) ? new StudyProcessor(getParameters(), (Study study) -> this.studyId = study.getProjectId()) : null);
    }

    @Override
    protected void readManifest(Path inputDir, File manifestFile) {
        getManifestReader().readManifest( inputDir, manifestFile );

        if (StringUtils.isBlank(studyId)) {
            studyId = getManifestReader().getStudyId();
        }

        if (StringUtils.isBlank(sampleId)) {
            sampleId = getManifestReader().getSampleId();
        }
        
        setDescription( getManifestReader().getDescription() );
    }


    @Override public void
    validate() throws WebinCliException
    {
        if( !FileUtils.emptyDirectory(getValidationDir()) )
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(getValidationDir()));

        if( !FileUtils.emptyDirectory( getProcessDir() ) )
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(getProcessDir()));

        if( !FileUtils.emptyDirectory(getSubmitDir()) )
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(getSubmitDir()));

        boolean valid = true;
        AtomicBoolean paired = new AtomicBoolean();
        
        List<RawReadsFile> files = getManifestReader().getRawReadFiles();

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

        is_paired = paired.get();
        
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
            
            
            FastqScanner fs = new FastqScanner( getManifestReader().getPairingHorizon() );            
            ValidationResult vr = fs.checkFiles( files.toArray( new RawReadsFile[ files.size() ] ) );
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
        boolean valid = true;
        for( RawReadsFile rf : files )
        {
            long read_no = 0;
            long reads_cnt = 0;

            try (ValidationMessageReporter reporter = new ValidationMessageReporter( getReportFile( rf.getFilename() ))) {
                try {
                    String msg = String.format("Processing file %s\n", rf.getFilename());
                    log.info(msg);
                    ENAReferenceSource reference_source = new ENAReferenceSource();
                    reference_source.setLoggerWrapper(new ENAReferenceSource.LoggerWrapper() {
                        @Override
                        public void
                        error(Object... messageParts) {
                            reporter.write(Severity.ERROR, null == messageParts ? "null" : String.valueOf(Arrays.asList(messageParts)));
                        }

                        @Override
                        public void
                        warn(Object... messageParts) {
                            reporter.write(Severity.WARNING, null == messageParts ? "null" : String.valueOf(Arrays.asList(messageParts)));
                        }

                        @Override
                        public void
                        info(Object... messageParts) {
                            reporter.write(Severity.INFO, null == messageParts ? "null" : String.valueOf(Arrays.asList(messageParts)));
                        }
                    });

                    reporter.write(Severity.INFO, "REF_PATH  " + reference_source.getRefPathList());
                    reporter.write(Severity.INFO, "REF_CACHE " + reference_source.getRefCacheList());


                    File file = new File(rf.getFilename());
                    Log.setGlobalLogLevel(LogLevel.ERROR);
                    SamReaderFactory.setDefaultValidationStringency(ValidationStringency.SILENT);
                    SamReaderFactory factory = SamReaderFactory.make();
                    factory.enable(SamReaderFactory.Option.DONT_MEMORY_MAP_INDEX);
                    factory.validationStringency(ValidationStringency.SILENT);
                    factory.referenceSource(reference_source);
                    factory.samRecordFactory(DefaultSAMRecordFactory.getInstance());
                    SamInputResource ir = SamInputResource.of(file);
                    File indexMaybe = SamFiles.findIndex(file);
                    reporter.write(Severity.INFO, "proposed index: " + indexMaybe);

                    if (null != indexMaybe)
                        ir.index(indexMaybe);

                    SamReader reader = factory.open(ir);

                    for (SAMRecord record : reader) {
                        read_no++;
                        //do not load supplementary reads
                        if (record.isSecondaryOrSupplementary())
                            continue;

                        if (record.getDuplicateReadFlag())
                            continue;

                        if (record.getReadString().equals(BAM_STAR) && record.getBaseQualityString().equals(BAM_STAR))
                            continue;

                        if (record.getReadBases().length != record.getBaseQualities().length) {
                            ValidationMessage<Origin> validationMessage = ValidationMessageReporter.createValidationMessage(Severity.ERROR, "Mismatch between length of read bases and qualities",
                                    new DefaultOrigin(String.format("%s:%d", rf.getFilename(), read_no)));

                            reporter.write(validationMessage);
                            valid = false;
                        }

                        paired.compareAndSet(false, record.getReadPairedFlag());
                        reads_cnt++;
                        if (0 == reads_cnt % 1000)
                            logProcessedReadNumber(reads_cnt);
                    }

                    logProcessedReadNumber(reads_cnt);

                    reader.close();

                    reporter.write(Severity.INFO, "Valid reads count: " + reads_cnt);
                    reporter.write(Severity.INFO, "LibraryLayout: " + (paired.get() ? "PAIRED" : "SINGLE"));

                    if (0 == reads_cnt) {
                        reporter.write(Severity.ERROR, "File contains no valid reads");
                        valid = false;
                    }

                } catch (SAMFormatException | CRAMException e) {
                    reporter.write(Severity.ERROR, e.getMessage());
                    valid = false;

                } catch (IOException ex) {
                    throw WebinCliException.systemError(ex);
                }
            }
        }
        return valid;
    }

    
    @Override public void
    prepareSubmissionBundle() throws WebinCliException
    {
        try
        {
            List<RawReadsFile> files = getManifestReader().getRawReadFiles();
            
            List<File> uploadFileList = files.stream().map( e -> new File( e.getFilename() ) ).collect( Collectors.toList() );
            Path uploadDir = getUploadRoot().resolve( Paths.get( String.valueOf( getContext() ), WebinCli.getSafeOutputDir( getName() ) ) );
            files.forEach( e -> e.setChecksumMethod( ChecksumMethod.MD5 ) );
            files.forEach( e -> {
                try
                {
                    e.setChecksum( FileUtils.calculateDigest( String.valueOf( e.getChecksumMethod() ), new File( e.getFilename() ) ) );
                } catch( NoSuchAlgorithmException | IOException e1 )
                {
                    throw new RuntimeException( e1 );
                }
            } );
            List<Element> eList = files.stream()
                                       .sequential()
                                       .map( e -> e.toElement( "FILE", uploadDir ) )
                                       .collect( Collectors.toList() );
    
            //do something
            String experiment_ref = getAlias();
            
            String e_xml = createExperimentXml( experiment_ref, getParameters().getCenterName(), is_paired, getManifestReader().getDescription() );
            String r_xml = createRunXml( eList, experiment_ref, getParameters().getCenterName() );
            
            Path runXmlFile = getSubmitDir().toPath().resolve( RUN_XML );
            Path experimentXmlFile = getSubmitDir().toPath().resolve( EXPERIMENT_XML );
            
            Files.write( runXmlFile, r_xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            Files.write( experimentXmlFile, e_xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
            
            setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
                                                       uploadDir.toString(), 
                                                       uploadFileList,
                                                       Arrays.asList( new SubmissionXMLFile( SubmissionXMLFileType.EXPERIMENT, experimentXmlFile.toFile(), FileUtils.calculateDigest( "MD5", experimentXmlFile.toFile() ) ), 
                                                                      new SubmissionXMLFile( SubmissionXMLFileType.RUN, runXmlFile.toFile(), FileUtils.calculateDigest( "MD5", runXmlFile.toFile() ) ) ),
                                                       getParameters().getCenterName(),
                                                       FileUtils.calculateDigest( "MD5", getParameters().getManifestFile() ) ) );
        } catch( NoSuchAlgorithmException | IOException ex )
        {
            throw WebinCliException.systemError( ex );
        }
    }

/*
    <RUN_SET>
    <RUN alias="" center_name="" run_center="blah">
        <EXPERIMENT_REF refname="" />
        <DATA_BLOCK>
            <FILES>
                <FILE filename="test_forward.fastq.gz" filetype="fastq" checksum="5aeca824118be49da0982bef9b57e689" checksum_method="MD5" quality_scoring_system="phred" ascii_offset="!" quality_encoding="ascii">
                    <READ_LABEL>F</READ_LABEL>
                </FILE>
                <FILE filename="test_reverse.fastq.gz" filetype="fastq" checksum="216e1803c0f22825caa58aa3622a0be5" checksum_method="MD5" quality_scoring_system="phred" ascii_offset="!" quality_encoding="ascii">
                    <READ_LABEL>R</READ_LABEL>
                </FILE>
            </FILES>
        </DATA_BLOCK>
    </RUN>
    </RUN_SET>
*/

    private String
    createExperimentXml(String experiment_ref, String centerName, boolean is_paired, String design_description)
    {
        String instrument_model = getManifestReader().getInstrument();
        design_description = StringUtils.isBlank( design_description ) ? "unspecified" : design_description;
        String library_strategy  = getManifestReader().getLibraryStrategy();
        String library_source    = getManifestReader().getLibrarySource();
        String library_selection = getManifestReader().getLibrarySelection();
        String library_name      = getManifestReader().getLibraryName();

        String platform  = getManifestReader().getPlatform();
        Integer insert_size = getManifestReader().getInsertSize();
                
        try 
        {
            String full_name = WebinCliContext.reads.getXmlTitle( getName() );
            Element experimentSetE = new Element( "EXPERIMENT_SET" );
            Element experimentE = new Element( "EXPERIMENT" );
            experimentSetE.addContent( experimentE );
            
            Document doc = new Document( experimentSetE );
            experimentE.setAttribute( "alias", experiment_ref );
            
            if( null != centerName && !centerName.isEmpty() )
                experimentE.setAttribute( "center_name", centerName );
            
            experimentE.addContent( new Element( "TITLE" ).setText( full_name ) );
            
            Element studyRefE = new Element( "STUDY_REF" );
            experimentE.addContent( studyRefE );
            studyRefE.setAttribute( "accession", studyId );
  
            Element designE = new Element( "DESIGN" );
            experimentE.addContent( designE );
            
            Element designDescriptionE = new Element( "DESIGN_DESCRIPTION" );
            designDescriptionE.setText( design_description );
            designE.addContent( designDescriptionE );
            
            Element sampleDescriptorE = new Element( "SAMPLE_DESCRIPTOR" );
            sampleDescriptorE.setAttribute( "accession", sampleId );

            designE.addContent( sampleDescriptorE );

            Element libraryDescriptorE = new Element( "LIBRARY_DESCRIPTOR" );
            designE.addContent( libraryDescriptorE );
            
            if( null != library_name )
            {
                Element libraryNameE = new Element( "LIBRARY_NAME" );
                libraryNameE.setText( library_name );
                libraryDescriptorE.addContent( libraryNameE );
            }   
            
            Element libraryStrategyE = new Element( "LIBRARY_STRATEGY" );
            libraryStrategyE.setText( library_strategy );
            libraryDescriptorE.addContent( libraryStrategyE );
            
            Element librarySourceE = new Element( "LIBRARY_SOURCE" );
            librarySourceE.setText( library_source );
            libraryDescriptorE.addContent( librarySourceE );
            
            Element librarySelectionE = new Element( "LIBRARY_SELECTION" );
            librarySelectionE.setText( library_selection );
            libraryDescriptorE.addContent( librarySelectionE );

            Element libraryLayoutE = new Element( "LIBRARY_LAYOUT" );
            if( !is_paired )
            {
                libraryLayoutE.addContent( new Element( "SINGLE" ) );
            } else
            {
                Element pairedE = new Element( "PAIRED" );
                libraryLayoutE.addContent( pairedE );
                
                if( null != insert_size )
                    pairedE.setAttribute( "NOMINAL_LENGTH", String.valueOf( insert_size ) );
            }

            libraryDescriptorE.addContent( libraryLayoutE );
            
            Element platformE = new Element( "PLATFORM" );
            experimentE.addContent( platformE );
            
            Element platformRefE = new Element( platform );
            platformE.addContent( platformRefE );
            Element instrumentModelE = new Element( "INSTRUMENT_MODEL" );
            instrumentModelE.setText( instrument_model );
            platformRefE.addContent( instrumentModelE );
            
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat( Format.getPrettyFormat() );
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output( doc, stringWriter );
            return stringWriter.toString();
            
        } catch( IOException ex )
        {
            throw WebinCliException.systemError( ex );
        }
    }
    
    
    String
    createRunXml( List<Element> fileList, String experiment_ref, String centerName  ) 
    {
        try 
        {
            String full_name = WebinCliContext.reads.getXmlTitle( getName() );
            Element runSetE = new Element( "RUN_SET" );
            Element runE = new Element( "RUN" );
            runSetE.addContent( runE );
            
            Document doc = new Document( runSetE );
            runE.setAttribute( "alias", getAlias() );
            
            if( null != centerName && !centerName.isEmpty() )
                runE.setAttribute( "center_name", centerName );
            
            runE.addContent( new Element( "TITLE" ).setText( full_name ) );
            Element experimentRefE = new Element( "EXPERIMENT_REF" );
            runE.addContent( experimentRefE );
            experimentRefE.setAttribute( "refname", experiment_ref );
            
            Element dataBlockE = new Element( "DATA_BLOCK" );
            runE.addContent( dataBlockE );
            Element filesE = new Element( "FILES" );
            dataBlockE.addContent( filesE );
            
            for( Element e: fileList )
                filesE.addContent( e );
            
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat( Format.getPrettyFormat() );
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output( doc, stringWriter );
            return stringWriter.toString();
            
        } catch( IOException ex )
        {
            throw WebinCliException.systemError( ex );
        }
    }

    
    private void 
    logProcessedReadNumber( long count )
    {
        String msg = String.format( "\rProcessed %16d read(s)", count );
        System.out.print( msg );
        System.out.flush();
    }
}
