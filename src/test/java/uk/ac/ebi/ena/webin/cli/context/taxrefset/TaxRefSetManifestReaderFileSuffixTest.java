package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;

public class TaxRefSetManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<TaxRefSetManifestReader> manifestReader = TaxRefSetManifestReader.class;
    valid(manifestReader, TaxRefSetManifest.FileType.TAB, ".tsv.gz");
    valid(manifestReader, TaxRefSetManifest.FileType.TAB, ".tab.gz");
    valid(manifestReader, TaxRefSetManifest.FileType.FASTA, ".fasta.gz");
    valid(manifestReader, TaxRefSetManifest.FileType.FASTA, ".fa.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<TaxRefSetManifestReader> manifestReader = TaxRefSetManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, TaxRefSetManifest.FileType.TAB, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, TaxRefSetManifest.FileType.TAB, ".tsv");
    invalid(manifestReader, TaxRefSetManifest.FileType.TAB, ".tab");
    invalid(manifestReader, TaxRefSetManifest.FileType.FASTA, ".txt");
    invalid(manifestReader, TaxRefSetManifest.FileType.FASTA, ".fasta");

  }
}
