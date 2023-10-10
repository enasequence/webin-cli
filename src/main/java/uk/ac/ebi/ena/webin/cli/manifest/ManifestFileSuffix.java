/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.manifest;

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
