/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.readtools.validator.ReadsValidator;
import uk.ac.ebi.ena.txmbvalidator.TxmbValidator;
import uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader;
import uk.ac.ebi.ena.webin.cli.context.genome.GenomeXmlWriter;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsXmlWriter;
import uk.ac.ebi.ena.webin.cli.context.sequence.SequenceManifestReader;
import uk.ac.ebi.ena.webin.cli.context.sequence.SequenceXmlWriter;
import uk.ac.ebi.ena.webin.cli.context.taxrefset.PlaceHolderForTaxRefSetValidator;
import uk.ac.ebi.ena.webin.cli.context.taxrefset.TaxRefSetManifestReader;
import uk.ac.ebi.ena.webin.cli.context.taxrefset.TaxRefSetXmlWriter;
import uk.ac.ebi.ena.webin.cli.context.transcriptome.TranscriptomeManifestReader;
import uk.ac.ebi.ena.webin.cli.context.transcriptome.TranscriptomeXmlWriter;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderBuilder;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.api.Validator;
import uk.ac.ebi.ena.webin.cli.validator.manifest.*;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public enum WebinCliContext {
  genome(
      GenomeManifest.class,
      GenomeManifestReader.class,
      GenomeXmlWriter.class,
      SubmissionValidator.class,
      "Genome assembly"),
  transcriptome(
      TranscriptomeManifest.class,
      TranscriptomeManifestReader.class,
      TranscriptomeXmlWriter.class,
      SubmissionValidator.class,
      "Transcriptome assembly"),
  sequence(
      SequenceManifest.class,
      SequenceManifestReader.class,
      SequenceXmlWriter.class,
      SubmissionValidator.class,
      "Sequence assembly"),
  reads(
      ReadsManifest.class,
      ReadsManifestReader.class,
      ReadsXmlWriter.class,
      ReadsValidator.class,
      "Raw reads"),
  taxrefset(
          TaxRefSetManifest.class,
          TaxRefSetManifestReader.class,
          TaxRefSetXmlWriter.class,
          TxmbValidator.class,
          "Taxonomy reference set");

  private final Class<? extends Manifest> manifestClass;
  private final Class<? extends ManifestReader<? extends Manifest>> manifestReaderClass;
  private final Class<? extends XmlWriter<? extends Manifest, ? extends ValidationResponse>> xmlWriterClass;
  private final Class<? extends Validator<? extends Manifest, ? extends ValidationResponse>> validatorClass;

  private final String titlePrefix;

  WebinCliContext(
      Class<? extends Manifest> manifestClass,
      Class<? extends ManifestReader<? extends Manifest>> manifestReaderClass,
      Class<? extends XmlWriter<? extends Manifest, ? extends ValidationResponse>> xmlWriterClass,
      Class<? extends Validator<? extends Manifest, ? extends ValidationResponse>> validatorClass,
      String titlePrefix) {
    this.manifestClass = manifestClass;
    this.manifestReaderClass = manifestReaderClass;
    this.xmlWriterClass = xmlWriterClass;
    this.validatorClass = validatorClass;
    this.titlePrefix = titlePrefix;
  }

  public Class<? extends Manifest> getManifestClass() {
    return manifestClass;
  }

  public Class<? extends ManifestReader> getManifestReaderClass() {
    return manifestReaderClass;
  }

  public static <M extends Manifest, R extends ValidationResponse> WebinCliExecutor<M, R> createExecutor(
      Class<M> manifestClass, WebinCliParameters parameters) {
    for (WebinCliContext context : WebinCliContext.values()) {
      if (context.getManifestClass().equals(manifestClass)) {
        return (WebinCliExecutor<M, R>) context.createExecutor(parameters);
      }
    }
    return null;
  }

  public WebinCliExecutor<?, ?> createExecutor(WebinCliParameters parameters) {
    return createExecutor(parameters,
        new ManifestReaderBuilder(manifestReaderClass, parameters).build());
  }

  public WebinCliExecutor<?, ?> createExecutor(
      WebinCliParameters parameters, ManifestReader<?> manifestReader) {

    try {
      XmlWriter<?, ?> xmlWriter = xmlWriterClass.newInstance();
      Validator<?, ?> validator = validatorClass.newInstance();
      return new WebinCliExecutor(this, parameters, manifestReader, xmlWriter, validator);

    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getTitlePrefix() {
    return titlePrefix;
  }
}
