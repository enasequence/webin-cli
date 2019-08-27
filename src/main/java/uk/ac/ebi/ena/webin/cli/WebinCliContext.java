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

import uk.ac.ebi.ena.webin.cli.context.genome.GenomeManifestReader;
import uk.ac.ebi.ena.webin.cli.context.genome.GenomeXmlWriter;
import uk.ac.ebi.ena.webin.cli.context.sequence.SequenceManifestReader;
import uk.ac.ebi.ena.webin.cli.context.sequence.SequenceXmlWriter;
import uk.ac.ebi.ena.webin.cli.context.transcriptome.TranscriptomeManifestReader;
import uk.ac.ebi.ena.webin.cli.context.transcriptome.TranscriptomeXmlWriter;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderBuilder;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsWebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.manifest.*;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public enum WebinCliContext {
  genome(
          GenomeManifest.class,
          GenomeManifestReader.class,
          GenomeXmlWriter.class,
          "Genome assembly"),
  transcriptome(
          TranscriptomeManifest.class,
          TranscriptomeManifestReader.class,
          TranscriptomeXmlWriter.class,
          "Transcriptome assembly"),
  sequence(
          SequenceManifest.class,
          SequenceManifestReader.class,
          SequenceXmlWriter.class,
          "Sequence assembly"),
  reads(ReadsManifest.class, ReadsManifestReader.class, ReadsXmlWriter.class, "Raw reads");

  private final Class<? extends Manifest> manifestClass;
  private final Class<? extends ManifestReader<? extends Manifest>> manifestReaderClass;
  private final Class<? extends XmlWriter<? extends Manifest>> xmlWriterClass;

  private final String titlePrefix;

  WebinCliContext(
          Class<? extends Manifest> manifestClass,
          Class<? extends ManifestReader<? extends Manifest>> manifestReaderClass,
          Class<? extends XmlWriter<? extends Manifest>> xmlWriterClass,
          String titlePrefix) {
    this.manifestClass = manifestClass;
    this.manifestReaderClass = manifestReaderClass;
    this.xmlWriterClass = xmlWriterClass;
    this.titlePrefix = titlePrefix;
  }

  public Class<? extends Manifest> getManifestClass() {
    return manifestClass;
  }

  public static <M extends Manifest> WebinCliExecutor<M> createExecutor(Class<M> manifestClass, WebinCliParameters parameters) {
    for (WebinCliContext context : WebinCliContext.values()) {
      if (context.getManifestClass().equals(manifestClass)) {
        return (WebinCliExecutor<M>) context.createExecutor(parameters);
      }
    }
    return null;
  }

  public WebinCliExecutor<?> createExecutor(WebinCliParameters parameters) {
    return createExecutor(parameters, new ManifestReaderBuilder(manifestReaderClass, parameters).build());
  }

  public WebinCliExecutor<?> createExecutor(
          WebinCliParameters parameters, ManifestReader<?> manifestReader) {
    if (manifestClass.equals(ReadsManifest.class)) {
      // TODO: remove ReadsWebinCliExecutor
      return new ReadsWebinCliExecutor(parameters, (ReadsManifestReader)manifestReader);
    } else {
      try {
        XmlWriter<?> xmlWriter = xmlWriterClass.newInstance();
        return new WebinCliExecutorEx(this, parameters, manifestReader, xmlWriter);

      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public String getTitlePrefix() {
    return titlePrefix;
  }
}
