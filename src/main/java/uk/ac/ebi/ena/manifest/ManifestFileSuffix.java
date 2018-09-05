package uk.ac.ebi.ena.manifest;

import java.util.Arrays;
import java.util.List;

public interface ManifestFileSuffix {

    String GZIP_FILE_SUFFIX = ".gz";
    String BZIP2_FILE_SUFFIX = ".bz2";

    List<String> GZIP_OR_BZIP_FILE_SUFFIX = Arrays.asList(
            GZIP_FILE_SUFFIX,
            BZIP2_FILE_SUFFIX);

    List<String> AGP_FILE_SUFFIX = Arrays.asList(
            ".agp" + GZIP_FILE_SUFFIX,
            ".agp" + BZIP2_FILE_SUFFIX);

    List<String> BAM_FILE_SUFFIX = Arrays.asList(
            ".bam");

    List<String> CRAM_FILE_SUFFIX = Arrays.asList(
            ".cram");

    List<String> FASTA_FILE_SUFFIX = Arrays.asList(
        ".fasta" + GZIP_FILE_SUFFIX,
        ".fas" + GZIP_FILE_SUFFIX,
        ".fsa" + GZIP_FILE_SUFFIX,
        ".fna" + GZIP_FILE_SUFFIX,
        ".fa" + GZIP_FILE_SUFFIX,
        ".fasta" + BZIP2_FILE_SUFFIX,
        ".fas" + BZIP2_FILE_SUFFIX,
        ".fsa" + BZIP2_FILE_SUFFIX,
        ".fna" + BZIP2_FILE_SUFFIX,
        ".fa" + BZIP2_FILE_SUFFIX);

    List<String> TAB_FILE_SUFFIX = Arrays.asList(
        ".tab" + GZIP_FILE_SUFFIX,
        ".tsv" + GZIP_FILE_SUFFIX,
        ".tab" + BZIP2_FILE_SUFFIX,
        ".tsv" + BZIP2_FILE_SUFFIX);

}
