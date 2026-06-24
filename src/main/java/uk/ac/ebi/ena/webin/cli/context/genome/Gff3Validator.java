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
package uk.ac.ebi.ena.webin.cli.context.genome;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.embl.gff3tools.cli.SequenceFormat;
import uk.ac.ebi.embl.gff3tools.exception.ReadException;
import uk.ac.ebi.embl.gff3tools.exception.ValidationException;
import uk.ac.ebi.embl.gff3tools.exception.WriteException;
import uk.ac.ebi.embl.gff3tools.gff3.reader.GFF3FileReader;
import uk.ac.ebi.embl.gff3tools.validation.ValidationEngine;
import uk.ac.ebi.embl.gff3tools.validation.ValidationEngineBuilder;
import uk.ac.ebi.embl.gff3tools.validation.provider.CompositeSequenceProvider;
import uk.ac.ebi.embl.gff3tools.validation.provider.FileSequenceSource;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;

/**
 * Orchestrates client-side GFF3 validation using the {@code gff3tools} programmatic API.
 *
 * <p>For every GFF3 submission file the validator builds a {@link ValidationEngine} (with all
 * auto-discovered builtin validators) and a {@link CompositeSequenceProvider} backed by the
 * manifest's FASTA file(s) via {@link FileSequenceSource}, which handles gzip decompression
 * internally. Cross-validation rules (translation, location, gap) resolve sequences by the GFF3
 * column-1 seqId. Collected errors and parsing warnings are written to the per-file {@code .report}
 * configured by the executor.
 *
 * <p><b>ENA submission protocol requirement:</b> FASTA files submitted alongside a GFF3 annotation
 * must use the JSON header format {@code >ID | {json}} (e.g. {@code >SEQ1 | {}}). The {@code |}
 * separator is mandatory — it is enforced by the ENA submission protocol, not merely a library
 * constraint. Plain headers without {@code |} are rejected.
 *
 * <p>Only gzip-compressed FASTA ({@code .fasta.gz}) is supported alongside GFF3. bzip2-compressed
 * FASTA is not accepted because {@code gff3tools}'s {@link FileSequenceSource} decompresses only
 * gzip; bzip2 files would be passed raw to the FASTA reader, producing an opaque failure.
 */
public class Gff3Validator {

  private static final Logger log = LoggerFactory.getLogger(Gff3Validator.class);

  /**
   * Validates every GFF3 file declared in the manifest.
   *
   * @return {@code true} if all GFF3 files validated without errors, {@code false} otherwise.
   */
  public boolean validate(
      List<? extends SubmissionFile<?>> gff3Files,
      List<? extends SubmissionFile<?>> fastaFiles) {
    if (gff3Files == null || gff3Files.isEmpty()) {
      return true;
    }

    boolean valid = true;
    for (SubmissionFile<?> gff3File : gff3Files) {
      valid &= validateFile(gff3File, fastaFiles);
    }
    return valid;
  }

  private boolean validateFile(
      SubmissionFile<?> gff3File, List<? extends SubmissionFile<?>> fastaFiles) {

    File gff3 = gff3File.getFile();

    // Guard: bzip2-compressed FASTA is not supported alongside GFF3 — gff3tools's
    // FileSequenceSource decompresses only gzip; bzip2 would be passed raw to the FASTA
    // reader, producing an opaque failure. Check all FASTA files before reporting.
    if (fastaFiles != null) {
      List<ValidationException> bzip2Errors = new ArrayList<>();
      for (SubmissionFile<?> fastaFile : fastaFiles) {
        String fastaName = fastaFile.getFile().getName();
        if (fastaName.endsWith(".bz2") || fastaName.endsWith(".bzip2")) {
          bzip2Errors.add(
              new ValidationException(
                  "bzip2-compressed FASTA is not supported with GFF3 annotation; "
                      + "use gzip compression (.fasta.gz): "
                      + fastaName));
        }
      }
      if (!bzip2Errors.isEmpty()) {
        writeReport(gff3File.getReportFile(), bzip2Errors, List.of());
        log.info("GFF3 file {} validation failed: bzip2 FASTA not supported.", gff3.getName());
        return false;
      }
    }

    CompositeSequenceProvider sequenceProvider = new CompositeSequenceProvider();
    if (fastaFiles != null) {
      for (SubmissionFile<?> fastaFile : fastaFiles) {
        // FileSequenceSource handles gzip decompression internally (gff3tools >= 4.6.2)
        // and cleans up any temporary decompressed files when engine.close() is called.
        sequenceProvider.addSource(
            new FileSequenceSource(fastaFile.getFile().toPath(), SequenceFormat.fasta, null));
      }
    }

    List<ValidationException> errors = new ArrayList<>();
    List<ValidationException> warnings = new ArrayList<>();

    // The ValidationEngine (try-with-resources) takes ownership of the sequenceProvider via
    // withProvider(...) and closes it when engine.close() is called — including cleaning up
    // any temp files created by FileSequenceSource for decompressing gzipped FASTA.
    try (Reader reader = FileUtils.getBufferedReader(gff3);
        ValidationEngine engine =
            new ValidationEngineBuilder().failFast(false).withProvider(sequenceProvider).build();
        GFF3FileReader gff3Reader = new GFF3FileReader(engine, reader, gff3.toPath())) {

      gff3Reader.readHeader();
      // Validation runs as a side-effect of reading; annotations are not needed here.
      gff3Reader.read(annotation -> {});

      errors.addAll(engine.getCollectedErrors());
      warnings.addAll(engine.getParsingWarnings());
    } catch (ValidationException ex) {
      // A fatal validation/syntactic error stopped processing before completion.
      errors.add(ex);
    } catch (ReadException | WriteException ex) {
      errors.add(new ValidationException("Failed to read GFF3 file: " + ex.getMessage()));
    } catch (IOException ex) {
      errors.add(new ValidationException("Failed to read GFF3 file: " + ex.getMessage()));
    } catch (RuntimeException ex) {
      log.error("Unexpected error during GFF3 validation of {}", gff3.getName(), ex);
      errors.add(
          new ValidationException(
              "GFF3 validation failed unexpectedly: "
                  + (ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName())));
    }

    writeReport(gff3File.getReportFile(), errors, warnings);

    if (!errors.isEmpty()) {
      log.info("GFF3 file {} validation failed with {} error(s).", gff3.getName(), errors.size());
      return false;
    }
    return true;
  }

  private void writeReport(
      File reportFile, List<ValidationException> errors, List<ValidationException> warnings) {
    if (reportFile == null) {
      return;
    }

    List<String> lines = new ArrayList<>();
    for (ValidationException error : errors) {
      lines.add(formatMessage("ERROR", error));
    }
    for (ValidationException warning : warnings) {
      lines.add(formatMessage("WARNING", warning));
    }

    try (Writer writer =
        Files.newBufferedWriter(
            reportFile.toPath(),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING)) {
      for (String line : lines) {
        writer.write(line);
        writer.write(System.lineSeparator());
      }
    } catch (IOException ex) {
      log.warn("Failed to write GFF3 validation report {}: {}", reportFile, ex.getMessage());
    }
  }

  private String formatMessage(String severity, ValidationException exception) {
    StringBuilder sb = new StringBuilder();
    sb.append(severity).append(": ").append(exception.getMessage());
    if (exception.getLine() > 0) {
      sb.append(" [line: ").append(exception.getLine()).append("]");
    }
    return sb.toString();
  }
}
