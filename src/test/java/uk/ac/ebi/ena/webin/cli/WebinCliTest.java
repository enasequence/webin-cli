package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCliTest;
import uk.ac.ebi.ena.rawreads.RawReadsManifest.RawReadsManifestTags;
import uk.ac.ebi.ena.submit.ContextE;

public class 
WebinCliTest
{
    String passwd;
    String usernm;
    
    @Before
    public void
    before() throws UnsupportedEncodingException
    {
        usernm = System.getenv( "webin-cli-username" );
        passwd = System.getenv( "webin-cli-password" );
    }
    
    
    private String
    getRawReadsInfoPart( String name )
    {
        return    RawReadsManifestTags.STUDY             + " SRP052303\n"
                + RawReadsManifestTags.SAMPLE            + " ERS2554688\n"
                + RawReadsManifestTags.PLATFORM          + " ILLUMINA\n"
                + RawReadsManifestTags.INSTRUMENT        + " unspecifieD\n"
                + RawReadsManifestTags.INSERT_SIZE       + " 1\n"
                + RawReadsManifestTags.LIBRARY_NAME      + " YOBA LIB\n"
                + RawReadsManifestTags.LIBRARY_STRATEGY  + " CLONEEND\n"
                + RawReadsManifestTags.LIBRARY_SOURCE    + " OTHER\n"
                + RawReadsManifestTags.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + RawReadsManifestTags.NAME              + " " + name + "\n ";
    }
    
    
    @Test public void
    testRawReadsSubmission() throws Exception
    {
        Path input_dir = createOutputFolder().toPath();
        Path cram_file = copyRandomized( "uk/ac/ebi/ena/rawreads/18045_1#93.cram", input_dir, false );
        
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.userName = usernm;
        parameters.password = passwd;
        parameters.context  = ContextE.reads.toString();
        parameters.centerName = "C E N T E R N A M E";
        parameters.inputDir   = input_dir.toString();
        parameters.outputDir  = createOutputFolder().getPath();
        parameters.manifest   = Files.write( File.createTempFile( "MANIFEST", "FILE" ).toPath(), 
                                             ( getRawReadsInfoPart( String.format( "SOME-FANCY-NAME %X", System.currentTimeMillis() ) ) + "CRAM " + input_dir.relativize( cram_file ) ).getBytes( StandardCharsets.UTF_8 ), 
                                             StandardOpenOption.TRUNCATE_EXISTING ).toString();
        parameters.test = true;
        parameters.validate = true;
        parameters.submit   = true;
        
        WebinCli webinCli = new WebinCli();
        webinCli.init( parameters );
        webinCli.setTestMode( true );
        webinCli.execute();

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
    
    
    public String 
    getAssemblyInfo( String name )
    {
        return   "ASSEMBLYNAME " + name + "\n"
               + "COVERAGE 45\n"
               + "PROGRAM assembly\n"
               + "PLATFORM fghgf\n"
               + "MINGAPLENGTH 34\n"
               + "MOLECULETYPE genomic DNA\n"
               + "SAMPLE SAMN04526268\n"
               + "STUDY PRJEB20083\n";
    }
    
    
    @Test public void
    testGenomeSubmission() throws Exception
    {
        Path input_dir = createOutputFolder().toPath();
        
        Path flatfile = copyRandomized( "uk/ac/ebi/ena/assembly/valid_flatfileforAgp.txt", input_dir, true, ".gz" );
        Path agpfile  = copyRandomized( "uk/ac/ebi/ena/assembly/valid_flatfileagp.txt", input_dir, true, ".agp.gz" );
        Path infofile = Files.write( Files.createTempFile( input_dir, "INFO", "FILE" ), 
                                     getAssemblyInfo( String.format( "SOME-FANCY-NAME %X", System.currentTimeMillis() ) ).getBytes( StandardCharsets.UTF_8 ), 
                                     StandardOpenOption.TRUNCATE_EXISTING );

        
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.userName = usernm;
        parameters.password = passwd;
        parameters.context  = ContextE.genome.toString();
        parameters.centerName = "C E N T E R N A M E";
        parameters.inputDir   = input_dir.toString();
        parameters.outputDir  = createOutputFolder().getPath();
        parameters.manifest   = Files.write( File.createTempFile( "MANIFEST", "FILE" ).toPath(), 
                                             ( "INFO " + input_dir.relativize( infofile ) + "\n"
                                             + "FLATFILE " + input_dir.relativize( flatfile ) + "\n"
                                             + "AGP " + input_dir.relativize(  agpfile ) ).getBytes( StandardCharsets.UTF_8 ), 
                                             StandardOpenOption.TRUNCATE_EXISTING ).toString();
        parameters.test = true;
        parameters.validate = true;
        parameters.submit   = true;
        
        WebinCli webinCli = new WebinCli();
        webinCli.init( parameters );
        webinCli.setTestMode( true );
        webinCli.execute();

    }
    
    
    @Test public void
    testSequenceSubmission() throws Exception
    {
        Path input_dir = createOutputFolder().toPath();
        
        Path tabfile = copyRandomized( "uk/ac/ebi/ena/template/tsvfile/ERT000003-EST.tsv.gz", input_dir, false );
        Path infofile = Files.write( Files.createTempFile( input_dir, "INFO", "FILE" ), 
                                     getAssemblyInfo( String.format( "SOME-FANCY-NAME %X", System.currentTimeMillis() ) ).getBytes( StandardCharsets.UTF_8 ), 
                                     StandardOpenOption.TRUNCATE_EXISTING );

        
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.userName = usernm;
        parameters.password = passwd;
        parameters.context  = ContextE.sequence.toString();
        parameters.centerName = "C E N T E R N A M E";
        parameters.inputDir   = input_dir.toString();
        parameters.outputDir  = createOutputFolder().getPath();
        parameters.manifest   = Files.write( File.createTempFile( "MANIFEST", "FILE" ).toPath(), 
                                             ( "INFO " + input_dir.relativize( infofile ) + "\n"
                                             + "TAB " + input_dir.relativize( tabfile ) ).getBytes( StandardCharsets.UTF_8 ), 
                                             StandardOpenOption.TRUNCATE_EXISTING ).toString();
        parameters.test = true;
        parameters.validate = true;
        parameters.submit   = true;
        
        WebinCli webinCli = new WebinCli();
        webinCli.init( parameters );
        webinCli.setTestMode( true );
        webinCli.execute();
    }

    //TODO update when inputDir supported
    @Test public void
    testTranscriptomeSubmission() throws Exception
    {
        Path input_dir = createOutputFolder().toPath();

        Path fastafile = copyRandomized( "uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.fasta.gz", input_dir, false );
        Path infofile = Files.write( Files.createTempFile( input_dir, "INFO", "FILE.info" ), 
                                     getAssemblyInfo( String.format( "SOME-FANCY-NAME %X", System.currentTimeMillis() ) ).getBytes( StandardCharsets.UTF_8 ), 
                                     StandardOpenOption.TRUNCATE_EXISTING );
        
        WebinCli.Params parameters = new WebinCli.Params();
        parameters.userName = usernm;
        parameters.password = passwd;
        parameters.context  = ContextE.transcriptome.toString();
        parameters.centerName = "C E N T E R N A M E";
        parameters.inputDir   = input_dir.toString();
        parameters.outputDir  = createOutputFolder().getPath();
        parameters.manifest   = Files.write( File.createTempFile( "MANIFEST", "FILE" ).toPath(), 
                                             ( "INFO " + /*input_dir.relativize( */infofile /*)*/ + "\n"
                                             + "FASTA " + /*input_dir.relativize( */fastafile /*)*/ ).getBytes( StandardCharsets.UTF_8 ), 
                                             StandardOpenOption.TRUNCATE_EXISTING ).toString();
        parameters.test = true;
        parameters.validate = true;
        parameters.submit   = true;
        
        WebinCli webinCli = new WebinCli();
        webinCli.init( parameters );
        webinCli.setTestMode( true );
        webinCli.execute();
    }

    
    private File
    createOutputFolder() throws IOException
    {
        File output = File.createTempFile( "test", "test" );
        Assert.assertTrue( output.delete() );
        Assert.assertTrue( output.mkdirs() );
        return output;
    }
}
