# Webin command line submission interface (Webin-CLI)

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Introduction

Data submissions to ENA can be made using the Webin command line submission interface (Webin-CLI). Webin submission account credentials are required to use the program.

The following types of submissions are supported:

- genome assemblies (FASTA, flat file, or FASTA + GFF3 annotation)
- transcriptome assemblies
- annotated sequences
- read data submissions (Fastq, BAM, CRAM)
- taxonomy reference sets
- environmental sequence sets (polysample)

For further information about Webin-CLI please refer to:

<https://ena-docs.readthedocs.io/en/latest/submit/general-guide/webin-cli.html>

## Executable Java JAR

The latest version of the Webin-CLI can be downloaded from:

<https://github.com/enasequence/webin-cli/releases>

The program requires `Java 17` or a newer version. Please go here to learn more:

<https://openjdk.org/install/>

The program is run using the java command:

```
java -jar <webin-cli-jar-file> <options>
```

for example:

```
java -jar webin-cli-9.0.3.jar -help
```

To increase the memory available to Webin-CLI please use the -Xms java option:

```
java -Xms2G -jar <webin-cli-jar-file> -help
```

## Docker

### Run

Since version 1.8.12 Webin-CLI is available as a docker image:

```
docker pull enasequence/webin-cli
docker run --rm -v <local data directory>:/data enasequence/webin-cli -help
```

To increase the memory available to Webin-CLI please set the JAVA_TOOL_OPTIONS environment variable:

```
docker run --rm -v <local data directory>:/data -e JAVA_TOOL_OPTIONS="-Xms2G" enasequence/webin-cli -help
```

### Publishing

- Create docker image with default tags by running `gradle dockerTag`

## Testing

Testing requires the following environmental variables to be set:
- webin-cli-username or webinCliUsername
- webin-cli-password or webinCliPassword

## Library Jar Publishing

To publish webin-cli as a library :

```
gradle publish
```

## Genome Assembly Manifest Fields

The genome context (`-context genome`) supports the following data-file fields in the manifest:

| Field             | Required/Optional | Description                                                     | Accepted suffixes                             |
|-------------------|-------------------|-----------------------------------------------------------------|-----------------------------------------------|
| `FASTA`           | See file groups   | Sequences in a FASTA file                                       | `.fasta.gz`, `.fa.gz`, `.fna.gz` (+ `.bz2`)  |
| `FLATFILE`        | See file groups   | Sequences and annotation in an EMBL flat file                   | `.gz`, `.bz2`                                 |
| `GFF3`            | See file groups   | Genome annotation in a GFF3 file (paired with `FASTA`)         | `.gff3.gz`, `.gff.gz` (+ `.bz2`)             |
| `CHROMOSOME_LIST` | Optional          | Tab-delimited list of chromosomes / named top-level sequences   | `.gz`, `.bz2`                                 |
| `UNLOCALISED_LIST`| Optional          | Tab-delimited list of unlocalised sequences                     | `.gz`, `.bz2`                                 |

### Supported file-group combinations

| Files                                        | Notes                                            |
|----------------------------------------------|--------------------------------------------------|
| `FASTA`                                      | Sequence-only assembly                           |
| `FASTA` + `CHROMOSOME_LIST`                  | Chromosomal assembly without annotation          |
| `FASTA` + `CHROMOSOME_LIST` + `UNLOCALISED_LIST` | Chromosomal assembly with unlocalised sequences |
| `FLATFILE`                                   | Annotated assembly via EMBL flat file            |
| `FLATFILE` + `CHROMOSOME_LIST`               | Chromosomal annotated assembly                   |
| `FLATFILE` + `CHROMOSOME_LIST` + `UNLOCALISED_LIST` | Chromosomal annotated assembly with unlocalised sequences |
| `FASTA` + `GFF3`                             | GFF3-annotated assembly (**new in 9.0.3**)       |
| `FASTA` + `GFF3` + `CHROMOSOME_LIST`         | Chromosomal GFF3-annotated assembly              |
| `FASTA` + `GFF3` + `CHROMOSOME_LIST` + `UNLOCALISED_LIST` | Chromosomal GFF3-annotated assembly with unlocalised sequences |

> **Note:** GFF3 annotation files must be gzip- or bzip2-compressed (`.gff3.gz`, `.gff.gz`,
> `.gff3.bz2`, `.gff.bz2`). GFF3 is paired with `FASTA` only; it cannot be combined with `FLATFILE`.
> GFF3 is not supported for `primary metagenome`, `binned metagenome`, or `clinical isolate assembly` types.

## Support

Please contact [ENA helpdesk](https://www.ebi.ac.uk/ena/browser/support) for bugs, features and other issues related to this tool.
