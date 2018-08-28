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
import java.io.OutputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.jdom2.Element;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.rawreads.RawReadsManifest;
import uk.ac.ebi.ena.rawreads.RawReadsManifest.Fields;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class
SequenceWebinCliTest
{
    @Before public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }
    
    
    private File
    createOutputFolder() throws IOException
    {
        File output = File.createTempFile( "test", "test" );
        Assert.assertTrue( output.delete() );
        Assert.assertTrue( output.mkdirs() );
        return output;
    }
    
    
    
    @Test public void 
    analysisXML() throws IOException, ValidationEngineException
    {
        SequenceWebinCli s = new SequenceWebinCli() 
        {
            @Override public boolean validateInternal() throws ValidationEngineException { return true; }
            @Override public boolean getTestMode() { return true; }
            @Override ContextE getContext() { return ContextE.genome; }
			@Override public File getSubmissionBundleFileName() { return null; }
            @Override Element makeAnalysisType( AssemblyInfoEntry entry )
            {
                return new GenomeAssemblyWebinCli().makeAnalysisType( entry );
            }
        };

        s.setName( "123" );
        AssemblyInfoEntry aie = new AssemblyInfoEntry();
        aie.setSampleId( "sample_id" );
        aie.setStudyId( "study_id" );
        
        String xml = s.createAnalysisXml( Collections.emptyList(), aie, null ); 
        Assert.assertTrue( xml.contains( "SEQUENCE_ASSEMBLY" ) );
        Assert.assertTrue( xml.contains( "sample_id" ) );
        Assert.assertTrue( xml.contains( "study_id" ) );
        Assert.assertTrue( xml.contains( "Genome assembly: 123" ) );
        Assert.assertTrue( !xml.contains( "FILE " ) );
    }
    
    
    @Test public void 
    testAssemblywithAGP() throws Exception 
    {
        SequenceWebinCli s = new GenomeAssemblyWebinCli( true );

        Path fasta_file = Files.write( File.createTempFile( "FASTA", "FASTA" ).toPath(), ">123\nACGT".getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.TRUNCATE_EXISTING );
        s.getParameters().setInputDir( fasta_file.getParent().toFile() );
        
        s.setName( "123" );
        AssemblyInfoEntry aie = new AssemblyInfoEntry();
        aie.setSampleId( "sample_id" );
        aie.setStudyId( "study_id" );
        File submit_dir = createOutputFolder();
        s.setSubmitDir( submit_dir );
        s.setAssemblyInfo( aie );
        s.defineFileTypes( Files.write( File.createTempFile( "FILE", "FILE" ).toPath(), 
                                        ( "INFO 123.gz\nFASTA " + fasta_file.toString() ).getBytes( StandardCharsets.UTF_8 ), 
                                        StandardOpenOption.TRUNCATE_EXISTING ).toFile() );
        
        s.prepareSubmissionBundle();
        
        SubmissionBundle sb = s.getSubmissionBundle();
        Assert.assertTrue( Files.isSameFile( sb.getSubmitDirectory().toPath(), submit_dir.toPath() ) );
        Assert.assertTrue( sb.getXMLFileList().get( 0 ).getFile().exists() );
        
        String xmlfile = new String( Files.readAllBytes( sb.getXMLFileList().get( 0 ).getFile().toPath() ), StandardCharsets.UTF_8 );
        Assert.assertTrue( xmlfile.contains( fasta_file.getFileName() + "\"" ) );
        Assert.assertTrue( xmlfile.contains( "6f82bc96add84ece757afad265d7e341" ) );
        Assert.assertTrue( xmlfile.contains( "FASTA" ) );
        Assert.assertTrue( xmlfile.contains( "MD5" ) );
        Assert.assertTrue( !xmlfile.contains( "123.gz" ) );
        Assert.assertTrue( !xmlfile.contains( "INFO" ) );
        
        //System.out.println( xmlfile );
    }
    
    
    public Path
    copyRandomized( String resource_name, Path folder, boolean compress, String...suffix ) throws IOException
    {
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( resource_name );
        File file = new File( URLDecoder.decode( url.getFile(), "UTF-8" ) );
        Path path = Files.createTempFile( folder, "COPY", file.getName() + ( suffix.length > 0 ? Stream.of( suffix ).collect( Collectors.joining( "" ) ) : "" ) );
        OutputStream os;
        Files.copy( file.toPath(), ( os = compress ? new GZIPOutputStream( Files.newOutputStream( path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC ) ) 
                                            : Files.newOutputStream( path, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC ) ) );
        os.flush();
        os.close();
        Assert.assertTrue( Files.exists( path ) );
        Assert.assertTrue( Files.isRegularFile( path ) );
        return path;
    }
    
    
    @Test( expected = WebinCliException.class ) public void
    manifestEmbeddedInfo() throws ValidationEngineException, IOException
    {
        SequenceWebinCli s = new SequenceWebinCli() {
            @Override protected boolean validateInternal() throws ValidationEngineException { return false; }
            @Override Element makeAnalysisType(AssemblyInfoEntry entry) { return null; }
            @Override ContextE getContext() { return null; }
            @Override public void init( WebinCliParameters parameters ) { setParameters( parameters ); setValidationDir( parameters.getOutputDir() ); } 
            
        };
        
        Path input_dir = createOutputFolder().toPath();
        Path fastafile = copyRandomized( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );
        
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                ( Fields.STUDY             + " SRP123456789\n"
                + Fields.SAMPLE            + " ERS198522\n"
                + Fields.PLATFORM          + " ILLUMINA\n"
                + Fields.INSTRUMENT        + " unspecifieD\n"
                + Fields.INSERT_SIZE       + " -1\n"
                + Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                + Fields.LIBRARY_SOURCE    + " OTHER\n"
                + Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + Fields.NAME              + " SOME-FANCY-NAME\n "
                + "FASTA " + input_dir.relativize( fastafile ).toString() ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        WebinCliParameters parameters = new WebinCliParameters();
                
        parameters.setManifestFile( man.toFile() );
        parameters.setInputDir( input_dir.toFile() );
        parameters.setOutputDir( createOutputFolder() );

        s.init( parameters );
        s.getParameters().setManifestFile( man.toFile() );
        s.getParameters().setInputDir( input_dir.toFile() );
        s.getParameters().setOutputDir( createOutputFolder() );
        s.defineFileTypes( man.toFile() );
        
        Assert.assertTrue( s.infoFile.getName().contains( man.getFileName().toString() ) );
        Assert.assertEquals( 1, s.fastaFiles.size() );
        
        s.defineInfo( s.infoFile );
    }


    @Test public void
    manifestSeparatedInfo() throws ValidationEngineException, IOException
    {
        SequenceWebinCli s = new SequenceWebinCli() {
            @Override protected boolean validateInternal() throws ValidationEngineException { return false; }
            @Override Element makeAnalysisType(AssemblyInfoEntry entry) { return null; }
            @Override ContextE getContext() { return ContextE.genome; }
            @Override public void init( WebinCliParameters parameters ) { 
                setParameters( parameters ); 
                setValidationDir( parameters.getOutputDir() ); 
            } 
            
        };
        
        Path input_dir = createOutputFolder().toPath();
        Path fastafile = copyRandomized( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );

        Path info = Files.write( Files.createTempFile( input_dir, "TEMP", ".info" ), 
                ( Fields.STUDY             + " SRP123456789\n"
                + Fields.SAMPLE            + " ERS198522\n"
                + Fields.PLATFORM          + " ILLUMINA\n"
                + Fields.INSTRUMENT        + " unspecifieD\n"
                + Fields.INSERT_SIZE       + " -1\n"
                + Fields.LIBRARY_STRATEGY  + " CLONEEND\n"
                + Fields.LIBRARY_SOURCE    + " OTHER\n"
                + Fields.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + Fields.NAME              + " SOME-FANCY-NAME\n " ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );
        
        Path man = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ), 
                ( "INFO "  + input_dir.relativize( info ) + "\n"
                + "FASTA " + input_dir.relativize( fastafile ).toString() ).getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        WebinCliParameters parameters = new WebinCliParameters();
                
        parameters.setManifestFile( man.toFile() );
        parameters.setInputDir( input_dir.toFile() );
        parameters.setOutputDir( createOutputFolder() );

        s.init( parameters );
        s.defineFileTypes( man.toFile() );
        
        Assert.assertTrue( Files.isSameFile( info, s.infoFile.toPath() ) );
        Assert.assertEquals( 1, s.fastaFiles.size() );
        
        try
        {
            s.defineInfo( s.infoFile );
            Assert.assertTrue( false );
            
        } catch( WebinCliException e )
        {
            List<Path> files = Files.find( s.getParameters().getOutputDir().toPath(), 
                                           1, 
                                           ( path, attr ) -> { return String.valueOf( path.getFileName() ).contains( info.getFileName().toString() ); } )
                                    .collect( Collectors.toList() );
            Assert.assertEquals( 1, files.size() );
        }
    }

}
