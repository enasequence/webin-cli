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
package uk.ac.ebi.ena.webin.cli.context.reads;

import java.io.File;
import java.util.stream.Stream;

import org.apache.commons.lang.StringUtils;

import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.manifest.*;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.QualityScore;

public class
ReadsManifestReader extends ManifestReader<ReadsManifest> {

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
    public final static String QUALITY_SCORE_PHRED_33 = "PHRED_33";
    public final static String QUALITY_SCORE_PHRED_64 = "PHRED_64";
    public final static String QUALITY_SCORE_LOGODDS = "LOGODDS";

    public final static ManifestCVList CV_INSTRUMENT = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/instrument.properties"));
    public final static ManifestCVList CV_PLATFORM = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/platform.properties"));
    public final static ManifestCVList CV_SELECTION = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/selection.properties"));
    public final static ManifestCVList CV_SOURCE = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/source.properties"));
    public final static ManifestCVList CV_STRATEGY = new ManifestCVList(new File("uk/ac/ebi/ena/webin/cli/reads/strategy.properties"));
    public final static ManifestCVList CV_QUALITY_SCORE = new ManifestCVList(
            QUALITY_SCORE_PHRED_33,
            QUALITY_SCORE_PHRED_64,
            QUALITY_SCORE_LOGODDS
    );

    private final ReadsManifest manifest = new ReadsManifest();


    public ReadsManifestReader(
            ManifestReaderParameters parameters,
            MetadataProcessorFactory factory)
    {
        super(parameters,
                // Fields.
                new ManifestFieldDefinition.Builder()
                       .meta().required().name(Field.NAME).desc(Description.NAME).and()
                       .meta().required().name(Field.STUDY).desc(Description.STUDY).processor(factory.getStudyProcessor()).and()
                       .meta().required().name(Field.SAMPLE).desc(Description.SAMPLE).processor(factory.getSampleProcessor()).and()
                       .meta().optional().name(Field.DESCRIPTION).desc(Description.DESCRIPTION).and()
                       .meta().optional().recommended().name(Field.INSTRUMENT).desc(Description.INSTRUMENT).processor(new CVFieldProcessor(CV_INSTRUMENT)).and()
                       .meta().optional().name(Field.PLATFORM).desc(Description.PLATFORM).processor(new CVFieldProcessor(CV_PLATFORM)).and()
                       .meta().required().name(Field.LIBRARY_SOURCE).desc(Description.LIBRARY_SOURCE).processor(new CVFieldProcessor(CV_SOURCE)).and()
                       .meta().required().name(Field.LIBRARY_SELECTION).desc(Description.LIBRARY_SELECTION).processor(new CVFieldProcessor(CV_SELECTION)).and()
                       .meta().required().name(Field.LIBRARY_STRATEGY).desc(Description.LIBRARY_STRATEGY).processor(new CVFieldProcessor(CV_STRATEGY)).and()
                       .meta().optional().name(Field.LIBRARY_CONSTRUCTION_PROTOCOL).desc(Description.LIBRARY_CONSTRUCTION_PROTOCOL).and()
                       .meta().optional().name(Field.LIBRARY_NAME).desc(Description.LIBRARY_NAME).and()
                       .meta().optional().name(Field.INSERT_SIZE).desc(Description.INSERT_SIZE).and()
                       .file().optional(2).name(Field.FASTQ).desc(Description.FASTQ).processor(getFastqProcessors()).and()
                       .file().optional().name(Field.BAM).desc(Description.BAM).processor(getBamProcessors()).and()
                       .file().optional().name(Field.CRAM).desc(Description.CRAM).processor(getCramProcessors()).and()
                       .meta().optional().hidden().name(Field.QUALITY_SCORE).desc(Description.QUALITY_SCORE).processor(new CVFieldProcessor(CV_QUALITY_SCORE)).and()
                       .meta().optional().hidden().name(Field.__HORIZON).desc(Description.__HORIZON).and()
                       .meta().optional().name(Fields.SUBMISSION_TOOL).desc(Descriptions.SUBMISSION_TOOL).and()
                       .meta().optional().name(Fields.SUBMISSION_TOOL_VERSION).desc(Descriptions.SUBMISSION_TOOL_VERSION)
                       .build()
                ,
                // File groups.
                new ManifestFileCount.Builder()
                        .group("Single or paired sequence reads in one or two fastq files.")
                        .required(Field.FASTQ, 2)
                        .and()
                        .group("Sequence reads in a CRAM file.")
                        .required(Field.CRAM)
                        .and()
                        .group("Sequence reads in a BAM file.")
                        .required(Field.BAM)
                        .build()
        );

        if ( factory.getStudyProcessor() != null ) {
            factory.getStudyProcessor().setCallback(study -> manifest.setStudy(study));
        }
        if ( factory.getSampleProcessor() != null ) {
            factory.getSampleProcessor().setCallback(sample -> manifest.setSample(sample));
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

    @Override public void
    processManifest()
    {
        manifest.setName(getManifestReaderResult().getValue( Field.NAME ));
        manifest.setDescription(getManifestReaderResult().getValue( Field.DESCRIPTION ));

        if (getManifestReaderResult().getCount(Field.INSTRUMENT) > 0 &&
                getManifestReaderResult().getField(Field.INSTRUMENT).isValidFieldValueOrFileSuffix())
            manifest.setInstrument(getManifestReaderResult().getValue(Field.INSTRUMENT));

        if (getManifestReaderResult().getCount(Field.PLATFORM) > 0 &&
                getManifestReaderResult().getField(Field.PLATFORM).isValidFieldValueOrFileSuffix())
            manifest.setPlatform(getManifestReaderResult().getValue(Field.PLATFORM));

        manifest.setInsertSize(getAndValidatePositiveInteger(getManifestReaderResult().getField(Field.INSERT_SIZE)));

        if (getManifestReaderResult().getCount(Field.LIBRARY_SOURCE) > 0 &&
                getManifestReaderResult().getField(Field.LIBRARY_SOURCE).isValidFieldValueOrFileSuffix())
            manifest.setLibrarySource(getManifestReaderResult().getValue(Field.LIBRARY_SOURCE));

        if (getManifestReaderResult().getCount(Field.LIBRARY_SELECTION) > 0 &&
                getManifestReaderResult().getField(Field.LIBRARY_SELECTION).isValidFieldValueOrFileSuffix())
            manifest.setLibrarySelection(getManifestReaderResult().getValue(Field.LIBRARY_SELECTION));

        if (getManifestReaderResult().getCount(Field.LIBRARY_STRATEGY) > 0 &&
                getManifestReaderResult().getField(Field.LIBRARY_STRATEGY).isValidFieldValueOrFileSuffix())
            manifest.setLibraryStrategy(getManifestReaderResult().getValue(Field.LIBRARY_STRATEGY));

        manifest.setLibraryConstructionProtocol(getManifestReaderResult().getValue(Field.LIBRARY_CONSTRUCTION_PROTOCOL));
        manifest.setLibraryName(getManifestReaderResult().getValue(Field.LIBRARY_NAME));

        if (getManifestReaderResult().getCount(Field.QUALITY_SCORE) > 0) {
            String qsStr = getManifestReaderResult().getValue(Field.QUALITY_SCORE);
            try {
                QualityScore qs = QualityScore.valueOf(qsStr);
                manifest.setQualityScore(qs);
            } catch (Exception ex) {
                error(WebinCliMessage.READS_MANIFEST_READER_INVALID_QUALITY_SCORE_ERROR, getManifestReaderResult().getValue(Field.QUALITY_SCORE));
            }
        }

        if (getManifestReaderResult().getCount(Field.__HORIZON) > 0)
            manifest.setPairingHorizon(getAndValidatePositiveInteger(getManifestReaderResult().getField(Field.__HORIZON)));

        manifest.setSubmissionTool(getManifestReaderResult().getValue(Fields.SUBMISSION_TOOL));
        manifest.setSubmissionToolVersion(getManifestReaderResult().getValue(Fields.SUBMISSION_TOOL_VERSION));

        processInstrumentAndPlatform();

        SubmissionFiles<ReadsManifest.FileType> submissionFiles = manifest.files();

        getFiles( getInputDir(), getManifestReaderResult(), ReadsManifestReader.Field.BAM ).forEach(file -> submissionFiles.add( new SubmissionFile( ReadsManifest.FileType.BAM, file ) ) );
        getFiles( getInputDir(), getManifestReaderResult(), ReadsManifestReader.Field.CRAM ).forEach(file -> submissionFiles.add( new SubmissionFile( ReadsManifest.FileType.CRAM, file ) ) );
        getFiles( getInputDir(), getManifestReaderResult(), ReadsManifestReader.Field.FASTQ ).forEach(file -> submissionFiles.add( new SubmissionFile( ReadsManifest.FileType.FASTQ, file ) ) );
    }

    private void
    processInstrumentAndPlatform()
    {
        if( null == manifest.getPlatform()&& ( null == manifest.getInstrument() || manifest.getInstrument().equals(INSTRUMENT_UNSPECIFIED) ) )
        {
            error(WebinCliMessage.READS_MANIFEST_READER_MISSING_PLATFORM_AND_INSTRUMENT_ERROR,
                    String.join(", ", CV_PLATFORM.keyList()),
                    String.join(", ", CV_INSTRUMENT.keyList()));
        }

        if( manifest.getInstrument() != null )
        {
            // Set platform.

            String platforms = CV_INSTRUMENT.getValue( manifest.getInstrument() );
            if( StringUtils.isBlank( platforms ) )
            {
                error(WebinCliMessage.READS_MANIFEST_READER_MISSING_PLATFORM_FOR_INSTRUMENT_ERROR, manifest.getInstrument());
            }

            String[] platformList = platforms.split( "[;,]" );

            if( 1 == platformList.length )
            {
                manifest.setPlatform(CV_PLATFORM.getKey( platformList[ 0 ] ));
            } else if(Stream.of( platformList ).noneMatch(e -> e.equals( manifest.getPlatform() ) ))
            {
                error( WebinCliMessage.READS_MANIFEST_READER_INVALID_PLATFORM_FOR_INSTRUMENT_ERROR,
                        StringUtils.isBlank( manifest.getPlatform() ) ? "is not defined" : manifest.getPlatform() + " is not supported",
                        manifest.getInstrument(),
                        CV_INSTRUMENT.getValue( manifest.getInstrument() ) );
            }
        } else
        {
            manifest.setInstrument(INSTRUMENT_UNSPECIFIED);
        }
    }

    @Override
    public ReadsManifest getManifest() {
        return manifest;
    }
}
