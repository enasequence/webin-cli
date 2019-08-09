package uk.ac.ebi.ena.webin.cli.assembly;

import org.junit.Test;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

import java.io.File;

public class ManifestFileSuffixTest {

  private static final ValidatorBuilder<GenomeAssemblyWebinCli>
      genomeValidatorBuilder =
          new ValidatorBuilder(GenomeAssemblyWebinCli.class)
              .createOutputDirs(false)
              .manifestMetadataProcessors(false)
              .manifestValidateMandatory(false)
              .manifestValidateFileExists(false)
              .manifestValidateFileCount(false);

    private static final ValidatorBuilder<TranscriptomeAssemblyWebinCli>
            transcriptomeValidatorBuilder =
            new ValidatorBuilder(TranscriptomeAssemblyWebinCli.class)
                    .createOutputDirs(false)
                    .manifestMetadataProcessors(false)
                    .manifestValidateMandatory(false)
                    .manifestValidateFileExists(false)
                    .manifestValidateFileCount(false);


    private static final ValidatorBuilder<SequenceAssemblyWebinCli>
            sequenceValidatorBuilder =
            new ValidatorBuilder(SequenceAssemblyWebinCli.class)
                    .createOutputDirs(false)
                    .manifestMetadataProcessors(false)
                    .manifestValidateMandatory(false)
                    .manifestValidateFileExists(false)
                    .manifestValidateFileCount(false);

    private static <FileType extends Enum<FileType>> void invalidSuffix(
            ValidatorBuilder validatorBuilder, FileType fileType, String fileName) {
    File manifest = new ManifestBuilder().file(fileType, fileName).build();
    validatorBuilder.readManifestThrows(manifest, WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR);
  }

  private static <FileType extends Enum<FileType>> void validSuffix(
          ValidatorBuilder validatorBuilder, FileType fileType, String fileName) {
    File manifest = new ManifestBuilder().file(fileType, fileName).build();
    validatorBuilder.readManifest(manifest);
  }

  @Test
  public void testInvalidFileSuffixGenome() {
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.FASTA, ".INVALID.gz");
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.AGP, ".INVALID.gz");
    // No .gz
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.FASTA, ".fasta");
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.AGP, ".agp");
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.FLATFILE, ".txt");
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.CHROMOSOME_LIST, ".txt");
    invalidSuffix(genomeValidatorBuilder, GenomeManifest.FileType.UNLOCALISED_LIST, ".txt");
  }

    @Test
    public void testValidFileSuffixGenome() {
        validSuffix(genomeValidatorBuilder, GenomeManifest.FileType.FASTA, ".fasta.gz");
        validSuffix(genomeValidatorBuilder, GenomeManifest.FileType.AGP, ".agp.gz");
        validSuffix(genomeValidatorBuilder, GenomeManifest.FileType.FLATFILE, ".txt.gz");
        validSuffix(genomeValidatorBuilder, GenomeManifest.FileType.CHROMOSOME_LIST, ".txt.gz");
        validSuffix(genomeValidatorBuilder, GenomeManifest.FileType.UNLOCALISED_LIST, ".txt.gz");
    }

    @Test
    public void testInvalidFileSuffixSequence() {
        invalidSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.TAB, ".INVALID.gz");
        // No .gz
        invalidSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.TAB, ".tsv");
        invalidSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.TAB, ".tab");
        invalidSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.FLATFILE, ".txt");
    }

    @Test
    public void testValidFileSuffixSequence() {
        validSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.TAB, ".tsv.gz");
        validSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.TAB, ".tab.gz");
        validSuffix(sequenceValidatorBuilder, SequenceManifest.FileType.FLATFILE, ".txt.gz");
    }

    @Test
    public void testInvalidFileSuffixTranscriptome() {
        invalidSuffix(transcriptomeValidatorBuilder, TranscriptomeManifest.FileType.FASTA, ".INVALID.gz");
        // No .gz
        invalidSuffix(transcriptomeValidatorBuilder, TranscriptomeManifest.FileType.FASTA, ".fasta");
        invalidSuffix(transcriptomeValidatorBuilder, TranscriptomeManifest.FileType.FLATFILE, ".txt");
    }

    @Test
    public void testValidFileSuffixTranscriptome() {
        validSuffix(transcriptomeValidatorBuilder, TranscriptomeManifest.FileType.FASTA, ".fasta.gz");
        validSuffix(transcriptomeValidatorBuilder, TranscriptomeManifest.FileType.FLATFILE, ".txt.gz");
    }
}
