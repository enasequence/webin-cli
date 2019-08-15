package uk.ac.ebi.ena.webin.cli.assembly;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.valid;
import static uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderFileSuffixTester.invalid;

public class TranscriptomeAssemblyManifestReaderFileSuffixTest {

  @Test
  public void testValidFileSuffix() {
    Class<TranscriptomeAssemblyManifestReader> manifestReader = TranscriptomeAssemblyManifestReader.class;
    valid(manifestReader, TranscriptomeManifest.FileType.FASTA, ".fasta.gz");
    valid(manifestReader, TranscriptomeManifest.FileType.FLATFILE, ".txt.gz");
  }

  @Test
  public void testInvalidFileSuffix() {
    Class<TranscriptomeAssemblyManifestReader> manifestReader = TranscriptomeAssemblyManifestReader.class;
    // Invalid suffix before .gz
    invalid(manifestReader, TranscriptomeManifest.FileType.FASTA, ".INVALID.gz");
    // No .gz
    invalid(manifestReader, TranscriptomeManifest.FileType.FASTA, ".fasta");
    invalid(manifestReader, TranscriptomeManifest.FileType.FLATFILE, ".txt");
  }

}
