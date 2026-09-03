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
package uk.ac.ebi.ena.webin.cli.context.annotation;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.embl.gff3tools.exception.ValidationException;
import uk.ac.ebi.embl.gff3tools.gff3.reader.GFF3FileReader;
import uk.ac.ebi.embl.gff3tools.validation.ValidationEngine;
import uk.ac.ebi.embl.gff3tools.validation.ValidationEngineBuilder;
import uk.ac.ebi.embl.gff3tools.validation.meta.RuleSeverity;
import uk.ac.ebi.embl.gff3tools.validation.provider.CompositeSequenceProvider;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFile;

/**
 * Validates GFF3 files for the annotation (DECOUPLED_ANNOTATION) submission context.
 *
 * <p>Unlike the genome context, annotation submissions do not include a FASTA file — the sequences
 * are referenced by {@code PRIMARY_ID} (an existing ENA assembly accession). The GFF3 is validated
 * structurally only: the {@link CompositeSequenceProvider} is left empty, so cross-validation rules
 * that resolve sequences (translation, location, gap) will report unresolved seqId errors for any
 * sequence not found, but the structural/syntactic GFF3 checks still run. Full cross-validation
 * against the referenced assembly happens server-side in the {@code webin-gff3-stages} pipeline,
 * which downloads the reference sequences during its VALIDATE stage.
 */
public class Gff3Validator {

  private static final Logger log = LoggerFactory.getLogger(Gff3Validator.class);

  /**
   * Validates every GFF3 file in the provided list.
   *
   * @param gff3Files GFF3 submission files to validate (must not be null or empty)
   * @return {@code true} if all GFF3 files validated without errors, {@code false} otherwise.
   */
  public boolean validate(List<? extends SubmissionFile<?>> gff3Files) {
    if (gff3Files == null || gff3Files.isEmpty()) {
      return true;
    }
    boolean valid = true;
    for (SubmissionFile<?> gff3File : gff3Files) {
      valid &= validateFile(gff3File);
    }
    return valid;
  }

  private boolean validateFile(SubmissionFile<?> gff3File) {
    File gff3 = gff3File.getFile();

    // No FASTA in annotation context — sequences are referenced by PRIMARY_ID and resolved
    // server-side. Structural validation only: disable validators that require sequence data
    // (coordinate bounds checking, CDS location bounds, translation comparison) since no
    // SequenceLookup is available. Full cross-validation happens server-side in
    // webin-gff3-stages.
    CompositeSequenceProvider sequenceProvider = new CompositeSequenceProvider();

    Map<String, RuleSeverity> ruleOverrides = new HashMap<>();
    ruleOverrides.put("TRANSLATION_COMPARISON", RuleSeverity.OFF);

    List<ValidationException> errors = new ArrayList<>();
    List<ValidationException> warnings = new ArrayList<>();

    try (Reader reader = FileUtils.getBufferedReader(gff3);
        ValidationEngine engine =
            new ValidationEngineBuilder()
                .failFast(false)
                .withProvider(sequenceProvider)
                .overrideMethodRules(ruleOverrides)
                .build();
        GFF3FileReader gff3Reader = new GFF3FileReader(engine, reader, gff3.toPath())) {

      gff3Reader.readHeader();
      gff3Reader.read(annotation -> {});

      // In the annotation context, no FASTA is provided — sequences are referenced via
      // PRIMARY_ID and resolved server-side. The GFF3FileReader reports GFF3_UNDEFINED_SEQID
      // for every feature whose seqId is not in the (empty) SequenceLookup. These are
      // expected and downgraded to warnings; full sequence validation happens server-side.
      for (ValidationException e : engine.getCollectedErrors()) {
        if ("GFF3_UNDEFINED_SEQID".equals(e.getValidationRule())) {
          warnings.add(e);
        } else {
          errors.add(e);
        }
      }
      warnings.addAll(engine.getParsingWarnings());
    } catch (ValidationException ex) {
      errors.add(ex);
    } catch (IOException ex) {
      errors.add(new ValidationException("Failed to read GFF3 file: " + ex.getMessage()));
    } catch (Exception ex) {
      errors.add(new ValidationException("GFF3 validation failed: " + ex.getMessage()));
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

    if (lines.isEmpty()) {
      return;
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
