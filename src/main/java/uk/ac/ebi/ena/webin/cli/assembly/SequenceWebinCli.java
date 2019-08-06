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
package uk.ac.ebi.ena.webin.cli.assembly;

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

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.model.manifest.Manifest;
import uk.ac.ebi.ena.model.reference.Analysis;
import uk.ac.ebi.ena.model.reference.Run;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.service.IgnoreErrorsService;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

public abstract class 
SequenceWebinCli<R extends SequenceManifestReader, M extends Manifest> extends AbstractWebinCli<R>
{
    private static final String DIGEST_NAME = "MD5";
    protected final static String ANALYSIS_XML = "analysis.xml";

    private static final Logger log = LoggerFactory.getLogger(SequenceWebinCli.class);

    public void 
    setInputDir( File inputDir )
    {
        getParameters().setInputDir( inputDir );
    }

    public File
    getInputDir()
    {
        return getParameters().getInputDir();
    }

    @Override
    public R getManifestReader() {
        return super.getManifestReader();
    }

    private M getManifest() {
        SequenceManifestReader<M> manifestReader = getManifestReader();
        if (manifestReader != null) {
            return manifestReader.getManifest();
        }
        return null;
    }

    // TODO: remove function
    @Override protected void
    readManifest( Path inputDir, File manifestFile )
    {
        getManifestReader().readManifest( inputDir, manifestFile );
        setDescription( getManifestReader().getManifest().getDescription() );
    }

    abstract Element createXmlAnalysisTypeElement();

    protected Element
    createXmlTextElement(String name, String text )
    {
        Element e = new Element( name );
        e.setText( text );
        return e;
    }

    private String
    createAnalysisXml( List<Element> fileElements, String centerName )
    {
        M manifest = getManifest();

        try
        {
            String full_name = getContext().getXmlTitle( getName() );

            Element analysisSetE = new Element( "ANALYSIS_SET" );
            Element analysisE = new Element( "ANALYSIS" );
            analysisSetE.addContent( analysisE );
            
            Document doc = new Document( analysisSetE );
            analysisE.setAttribute( "alias", getAlias() );
            
            if( null != centerName && !centerName.isEmpty() )
                analysisE.setAttribute( "center_name", centerName );
            
            analysisE.addContent( new Element( "TITLE" ).setText( full_name ) );
            
            if( null != manifest.getDescription() && !manifest.getDescription().isEmpty() )
                analysisE.addContent( new Element( "DESCRIPTION" ).setText( manifest.getDescription() ) );
                
            Element studyRefE = new Element( "STUDY_REF" );
            analysisE.addContent( studyRefE );
            studyRefE.setAttribute( "accession", manifest.getStudy().getBioProjectId() );
			if( manifest.getSample() != null && manifest.getSample().getBioSampleId() != null && !manifest.getSample().getBioSampleId().isEmpty() )
            {
                Element sampleRefE = new Element( "SAMPLE_REF" );
                analysisE.addContent( sampleRefE );
				sampleRefE.setAttribute( "accession", manifest.getSample().getBioSampleId() );
            }
			
			
            if( null != manifest.getRun() )
            {
                List<Run> run = manifest.getRun();
                for( Run r : run )
                {
                    Element runRefE = new Element( "RUN_REF" );
                    analysisE.addContent( runRefE );
                    runRefE.setAttribute( "accession", r.getRunId() );
                }
            }

            
            if( null != manifest.getAnalysis() )
            {
                List<Analysis> analysis = manifest.getAnalysis();
                for( Analysis a : analysis )
                {
                    Element analysisRefE = new Element( "ANALYSIS_REF" );
                    analysisE.addContent( analysisRefE );
                    analysisRefE.setAttribute( "accession", a.getAnalysisId() );
                }
            }
			
            Element analysisTypeE = new Element( "ANALYSIS_TYPE" );
            analysisE.addContent( analysisTypeE );
            Element typeE = createXmlAnalysisTypeElement();
            analysisTypeE.addContent( typeE );

            Element filesE = new Element( "FILES" );
            analysisE.addContent( filesE );
            
            for( Element e: fileElements )
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


    protected Element
    createXmlFileElement(Path uploadDir, File file, String fileType)
    {
        try
        {
            return createXmlFileElement( String.valueOf( uploadDir.resolve( extractSubpath( getParameters().getInputDir(), file ) ) ).replaceAll( "\\\\+", "/" ),
                                      String.valueOf( fileType ),
                                      DIGEST_NAME, 
                                      FileUtils.calculateDigest( DIGEST_NAME, file ) );
        }catch( IOException | NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }
    }

    
    private Element
    createXmlFileElement(String fileName, String fileType, String digest, String checksum )
    {
        Element fileE = new Element( "FILE" );
        fileE.setAttribute( "filename", fileName );
        fileE.setAttribute( "filetype", String.valueOf( fileType ) );
        fileE.setAttribute( "checksum_method", digest );
        fileE.setAttribute( "checksum", checksum );
        return fileE;
    }

    private String
    extractSubpath(File inputDir, File file)
    {
        return file.toPath().startsWith( inputDir.toPath() ) ? inputDir.toPath().relativize( file.toPath() ).toString() : file.getName();
    }

    protected abstract void validate(File reportDir, File processDir) throws WebinCliException, ValidationEngineException;
   
    @Override public void
    validate() throws WebinCliException
    {
        if( !FileUtils.emptyDirectory( getValidationDir() ) )
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(getValidationDir()));

        if( !FileUtils.emptyDirectory( getProcessDir() ) )
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(getProcessDir()));

        if( !FileUtils.emptyDirectory( getSubmitDir() ) )
            throw WebinCliException.systemError(WebinCliMessage.Cli.EMPTY_DIRECTORY_ERROR.format(getSubmitDir()));

        M manifest = getManifest();

        manifest.setIgnoreErrors(false);
        try {
            IgnoreErrorsService ignoreErrorsService = new IgnoreErrorsService.Builder()
                    .setCredentials(getParameters().getUsername(),
                            getParameters().getPassword())
                    .setTest(getTestMode())
                    .build();

            manifest.setIgnoreErrors(ignoreErrorsService.getIgnoreErrors(getContext().name(), getName()));
        }
        catch (RuntimeException ex) {
            log.warn(WebinCliMessage.Service.IGNORE_ERRORS_SERVICE_SYSTEM_ERROR.format());
        }

        try
        {
            validate(getValidationDir(), getProcessDir());

        } catch( ValidationEngineException ex )
        {
            switch( ex.getErrorType() )
            {
                case VALIDATION_ERROR:
                    throw WebinCliException.validationError( ex );

                default:
                    throw WebinCliException.systemError( ex );
            }
        }
    }

    protected abstract List<Element> createXmlFileElements(Path uploadDir);
    
    @Override public void
    prepareSubmissionBundle() throws WebinCliException
    {
        try
        {
            Path uploadDir = getUploadRoot().resolve( Paths.get( String.valueOf( getContext() ), WebinCli.getSafeOutputDir( getName() ) ) );
            
            List<File> uploadFileList = getManifest().files().files();
            List<Element> fileElements = createXmlFileElements( uploadDir );

            String xml = createAnalysisXml( fileElements, getParameters().getCenterName() );
            
            Path analysisFile = getSubmitDir().toPath().resolve( ANALYSIS_XML );
    
            Files.write( analysisFile, xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
    
            setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
                                                       uploadDir.toString(), 
                                                       uploadFileList, 
                                                       Arrays.asList( new SubmissionXMLFile( SubmissionXMLFileType.ANALYSIS, analysisFile.toFile(), FileUtils.calculateDigest( "MD5", analysisFile.toFile() ) ) ), 
                                                       getParameters().getCenterName(),
                                                       FileUtils.calculateDigest( "MD5", getParameters().getManifestFile() ) ) );   
        } catch( IOException | NoSuchAlgorithmException ex )
        {
            throw WebinCliException.systemError( ex );
        }        
    }
}
