package uk.ac.ebi.ena.webin.cli.context.sequence;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;

public class SequenceManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<SequenceManifestReader> manifestReader = SequenceManifestReader.class;
    valid(manifestReader, SequenceManifest.FileType.TAB, ".tsv.gz");
    valid(manifestReader, SequenceManifest.FileType.TAB, ".tab.gz");
    valid(manifestReader, SequenceManifest.FileType.FLATFILE, ".txt.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<SequenceManifestReader> manifestReader = SequenceManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, SequenceManifest.FileType.TAB, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, SequenceManifest.FileType.TAB, ".tsv");
    invalid(manifestReader, SequenceManifest.FileType.TAB, ".tab");
    invalid(manifestReader, SequenceManifest.FileType.FLATFILE, ".txt");
  }
}
