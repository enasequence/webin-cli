package uk.ac.ebi.ena.webin.cli.context.genome;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;

public class GenomeManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<GenomeManifestReader> manifestReader = GenomeManifestReader.class;
    valid(manifestReader, GenomeManifest.FileType.FASTA, ".fasta.gz");
    valid(manifestReader, GenomeManifest.FileType.AGP, ".agp.gz");
    valid(manifestReader, GenomeManifest.FileType.FLATFILE, ".txt.gz");
    valid(manifestReader, GenomeManifest.FileType.CHROMOSOME_LIST, ".txt.gz");
    valid(manifestReader, GenomeManifest.FileType.UNLOCALISED_LIST, ".txt.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<GenomeManifestReader> manifestReader = GenomeManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, GenomeManifest.FileType.FASTA, ".INVALID.gz");
    invalid(manifestReader, GenomeManifest.FileType.AGP, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, GenomeManifest.FileType.FASTA, ".fasta");
    invalid(manifestReader, GenomeManifest.FileType.AGP, ".agp");
    invalid(manifestReader, GenomeManifest.FileType.FLATFILE, ".txt");
    invalid(manifestReader, GenomeManifest.FileType.CHROMOSOME_LIST, ".txt");
    invalid(manifestReader, GenomeManifest.FileType.UNLOCALISED_LIST, ".txt");
  }
}
