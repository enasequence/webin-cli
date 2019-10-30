# Webin command line submission interface (Webin-CLI)

[![Codacy Badge](https://api.codacy.com/project/badge/Grade/c4fa2bcf5593436da9ea27149f84ee6e)](https://app.codacy.com/app/enasequence/webin-cli?utm_source=github.com&utm_medium=referral&utm_content=enasequence/webin-cli&utm_campaign=badger)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

## Introduction

Data submissions to ENA can be made using the Webin command line submission interface (Webin-CLI). Webin submission account credentials are required to use the program.

The following types of submissions are supported:

- genome assemblies
- transcriptome assemblies
- annotated sequences
- read data submissions (Fastq, BAM, CRAM)

For further information about Webin-CLI please refer to:

<https://ena-docs.readthedocs.io/en/latest/submit/general-guide/webin-cli.html>

## Executable Java JAR

The latest version of the Webin-CLI can be downloaded from:

<https://github.com/enasequence/webin-cli/releases>

The program requires Java 1.8 or a newer which can be downloaded from:

<https://java.com/en/download/>


The program is run using the java command:

```
java -jar webin-cli-<version>.jar <options>
```

for example:

```
java -jar webin-cli-2.0.0.jar -help
```

To increase the memory available to Webin-CLI please use the -Xms java option:

```
java -Xms2G -jar webin-cli-2.0.0.jar -help
```

## Docker file

Since version 1.8.12 Webin-CLI is available as a docker image:

```
docker pull enasequence/webin-cli
docker run --rm -v <local data directory>:/data enasequence/webin-cli -help
```

To increase the memory available to Webin-CLI please set the JAVA_TOOL_OPTIONS environment variable:

```
docker run --rm -v <local data directory>:/data -e JAVA_TOOL_OPTIONS="-Xms2G" enasequence/webin-cli -help
```
