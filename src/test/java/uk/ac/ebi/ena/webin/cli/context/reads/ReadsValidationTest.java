package uk.ac.ebi.ena.webin.cli.context.reads;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static uk.ac.ebi.ena.webin.cli.WebinCliTestUtils.resourceDir;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.WebinCliExecutorBuilder;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFiles;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;

public class ReadsValidationTest {

  private static final File RESOURCE_DIR = resourceDir("uk/ac/ebi/ena/webin/cli/rawreads");

  private static ManifestBuilder manifestBuilder() {
    return new ManifestBuilder()
        .field("STUDY", "test")
        .field("SAMPLE", "test")
        .field("PLATFORM", "ILLUMINA")
        .field("INSTRUMENT", "unspecified")
        .field("NAME", "test")
        .field("INSERT_SIZE", "1")
        .field("LIBRARY_STRATEGY", "CLONEEND")
        .field("LIBRARY_SOURCE", "OTHER")
        .field("LIBRARY_SELECTION", "Inverse rRNA selection");
  }

  private static final WebinCliExecutorBuilder<ReadsManifest, ReadsValidationResponse> executorBuilder =
      new WebinCliExecutorBuilder(ReadsManifest.class)
          .manifestMetadataProcessors(false);

  @Test
  public void
  manifestTwoBAMs() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.BAM, "file1.bam")
            .file(FileType.BAM, "file2.bam")
            .build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestTwoCRAMs() {
    File manifestFile =
        manifestBuilder().
            file(FileType.CRAM, "file1.cram").
            file(FileType.CRAM, "file2.cram").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }


  @Test
  public void
  manifestMixingFormats() {
    File manifestFile =
        manifestBuilder().
            file(FileType.BAM, "file1.bam").
            file(FileType.CRAM, "file2.cram").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestNoFiles() {
    File manifestFile =
        manifestBuilder().build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  fastqFileDoesNotExist() throws IOException {
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, "doesnotexisits.fastq.gz.bz2").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestFileIsDirectory() throws IOException {
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, createOutputFolder() + " PHRED_33").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestNoPath() {
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, "").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestNonASCIIPath() throws IOException {
    URL url = ReadsWebinCliTest.class.getClassLoader()
        .getResource("uk/ac/ebi/ena/webin/cli/rawreads/MG23S_431.fastq.gz");
    File gz = new File(URLDecoder.decode(url.getFile(), "UTF-8"));

    Path file = Files
        .write(Files.createTempFile("FILE", "Š.fq.gz"), Files.readAllBytes(gz.toPath()),
            StandardOpenOption.TRUNCATE_EXISTING);
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, file).
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestFastqNoScoring() {
    File manifestFile =
        manifestBuilder().
            file(FileType.FASTQ, "file.fq.gz").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestBAMScoring() {
    File manifestFile =
        manifestBuilder().
            file(FileType.BAM, "PHRED_33 file.fq.gz").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestBAMCompression() {
    File manifestFile =
        manifestBuilder().
            file(FileType.BAM, "file.fq.gz").
            build();

    assertThatThrownBy(() -> executorBuilder.manifestMetadataProcessors(false).readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestCRAMScoring() {
    File manifestFile =
        manifestBuilder().
            file(FileType.CRAM, "PHRED_33 file.fq.gz").
            build();

    assertThatThrownBy(() -> executorBuilder.readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  manifestCRAMCompression() {
    File manifestFile =
        manifestBuilder().
            file(FileType.CRAM, "file.fq.gz").
            build();

    assertThatThrownBy(() -> executorBuilder.manifestMetadataProcessors(false).readManifest(manifestFile, RESOURCE_DIR))
        .isInstanceOf(WebinCliException.class)
        .hasMessageStartingWith("Invalid manifest file");
  }

  @Test
  public void
  testCorrectBAM() {
    File manifestFile =
        manifestBuilder().file(FileType.BAM, "OUTO500m_MetOH_narG_OTU18.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();
    executor.validateSubmission();
  }

  @Test
  public void
  testIncorrectBAM() {
    File manifestFile =
        manifestBuilder().file(FileType.BAM, "m54097_170904_165950.subreads.bam").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.BAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  testCorrectFastq() {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission();
  }

  @Test
  public void
  sameFilePairedFastq() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz")
            .file(FileType.FASTQ, "ZeroCycle_ES0_TTCCTT20NGA_0.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  samePairedFastq() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "EP0_GTTCCTT_0.txt.gz")
            .file(FileType.FASTQ, "EP0_GTTCCTT_0.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  pairedFastq() throws IOException {
    File manifestFile =
        manifestBuilder().file(FileType.FASTQ, "EP0_GTTCCTT_0.txt.gz").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();
    executor.validateSubmission();
//    String lines = new String( Files.readAllBytes( executor.readSubmissionBundle().getXMLFileList().stream().filter(e-> SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
//        StandardCharsets.UTF_8 );
//    Assert.assertTrue( lines.contains( "<PAIRED" ) );
  }

  @Test
  public void
  fastqPair() throws IOException {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "EP0_GTTCCTT_S1.txt.gz")
            .file(FileType.FASTQ, "EP0_GTTCCTT_S2.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);
    executor.validateSubmission();
//    String lines = new String( Files.readAllBytes( executor.readSubmissionBundle().getXMLFileList().stream().filter(e-> SubmissionXMLFileType.EXPERIMENT.equals( e.getType() ) ).findFirst().get().getFile().toPath() ),
//        StandardCharsets.UTF_8 );
//    Assert.assertTrue( lines.contains( "<PAIRED" ) );
  }

  @Test
  public void
  fastqFalsePair() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "EP0_GTTCCTT_P1.txt.gz")
            .file(FileType.FASTQ, "EP0_GTTCCTT_P2.txt.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(2);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isEqualTo(2);

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  testIncorrectFastq() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.FASTQ, "MG23S_431.fastq.gz")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.FASTQ).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  testIncorrectCram() {
    File manifestFile =
        manifestBuilder()
            .file(FileType.CRAM, "15194_1#135.cram")
            .build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();

    assertThatThrownBy(() -> executor.validateSubmission())
        .isInstanceOf(WebinCliException.class)
        .hasMessage("");
  }

  @Test
  public void
  testCorrectCram() {
    File manifestFile =
        manifestBuilder().file(FileType.CRAM, "18045_1#93.cram").build();

    WebinCliExecutor<ReadsManifest, ReadsValidationResponse> executor =
        executorBuilder.readManifest(manifestFile, RESOURCE_DIR);
    SubmissionFiles submissionFiles = executor.getManifestReader().getManifest().files();
    assertThat(submissionFiles.get().size()).isEqualTo(1);
    assertThat(submissionFiles.get(FileType.CRAM).size()).isOne();
    executor.validateSubmission();
  }

/*
    @Test( timeout = 200_000 ) public void
    openSamSmall() throws UnsupportedEncodingException
    {
        final SamReaderFactory factory =
                SamReaderFactory.makeDefault().enable( SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS ).validationStringency( ValidationStringency.LENIENT );

        URL url = ReadsWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/webin/cli/rawreads/16258_6#32.cram" );
        final SamInputResource resource = SamInputResource.of( new File( URLDecoder.decode( url.getFile(), "UTF-8" ) ) );
        ENAReferenceSource rs = new ENAReferenceSource( new String[] { } );
        rs.setLoggerWrapper( new LoggerWrapper() {

            @Override public void
            debug( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            warn( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            error( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

            @Override public void
            info( Object... messageParts )
            {
                System.out.println( Arrays.asList( messageParts ) );
            }

        } );
        factory.referenceSource( rs );
        final SamReader myReader = factory.open(resource);

        for (final SAMRecord samRecord : myReader)
        {
            System.err.print(samRecord);
        }

    }
*/

  private File
  createOutputFolder() throws IOException {
    File output = File.createTempFile("test", "test");
    Assert.assertTrue(output.delete());
    Assert.assertTrue(output.mkdirs());
    return output;
  }
}
