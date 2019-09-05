package uk.ac.ebi.ena.webin.cli.context.reads;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;

public class ReadsManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<ReadsManifestReader> manifestReader = ReadsManifestReader.class;
    valid(manifestReader, ReadsManifest.FileType.BAM, ".bam");
    valid(manifestReader, ReadsManifest.FileType.CRAM, ".cram");
    valid(manifestReader, ReadsManifest.FileType.FASTQ, ".fastq.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<ReadsManifestReader> manifestReader = ReadsManifestReader.class;
    // Invalid suffix
    invalid(manifestReader, ReadsManifest.FileType.BAM, ".INVALID");
    invalid(manifestReader, ReadsManifest.FileType.CRAM, ".INVALID");
    // Not allowed .gz
    invalid(manifestReader, ReadsManifest.FileType.BAM, ".bam.gz");
    invalid(manifestReader, ReadsManifest.FileType.CRAM, ".cram.gz");
    // No .gz
    invalid(manifestReader, ReadsManifest.FileType.FASTQ, ".fastq");
  }
}
