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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile.AsciiOffset;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile.Filetype;
import uk.ac.ebi.ena.readtools.webin.cli.rawreads.RawReadsFile.QualityScoringSystem;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.SampleProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.metadata.StudyProcessor;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;
import uk.ac.ebi.ena.webin.cli.validator.reference.Study;

public class
RawReadsManifestReader extends ManifestReader {

    public interface Field {
        String NAME = "NAME";
        String STUDY = "STUDY";
        String SAMPLE = "SAMPLE";
        String PLATFORM = "PLATFORM";
        String INSTRUMENT = "INSTRUMENT";
        String DESCRIPTION = "DESCRIPTION";
        String LIBRARY_SOURCE = "LIBRARY_SOURCE";
        String LIBRARY_SELECTION = "LIBRARY_SELECTION";
        String LIBRARY_STRATEGY = "LIBRARY_STRATEGY";
        String LIBRARY_CONSTRUCTION_PROTOCOL = "LIBRARY_CONSTRUCTION_PROTOCOL";
        String LIBRARY_NAME = "LIBRARY_NAME";
        String INSERT_SIZE = "INSERT_SIZE";
        String QUALITY_SCORE = "QUALITY_SCORE";
        String __HORIZON = "__HORIZON";
        String FASTQ = "FASTQ";
        String BAM = "BAM";
        String CRAM = "CRAM";
    }

    public interface Description {
        String NAME = "Unique sequencing experiment name";
        String STUDY = "Study accession or name";
        String SAMPLE = "Sample accession or name";
        String PLATFORM = "Sequencing platform";
        String INSTRUMENT = "Sequencing instrument";
        String DESCRIPTION = "Experiment description";
        String LIBRARY_SOURCE = "Source material";
        String LIBRARY_SELECTION = "Method used to select or enrich the source material";
        String LIBRARY_STRATEGY = "Sequencing technique";
        String LIBRARY_CONSTRUCTION_PROTOCOL = "Protocol used to construct the sequencing library";
        String LIBRARY_NAME = "Library name";
        String INSERT_SIZE = "Insert size for paired reads";
        String QUALITY_SCORE = "";
        String __HORIZON = "";
        String FASTQ = "Fastq file";
        String BAM = "BAM file";
        String CRAM = "CRAM file";
    }


    private final static String INSTRUMENT_UNSPECIFIED = "unspecified";
    private final static String QUALITY_SCORE_PHRED_33 = "PHRED_33";
    private final static String QUALITY_SCORE_PHRED_64 = "PHRED_64";
    private final static String QUALITY_SCORE_LOGODDS = "LOGODDS";

    public final static ManifestCVList CV_INSTRUMENT = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/rawreads/instrument.properties"));
    public final static ManifestCVList CV_PLATFORM = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/rawreads/platform.properties"));
    public final static ManifestCVList CV_SELECTION = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/rawreads/selection.properties"));
    public final static ManifestCVList CV_SOURCE = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/rawreads/source.properties"));
    public final static ManifestCVList CV_STRATEGY = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/rawreads/strategy.properties"));
    public final static ManifestCVList CV_QUALITY_SCORE = new ManifestCVList(
            QUALITY_SCORE_PHRED_33,
            QUALITY_SCORE_PHRED_64,
            QUALITY_SCORE_LOGODDS
    );

    private String name = null;
    private String description = null;
    private String study_id = null;
    private String sample_id = null;
    private String platform = null;
    private Integer insert_size;
    private Integer pairing_horizon = 500_000_000;
    private String library_construction_protocol;
    private String library_name;
    private String instrument;
    private String library_source;
    private String library_selection;
    private String library_strategy;
    private QualityScoringSystem qualityScoringSystem;
    private AsciiOffset asciiOffset;
    private List<RawReadsFile> files;

    // TODO remove
    private final SampleProcessor sampleProcessor;
    private final StudyProcessor studyProcessor;

    public static RawReadsManifestReader create(ManifestReaderParameters parameters, MetadataProcessorFactory factory) {
        return new RawReadsManifestReader(
                parameters,
               factory.createSampleProcessor(),
               factory.createStudyProcessor());
    }

    private RawReadsManifestReader(
            ManifestReaderParameters parameters,
            SampleProcessor sampleProcessor,
            StudyProcessor studyProcessor)
    {
        super(parameters,
                // Fields.
                new ManifestFieldDefinition.Builder()
                       .meta().required().name(Field.NAME).desc(Description.NAME).and()
                       .meta().required().name(Field.STUDY).desc(Description.STUDY).processor(studyProcessor).and()
                       .meta().required().name(Field.SAMPLE).desc(Description.SAMPLE).processor(sampleProcessor).and()
                       .meta().optional().name(Field.DESCRIPTION).desc(Description.DESCRIPTION).and()
                       .meta().optional().requiredInSpreadsheet().name(Field.INSTRUMENT).desc(Description.INSTRUMENT).processor(new CVFieldProcessor(CV_INSTRUMENT)).and()
                       .meta().optional().notInSpreadsheet().name(Field.PLATFORM).desc(Description.PLATFORM).processor(new CVFieldProcessor(CV_PLATFORM)).and()
                       .meta().required().name(Field.LIBRARY_SOURCE).desc(Description.LIBRARY_SOURCE).processor(new CVFieldProcessor(CV_SOURCE)).and()
                       .meta().required().name(Field.LIBRARY_SELECTION).desc(Description.LIBRARY_SELECTION).processor(new CVFieldProcessor(CV_SELECTION)).and()
                       .meta().required().name(Field.LIBRARY_STRATEGY).desc(Description.LIBRARY_STRATEGY).processor(new CVFieldProcessor(CV_STRATEGY)).and()
                       .meta().optional().name(Field.LIBRARY_CONSTRUCTION_PROTOCOL).desc(Description.LIBRARY_CONSTRUCTION_PROTOCOL).and()
                       .meta().optional().name(Field.LIBRARY_NAME).desc(Description.LIBRARY_NAME).and()
                       .meta().optional().name(Field.INSERT_SIZE).desc(Description.INSERT_SIZE).and()
                       .file().optional(2).name(Field.FASTQ).desc(Description.FASTQ).processor(getFastqProcessors()).and()
                       .file().optional().name(Field.BAM).desc(Description.BAM).processor(getBamProcessors()).and()
                       .file().optional().name(Field.CRAM).desc(Description.CRAM).processor(getCramProcessors()).and()
                       .meta().optional().notInSpreadsheet().name(Field.QUALITY_SCORE).desc(Description.QUALITY_SCORE).processor(new CVFieldProcessor(CV_QUALITY_SCORE)).and()
                       .meta().optional().notInSpreadsheet().name(Field.__HORIZON).desc(Description.__HORIZON).build()
                ,
                // File groups.
                new ManifestFileCount.Builder()
                        .group()
                        .required(Field.FASTQ, 2)
                        .and().group()
                        .required(Field.CRAM)
                        .and().group()
                        .required(Field.BAM)
                        .build()
        );
        this.sampleProcessor = sampleProcessor;
        this.studyProcessor = studyProcessor;
    }

    // TODO remove
    public void setSampleProcessorCallback(SampleProcessor.Callback<Sample> sampleProcessorCallback) {
        if (sampleProcessor != null) {
            sampleProcessor.setCallback(sampleProcessorCallback);
        }
    }

    // TODO remove
    public void setStudyProcessorCallback(StudyProcessor.Callback<Study> studyProcessorCallback) {
        if (studyProcessor != null) {
            studyProcessor.setCallback(studyProcessorCallback);
        }
    }

    private static ManifestFieldProcessor[] getFastqProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor(ManifestFileSuffix.GZIP_OR_BZIP_FILE_SUFFIX)};
    }

    private static ManifestFieldProcessor[] getBamProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor(ManifestFileSuffix.BAM_FILE_SUFFIX)};
    }

    private static ManifestFieldProcessor[] getCramProcessors() {
        return new ManifestFieldProcessor[]{
                new ASCIIFileNameProcessor(),
                new FileSuffixProcessor(ManifestFileSuffix.CRAM_FILE_SUFFIX)};
    }

    @Override public String
    getName()
    {
        return name;
    }


    public String
    getStudyId()
    {
        return study_id;
    }


    public String
    getSampleId()
    {
        return sample_id;
    }


    public String
    getInstrument()
    {
        return instrument;
    }


    public String
    getPlatform()
    {
        return platform;
    }


    public Integer
    getInsertSize()
    {
        return insert_size;
    }


    public String
    getLibrarySource()
    {
        return library_source;
    }


    public String
    getLibrarySelection()
    {
        return library_selection;
    }


    public String
    getLibraryStrategy()
    {
        return library_strategy;
    }


    public String
    getLibraryConstructionProtocol()
    {
        return library_construction_protocol;
    }


    public String
    getLibraryName()
    {
        return library_name;
    }


    public Integer
    getPairingHorizon()
    {
        return pairing_horizon;
    }


    public List<RawReadsFile>
    getRawReadFiles()
    {
        return files;
    }


    @Override public void
    processManifest()
    {
        name = getResult().getValue( Field.NAME );
        study_id = getResult().getValue( Field.STUDY );
        sample_id = getResult().getValue( Field.SAMPLE );
        description = getResult().getValue( Field.DESCRIPTION );
        
        if (getResult().getCount(Field.INSTRUMENT) > 0 &&
            getResult().getField(Field.INSTRUMENT).isValidFieldValueOrFileSuffix())
            instrument = getResult().getValue(Field.INSTRUMENT);

        if (getResult().getCount(Field.PLATFORM) > 0 &&
            getResult().getField(Field.PLATFORM).isValidFieldValueOrFileSuffix())
            platform = getResult().getValue(Field.PLATFORM);

        insert_size = getAndValidatePositiveInteger(getResult().getField(Field.INSERT_SIZE));

        if (getResult().getCount(Field.LIBRARY_SOURCE) > 0 &&
            getResult().getField(Field.LIBRARY_SOURCE).isValidFieldValueOrFileSuffix())
            library_source = getResult().getValue(Field.LIBRARY_SOURCE);

        if (getResult().getCount(Field.LIBRARY_SELECTION) > 0 &&
            getResult().getField(Field.LIBRARY_SELECTION).isValidFieldValueOrFileSuffix())
            library_selection = getResult().getValue(Field.LIBRARY_SELECTION);

        if (getResult().getCount(Field.LIBRARY_STRATEGY) > 0 &&
            getResult().getField(Field.LIBRARY_STRATEGY).isValidFieldValueOrFileSuffix())
            library_strategy = getResult().getValue(Field.LIBRARY_STRATEGY);

        library_construction_protocol = getResult().getValue(Field.LIBRARY_CONSTRUCTION_PROTOCOL);
        library_name = getResult().getValue(Field.LIBRARY_NAME);

        if( getResult().getValue( Field.QUALITY_SCORE ) != null )
        {
            switch( getResult().getValue( Field.QUALITY_SCORE ) )
            {
            case QUALITY_SCORE_PHRED_33:
                asciiOffset = AsciiOffset.FROM33;
                qualityScoringSystem = QualityScoringSystem.phred;
                break;
            case QUALITY_SCORE_PHRED_64:
                asciiOffset = AsciiOffset.FROM64;
                qualityScoringSystem = QualityScoringSystem.phred;
                break;
            case QUALITY_SCORE_LOGODDS:
                asciiOffset = null;
                qualityScoringSystem = QualityScoringSystem.log_odds;
                break;
            }
        }

        if (getResult().getCount(Field.__HORIZON) > 0)
            pairing_horizon = getAndValidatePositiveInteger(getResult().getField(Field.__HORIZON));

        processInstrumentAndPlatform();
        processFiles();
    }


    private void
    processInstrumentAndPlatform()
    {

        if( null == platform && ( null == instrument || instrument.equals(INSTRUMENT_UNSPECIFIED) ) )
        {
            error(WebinCliMessage.Manifest.MISSING_PLATFORM_AND_INSTRUMENT_ERROR,
                    String.join(", ", CV_PLATFORM.keyList()),
                    String.join(", ", CV_INSTRUMENT.keyList()));
        }

        if( instrument != null )
        {
            // Set platform.

            String platforms = CV_INSTRUMENT.getValue( instrument );
            if( StringUtils.isBlank( platforms ) )
            {
                error(WebinCliMessage.Manifest.MISSING_PLATFORM_FOR_INSTRUMENT_ERROR, instrument);
            }

            String[] platformList = platforms.split( "[;,]" );

            if( 1 == platformList.length )
            {
                platform = CV_PLATFORM.getKey( platformList[ 0 ] );
            } else if(Stream.of( platformList ).noneMatch(e -> e.equals( platform ) ))
            {
                error( WebinCliMessage.Manifest.INVALID_PLATFORM_FOR_INSTRUMENT_ERROR,
                        StringUtils.isBlank( platform ) ? "is not defined" : platform + " is not supported",
                        instrument,
                        CV_INSTRUMENT.getValue( instrument ) );
            }
        } else
        {
            instrument = INSTRUMENT_UNSPECIFIED;
        }
    }


    private void
    processFiles()
    {
        files = getResult().getFields().stream()
                .filter( field -> field.getDefinition().getType() == ManifestFieldType.FILE )
                .map( field -> createReadFile( getInputDir(), field ) )
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
    }


    static RawReadsFile
    createReadFile( Path inputDir, ManifestFieldValue field )
    {
        assert( field.getDefinition().getType() == ManifestFieldType.FILE );

        RawReadsFile f = new RawReadsFile();
        f.setInputDir( inputDir );
        f.setFiletype( Filetype.valueOf( field.getName().toLowerCase() ) );

        String fileName = field.getValue();
        if( !Paths.get( fileName ).isAbsolute() )
            f.setFilename( inputDir.resolve( Paths.get( fileName ) ).toString() );
        else
            f.setFilename( fileName );

        return f;
    }


    @Override public String 
    getDescription()
    {
        return description;
    }
}
