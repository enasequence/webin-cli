package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collections;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.webin.cli.SubmissionBundle;
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
            @Override public boolean validate() throws ValidationEngineException { return true; }
            @Override boolean getTestMode() { return true; }
            @Override ContextE getContext() { return ContextE.genome; }
			@Override
			public void prepareSubmissionBundle() throws IOException {
				// TODO Auto-generated method stub
				
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
        SequenceWebinCli s = new GenomeAssemblyWebinCli();

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
        Assert.assertTrue( sb.getXMLFile().exists() );
        
        String xmlfile = new String( Files.readAllBytes( sb.getXMLFile().toPath() ), StandardCharsets.UTF_8 );
        Assert.assertTrue( xmlfile.contains( fasta_file.getFileName() + "\"" ) );
        Assert.assertTrue( xmlfile.contains( "6f82bc96add84ece757afad265d7e341" ) );
        Assert.assertTrue( xmlfile.contains( "FASTA" ) );
        Assert.assertTrue( xmlfile.contains( "MD5" ) );
        Assert.assertTrue( !xmlfile.contains( "123.gz" ) );
        Assert.assertTrue( !xmlfile.contains( "INFO" ) );
        
        //System.out.println( xmlfile );
    }
    
}
