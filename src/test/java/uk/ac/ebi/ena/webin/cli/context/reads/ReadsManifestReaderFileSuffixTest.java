package uk.ac.ebi.ena.webin.cli.context.reads;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadManifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;

public class ReadsManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<ReadsManifestReader> manifestReader = ReadsManifestReader.class;
    valid(manifestReader, ReadManifest.FileType.BAM, ".bam");
    valid(manifestReader, ReadManifest.FileType.CRAM, ".cram");
    valid(manifestReader, ReadManifest.FileType.FASTQ, ".fastq.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<ReadsManifestReader> manifestReader = ReadsManifestReader.class;
    // Invalid suffix
    invalid(manifestReader, ReadManifest.FileType.BAM, ".INVALID");
    invalid(manifestReader, ReadManifest.FileType.CRAM, ".INVALID");
    // Not allowed .gz
    invalid(manifestReader, ReadManifest.FileType.BAM, ".bam.gz");
    invalid(manifestReader, ReadManifest.FileType.CRAM, ".cram.gz");
    // No .gz
    invalid(manifestReader, ReadManifest.FileType.FASTQ, ".fastq");
  }
}