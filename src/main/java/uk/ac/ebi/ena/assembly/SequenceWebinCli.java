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

package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.embl.agp.reader.AGPFileReader;
import uk.ac.ebi.embl.agp.reader.AGPLineReader;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.EntryReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.ena.manifest.ManifestReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public abstract class 
SequenceWebinCli<T extends ManifestReader> extends AbstractWebinCli<T>
{
    private static final String DIGEST_NAME = "MD5";
    protected final static String ANALYSIS_XML = "analysis.xml";


    private Study  study;
    private Sample sample;

    protected File chromosomeListFile;
    protected File unlocalisedListFile;
    protected List<File> fastaFiles = new ArrayList<>();
    protected List<File> flatFiles = new ArrayList<>();
    protected List<File> agpFiles = new ArrayList<>();
    protected List<File> tsvFiles = new ArrayList<>();

    protected AssemblyInfoEntry assembly_info;

    protected abstract boolean validateInternal() throws ValidationEngineException;
    
    protected File
    getReportFile( FileFormat filetype, String filename )
    {
        return super.getReportFile( String.valueOf( filetype ), filename );
    }

    
    public void
    setStudy( Study study )
    { 
        this.study = study;
    }

    public void
    setSample( Sample sample )
    {
        this.sample = sample;
    }


    public Study
    getStudy()
    { 
        return this.study;
    }


    public Sample
    getSample()
    {
        return this.sample;
    }

    @SuppressWarnings( "unchecked" ) protected <T> T
    getFileReader( FileFormat format, File file ) throws IOException
    {
         if( !EntryReader.getBlockCounter().isEmpty() )
             EntryReader.getBlockCounter().clear();
         
          if( !EntryReader.getSkipTagCounter().isEmpty() )
              EntryReader.getSkipTagCounter().clear();
          
        switch( format )
        {
        default:
            return null;
        
        case FASTA:
            return (T) new FastaFileReader( new FastaLineReader( FileUtils.getBufferedReader( file ) ) );

        case AGP:
            return (T) new AGPFileReader( new AGPLineReader( FileUtils.getBufferedReader( file ) ) );
        
        case FLATFILE:
            EmblEntryReader emblReader = new EmblEntryReader( FileUtils.getBufferedReader( file ), EmblEntryReader.Format.EMBL_FORMAT, file.getName() );
            emblReader.setCheckBlockCounts( true );
            return (T) emblReader;

        case INFO:
            return (T) new AssemblyInfoReader( file );

        case CHROMOSOME_LIST:
            return (T) new ChromosomeListFileReader( file );
            
        case UNLOCALISED_LIST:
            return (T) new UnlocalisedListFileReader( file );
        }
    }


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
    
    
    public AssemblyInfoEntry
    getAssemblyInfo()
    {
        return assembly_info;
    }


    public void
    setAssemblyInfo( AssemblyInfoEntry assembly_info )
    {
        this.assembly_info = assembly_info;
    }

    
    protected Element
    createTextElement( String name, String text )
    {
        Element e = new Element( name );
        e.setText( text );
        return e;
    }
   
    
   abstract Element makeAnalysisType( AssemblyInfoEntry entry );

   
    String
    createAnalysisXml( List<Element> fileList, AssemblyInfoEntry entry, String centerName  ) 
    {
        try 
        {
            String full_name = getContext().getTitle( getName() );
            //String type      = getContext().getType(); 
            
            Element analysisSetE = new Element( "ANALYSIS_SET" );
            Element analysisE = new Element( "ANALYSIS" );
            analysisSetE.addContent( analysisE );
            
            Document doc = new Document( analysisSetE );
            analysisE.setAttribute( "alias", "ena-ANALYSIS-" + System.currentTimeMillis() );
            
            if( null != centerName && !centerName.isEmpty() )
                analysisE.setAttribute( "center_name", centerName );
            
            analysisE.addContent( new Element( "TITLE" ).setText( full_name ) );
            Element studyRefE = new Element( "STUDY_REF" );
            analysisE.addContent( studyRefE );
            studyRefE.setAttribute( "accession", entry.getStudyId() );
            if( entry.getSampleId() != null && !entry.getSampleId().isEmpty() )
            {
                Element sampleRefE = new Element( "SAMPLE_REF" );
                analysisE.addContent( sampleRefE );
                sampleRefE.setAttribute( "accession", entry.getSampleId() );
            }
            Element analysisTypeE = new Element( "ANALYSIS_TYPE" );
            analysisE.addContent(analysisTypeE);
            Element typeE = makeAnalysisType( entry );
            analysisTypeE.addContent( typeE );

            Element filesE = new Element( "FILES" );
            analysisE.addContent( filesE );
            
            for( Element e: fileList )
                filesE.addContent( e );
            
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat( Format.getPrettyFormat() );
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output( doc, stringWriter );
            return stringWriter.toString();
            
        } catch( IOException e ) 
        {
            throw WebinCliException.createSystemError( e.getMessage() );
        }
    }


    protected Element
    createfileElement( Path uploadDir, File file, String file_type )
    {
        try
        {
            return createfileElement( String.valueOf( uploadDir.resolve( extractSubpath( getParameters().getInputDir(), file ) ) ).replaceAll( "\\\\+", "/" ), 
                                      String.valueOf( file_type ), 
                                      DIGEST_NAME, 
                                      FileUtils.calculateDigest( DIGEST_NAME, file ) );
        }catch( IOException | NoSuchAlgorithmException e )
        {
            throw new RuntimeException( e );
        }
    }

    
    private Element
    createfileElement( String file_path, String file_type, String digest, String checksum )
    {
        Element fileE = new Element( "FILE" );
        fileE.setAttribute( "filename", file_path );
        fileE.setAttribute( "filetype", String.valueOf( file_type ) );
        fileE.setAttribute( "checksum_method", digest );
        fileE.setAttribute( "checksum", checksum );
        return fileE;
    }
    

    protected String
    extractSubpath( File inputDir, File file ) throws IOException
    {
        return file.toPath().startsWith( inputDir.toPath() ) ? inputDir.toPath().relativize( file.toPath() ).toString() : file.getName();
    }

    @Override public boolean
    validate() throws ValidationEngineException
    {
        if( !FileUtils.emptyDirectory( getValidationDir() ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + getValidationDir() );
        
        if( !FileUtils.emptyDirectory( getSubmitDir() ) )
            throw WebinCliException.createSystemError( "Unable to empty directory " + getSubmitDir() );
        
        return validateInternal();
    }
    
    
    protected List<File>
    getUploadFiles() throws IOException
    {
        List<File> uploadFileList = new ArrayList<>();
        if( null != chromosomeListFile )
            uploadFileList.add( chromosomeListFile );           
        
        if( null != unlocalisedListFile )
            uploadFileList.add( unlocalisedListFile );
        
        uploadFileList.addAll( fastaFiles );
        uploadFileList.addAll( flatFiles );
        uploadFileList.addAll( agpFiles );
        uploadFileList.addAll( tsvFiles );
       
        return uploadFileList;
    }
    
    
    protected List<Element>
    getXMLFiles( Path uploadDir ) throws IOException
    {
        List<Element> eList = new ArrayList<>();

        if( null != chromosomeListFile )
            eList.add( createfileElement( uploadDir, chromosomeListFile, "chromosome_list" ) );
        
        if( null != unlocalisedListFile )
            eList.add( createfileElement( uploadDir, unlocalisedListFile, "unlocalised_list" ) );
        
        fastaFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "fasta" ) ) );
        flatFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "flatfile" ) ) );
        agpFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "agp" ) ) );
        tsvFiles.forEach( file -> eList.add( createfileElement( uploadDir, file, "tab" ) ) );

        return eList;
    }
    
    
    @Override public void
    prepareSubmissionBundle() throws WebinCliException
    {
        try
        {
            Path uploadDir = Paths.get( String.valueOf( getContext() ), getSafeOutputSubdir(getName()) );

            List<File> uploadFileList = getUploadFiles();
            List<Element> eList = getXMLFiles( uploadDir );

            String xml = createAnalysisXml( eList, getAssemblyInfo(), getParameters().getCenterName() );
            
            Path analysisFile = getSubmitDir().toPath().resolve( ANALYSIS_XML );
    
            Files.write( analysisFile, xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );
    
            setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
                                                       uploadDir.toString(), 
                                                       uploadFileList, 
                                                       Arrays.asList( new SubmissionXMLFile( SubmissionXMLFileType.ANALYSIS, analysisFile.toFile(), FileUtils.calculateDigest( "MD5", analysisFile.toFile() ) ) ), 
                                                       getParameters().getCenterName() ) );   
        } catch( IOException | NoSuchAlgorithmException e )
        {
            throw WebinCliException.createSystemError( e.getMessage() );
        }        
    }
}
