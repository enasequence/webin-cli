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
package uk.ac.ebi.ena.webin.cli;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;

import com.google.common.reflect.ClassPath;

import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.assembly.SequenceAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.assembly.TranscriptomeAssemblyManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest;
import uk.ac.ebi.ena.webin.cli.upload.ASCPService;

public class
WebinCliTest
{

    private String
    getRawReadsInfoFields()
    {
        return    RawReadsManifest.Field.STUDY             + " SRP052303\n"
                + RawReadsManifest.Field.SAMPLE            + " ERS2554688\n"
                + RawReadsManifest.Field.PLATFORM          + " ILLUMINA\n"
                + RawReadsManifest.Field.INSTRUMENT        + " unspecifieD\n"
                + RawReadsManifest.Field.INSERT_SIZE       + " 1\n"
                + RawReadsManifest.Field.LIBRARY_NAME      + " YOBA LIB\n"
                + RawReadsManifest.Field.LIBRARY_STRATEGY  + " CLONEEND\n"
                + RawReadsManifest.Field.LIBRARY_SOURCE    + " OTHER\n"
                + RawReadsManifest.Field.LIBRARY_SELECTION + " Inverse rRNA selection\n"
                + RawReadsManifest.Field.DESCRIPTION       + " Some reads description\n"
                + RawReadsManifest.Field.NAME              + " " + WebinCliTestUtils.createName() + "\n ";
    }

    private String
    getGenomeManifestFields( String name )
    {
        return    GenomeAssemblyManifestReader.Field.ASSEMBLYNAME + " " + name + "\n"
                + GenomeAssemblyManifestReader.Field.COVERAGE     + " 45\n"
                + GenomeAssemblyManifestReader.Field.PROGRAM      + " assembly\n"
                + GenomeAssemblyManifestReader.Field.PLATFORM     + " fghgf\n"
                + GenomeAssemblyManifestReader.Field.MINGAPLENGTH + " 34\n"
                + GenomeAssemblyManifestReader.Field.MOLECULETYPE + " genomic DNA\n"
                + GenomeAssemblyManifestReader.Field.SAMPLE       + " SAMN04526268\n"
                + GenomeAssemblyManifestReader.Field.STUDY        + " PRJEB20083\n"
                + GenomeAssemblyManifestReader.Field.RUN_REF      + " ERR2836762, ERR2836753, SRR8083599\n"
                + GenomeAssemblyManifestReader.Field.ANALYSIS_REF + " ERZ690501, ERZ690500\n"
                + GenomeAssemblyManifestReader.Field.DESCRIPTION  + " Some genome assembly description\n";
    }

    private String
    getTranscriptomeManifestFields(String name)
    {
        return    TranscriptomeAssemblyManifestReader.Field.ASSEMBLYNAME + " " + name + "\n"
                + TranscriptomeAssemblyManifestReader.Field.PROGRAM      + " assembly\n"
                + TranscriptomeAssemblyManifestReader.Field.PLATFORM     + " fghgf\n"
                + TranscriptomeAssemblyManifestReader.Field.SAMPLE       + " SAMN04526268\n"
                + TranscriptomeAssemblyManifestReader.Field.STUDY        + " PRJEB20083\n"
                + TranscriptomeAssemblyManifestReader.Field.RUN_REF      + " ERR2836762, ERR2836753, SRR8083599\n"
                + TranscriptomeAssemblyManifestReader.Field.ANALYSIS_REF + " ERZ690501, ERZ690500\n"
                + TranscriptomeAssemblyManifestReader.Field.DESCRIPTION  + " Some transcriptome assembly description\n";
    }

    private String
    getSequenceManifestFields(String name)
    {
        return    SequenceAssemblyManifestReader.Field.NAME         + " " + name + "\n"
                + SequenceAssemblyManifestReader.Field.STUDY        + " PRJEB20083\n"
                + SequenceAssemblyManifestReader.Field.RUN_REF      + " ERR2836762, ERR2836753, SRR8083599\n"
                + SequenceAssemblyManifestReader.Field.ANALYSIS_REF + " ERZ690501, ERZ690500\n"
                + SequenceAssemblyManifestReader.Field.DESCRIPTION  + " Some sequence assembly description\n";
    }


    private void
    testWebinCli( WebinCliContext context, Path inputDir, Path outputDir, String manifestContents, boolean ascp ) throws Exception
    {
        WebinCliCommand parameters = new WebinCliCommand();
        parameters.context = context;
        parameters.inputDir = inputDir.toFile();
        parameters.outputDir = outputDir.toFile();
        parameters.manifest = WebinCliTestUtils.createTempFile("manifest.txt", inputDir,
                manifestContents).toFile();
        parameters.userName = System.getenv( "webin-cli-username" );
        parameters.password = System.getenv( "webin-cli-password" );
        parameters.test = true;
        parameters.validate = true;
        parameters.submit = true;
        parameters.ascp = ascp;
        WebinCli webinCli = new WebinCli();
        webinCli.execute( webinCli.init( parameters ) );
    }
    
    
    private String 
    extractConstant( String str )
    {
        return str.substring( 0, str.indexOf( "{" ) );
    }
    
    
    @Test public void
    testInputDirParam() throws Exception
    {
        WebinCliCommand parameters = new WebinCliCommand();
        parameters.inputDir  = WebinCliTestUtils.createTempFile( "input-dir-file" ).toFile();
        parameters.outputDir = WebinCliTestUtils.createTempFile( "output-dir-dile" ).toFile();
        WebinCli webinCli   = new WebinCli();
        
        try
        {
            webinCli.init( parameters );
        } catch( WebinCliException ex )
        {
            Assert.assertTrue( ex.getMessage(), ex.getMessage().startsWith( extractConstant( WebinCliMessage.Parameters.INPUT_PATH_NOT_DIR.text ) ) );
        }
    }
    
    
    @Test public void
    testOutputDirParam() throws Exception
    {
        WebinCliCommand parameters = new WebinCliCommand();
        parameters.inputDir  = WebinCliTestUtils.createTempDir();
        parameters.outputDir = WebinCliTestUtils.createTempFile( "output-dir-dile" ).toFile();
        
        WebinCli webinCli   = new WebinCli();
        try
        {
            webinCli.init( parameters );
        } catch( WebinCliException ex )
        {
            Assert.assertTrue( ex.getMessage(), ex.getMessage().startsWith( extractConstant( WebinCliMessage.Parameters.OUTPUT_PATH_NOT_DIR.text ) ) );
        }
    }

    
    @Test public void
    testDirParam() throws Exception
    {
        WebinCliCommand parameters = new WebinCliCommand();
        parameters.inputDir  = new File( "." );
        parameters.outputDir = new File( "." );
        WebinCli webinCli   = new WebinCli();
        webinCli.init( parameters );
    }

    
    @Test public void
    testRawReadsSubmissionWithInfo() throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getRawReadsInfoFields());

        testWebinCli( WebinCliContext.reads,
                      inputDir,
                      outputDir,
                      ManifestReader.Fields.INFO + " " + infofile.getFileName() + "\n" +
                              RawReadsManifest.Field.CRAM + " " + cram_file.getFileName(),
                      false );
    }
    
    
    @Test public void
    testRawReadsSubmissionWithoutInfo() throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();
        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir, false );

        testWebinCli( WebinCliContext.reads,
                      inputDir,
                      outputDir,
                      RawReadsManifest.Field.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      false );
    }


    @Test public void
    testRawReadsSubmissionWithoutInfoAscp() throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path cram_file = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/rawreads/18045_1#93.cram", inputDir, false );
        Assert.assertTrue( new ASCPService().isAvailable() );
        testWebinCli( WebinCliContext.reads,
                      inputDir,
                      outputDir,
                      RawReadsManifest.Field.CRAM + " " + cram_file.getFileName() + "\n" + getRawReadsInfoFields(),
                      true );
    }
    
    
    @Test public void
    testGenomeSubmissionWithInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileforAgp.txt", inputDir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileAgp.txt", inputDir, true, ".agp.gz" );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getGenomeManifestFields(name));

        testWebinCli( WebinCliContext.genome,
                      inputDir,
                      outputDir,
                      GenomeAssemblyManifestReader.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                      GenomeAssemblyManifestReader.Field.AGP + " " + agpfile.getFileName() + "\n" +
                      ManifestReader.Fields.INFO + " " + infofile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testGenomeSubmissionWithoutInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileforAgp.txt", inputDir, true, ".gz" );
        Path agpfile  = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_flatfileAgp.txt", inputDir, true, ".agp.gz" );

        testWebinCli( WebinCliContext.genome,
                      inputDir,
                      outputDir,
                      GenomeAssemblyManifestReader.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                      GenomeAssemblyManifestReader.Field.AGP + " " + agpfile.getFileName()  + "\n" +
                      getGenomeManifestFields(name),
                      false );
    }

    @Test public void
    testGenomeSubmissionWithError() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.txt", inputDir, true, ".gz" );

        try {
            testWebinCli(WebinCliContext.genome,
                    inputDir,
                    outputDir,
                    GenomeAssemblyManifestReader.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                    getGenomeManifestFields(name),
                    false);
        }
        catch (WebinCliException ex) {
            Assert.assertTrue(ex.getMessage().contains("Submission validation failed because of a user error"));
            Path reportFile = outputDir.resolve("genome").resolve(name).resolve("validate").resolve(flatfile.getFileName().toString() + ".report" );
            Assert.assertTrue(WebinCliTestUtils.readFile(reportFile).contains("ERROR: Invalid ID line format [ line: 1]"));
            return;
        }
        Assert.assertTrue(false);
    }

    @Test public void
    testFastaSubmission1SequenceNonPrimaryMetagenome() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_fasta_primary_metagenome.fasta.gz", inputDir, false );

        try {

        testWebinCli( WebinCliContext.genome,
                inputDir,
                outputDir,
                GenomeAssemblyManifestReader.Field.FASTA + " " + fastafile.getFileName() + "\n" +
                        getGenomeManifestFields(name),
                false );
        }
        catch (WebinCliException ex) {
            Assert.assertTrue(ex.getMessage().contains("Invalid number of sequences : 1, Minimum number of sequences for CONTIG is: 2"));
        }
    }

    @Test public void
    testFastaSubmission1SequencePrimaryMetagenome() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/assembly/valid_fasta_primary_metagenome.fasta.gz", inputDir, false );

        try {

            testWebinCli( WebinCliContext.genome,
                    inputDir,
                    outputDir,
                    GenomeAssemblyManifestReader.Field.FASTA + " " + fastafile.getFileName() + "\n" +
                            getGenomeManifestFields(name)+ GenomeAssemblyManifestReader.Field.ASSEMBLY_TYPE+ " primary metagenome\n",
                    false );
        }
        catch (WebinCliException ex) {
            Assert.fail();
        }
    }
    @Test public void
    testSequenceSubmissionWithInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path tabfile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/template/valid/ERT000003-EST.tsv.gz", inputDir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getSequenceManifestFields(name));

        testWebinCli( WebinCliContext.sequence,
                      inputDir,
                      outputDir,
                      SequenceAssemblyManifestReader.Field.TAB + " " + tabfile.getFileName() + "\n" +
                      ManifestReader.Fields.INFO + " " + infofile.getFileName(),
                      false );
    }
    
    
    @Test public void
    testSequenceSubmissionWithoutInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path tabfile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/template/valid/ERT000003-EST.tsv.gz", inputDir, false );

        testWebinCli( WebinCliContext.sequence,
                      inputDir,
                      outputDir,
                      SequenceAssemblyManifestReader.Field.TAB + " " + tabfile.getFileName() + "\n" +
                      getSequenceManifestFields(name),
                      false );
    }


    @Test public void
    testSequenceSubmissionWithError() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.txt", inputDir, true, ".gz" );

        try {
            testWebinCli( WebinCliContext.sequence,
                    inputDir,
                    outputDir,
                    SequenceAssemblyManifestReader.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                    getSequenceManifestFields(name),
                    false );
        }
        catch (WebinCliException ex) {
            Assert.assertTrue(ex.getMessage().contains("Submission validation failed because of a user error"));
            Path reportFile = outputDir.resolve("sequence").resolve(name).resolve("validate").resolve(flatfile.getFileName().toString() + ".report" );
            Assert.assertTrue(WebinCliTestUtils.readFile(reportFile).contains("ERROR: Invalid ID line format [ line: 1]"));
            return;
        }
        Assert.assertTrue(false);
    }

    @Test public void
    testTranscriptomeSubmissionWithInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/transcriptome/valid/valid_fasta.fasta.gz", inputDir, false );
        Path infofile =  WebinCliTestUtils.createTempFile("info.txt", inputDir, getTranscriptomeManifestFields(name));

        testWebinCli( WebinCliContext.transcriptome,
                      inputDir,
                      outputDir,
                      TranscriptomeAssemblyManifestReader.Field.FASTA + " " + fastafile.getFileName() + "\n" +
                      ManifestReader.Fields.INFO + " " + infofile.getFileName(),
                      false );
    }


    @Test public void
    testTranscriptomeSubmissionWithoutInfo() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path fastafile = WebinCliTestUtils.createTempFileFromResource("uk/ac/ebi/ena/webin/cli/transcriptome/valid/valid_fasta.fasta.gz", inputDir, false );

        testWebinCli( WebinCliContext.transcriptome,
                      inputDir,
                      outputDir,
                      TranscriptomeAssemblyManifestReader.Field.FASTA + " " + fastafile.getFileName() + "\n" +
                      getTranscriptomeManifestFields(name),
                      false );
    }

    @Test public void
    testTranscriptomeSubmissionWithError() throws Exception
    {
        String name = WebinCliTestUtils.createName();
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path outputDir = WebinCliTestUtils.createTempDir().toPath();

        Path flatfile = WebinCliTestUtils.createTempFileFromResource( "uk/ac/ebi/ena/webin/cli/assembly/invalid_flatfile.txt", inputDir, true, ".gz" );

        try {
            testWebinCli(WebinCliContext.transcriptome,
                    inputDir,
                    outputDir,
                    TranscriptomeAssemblyManifestReader.Field.FLATFILE + " " + flatfile.getFileName() + "\n" +
                    getTranscriptomeManifestFields(name),
                    false);
        }
        catch (WebinCliException ex) {
                Assert.assertTrue(ex.getMessage().contains("Submission validation failed because of a user error"));
                Path reportFile = outputDir.resolve("transcriptome").resolve(name).resolve("validate").resolve(flatfile.getFileName().toString() + ".report" );
                Assert.assertTrue(WebinCliTestUtils.readFile(reportFile).contains("ERROR: Invalid ID line format [ line: 1]"));
                return;
            }
            Assert.assertTrue(false);

    }

    @Test public void
    testGetSafeOutputDir() {
        assertEquals("AaZ", WebinCli.getSafeOutputDir("AaZ")[0]);
        assertEquals("A_AA", WebinCli.getSafeOutputDir("A&AA")[0]);
        assertEquals("A.AA", WebinCli.getSafeOutputDir("A.AA")[0]);
        assertEquals("A-AA", WebinCli.getSafeOutputDir("A-AA")[0]);
        assertEquals("A_AA", WebinCli.getSafeOutputDir("A_____AA")[0]);
        assertEquals("AA", WebinCli.getSafeOutputDir("_____AA")[0]);
        assertEquals("AA", WebinCli.getSafeOutputDir("AA_____")[0]);
        assertEquals("_", WebinCli.getSafeOutputDir("_______")[0]);
        assertEquals(".", WebinCli.getSafeOutputDir(".")[0]);

        assertEquals(".", WebinCli.getSafeOutputDir(".", "E_vermicularis_Canary_Islands_upd")[0]);
        assertEquals("E_vermicularis_Canary_Islands_upd", WebinCli.getSafeOutputDir(".", "E_vermicularis_Canary_Islands_upd")[1]);

        assertEquals("AaZ", WebinCli.getSafeOutputDir("AaZ","AaZ")[0]);
        assertEquals("A.AA", WebinCli.getSafeOutputDir("AaZ","A.AA")[1]);
    }
    
    
    @Test public void
    contextDetector() throws IOException
    {
        long start = System.currentTimeMillis();
        @SuppressWarnings( "unchecked" )
        List<Class<? extends AbstractWebinCli<?>>> contexts = ClassPath.from( getClass().getClassLoader() )
                                                      .getAllClasses()
                                                      .stream()
                                                      .filter( e -> e.getPackageName().startsWith( "uk.ac.ebi.ena.webin.cli" ) )
                                                      .filter( c -> AbstractWebinCli.class.isAssignableFrom( c.load() ) )
                                                      .filter( c -> ! c.load().isAssignableFrom( AbstractWebinCli.class ) )
                                                      .filter( c -> ! Modifier.isAbstract( c.load().getModifiers() ) )
                                                      .map( c -> 
                                                      {
                                                          try 
                                                          {
                                                              return (Class<? extends AbstractWebinCli<?>>) c.load().asSubclass( AbstractWebinCli.class );
                                                          } catch( ClassCastException | NoClassDefFoundError | UnsupportedClassVersionError e )
                                                          {
                                                              return null;
                                                          }
                                                      } )
                                                      .filter( Objects::nonNull )
                                                      .collect( Collectors.toList() );
        contexts.forEach( System.out::println ); 
        
        contexts.forEach( cpe -> 
        {
            try 
            { 
                System.out.println( cpe.newInstance().getContext() );
            } catch( Throwable t )
            {
                throw new RuntimeException( t );
            }
        } );
        System.out.println( System.currentTimeMillis() - start );
        //23331 all classes
        //  889 only uk.ac.ebi
    }
}
