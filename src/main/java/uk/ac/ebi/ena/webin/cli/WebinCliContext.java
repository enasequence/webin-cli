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

import uk.ac.ebi.ena.webin.cli.assembly.*;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReaderParameters;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifestReader;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsWebinCliExecutor;
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.manifest.*;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public enum WebinCliContext {
  genome(
          GenomeManifest.class,
          GenomeAssemblyManifestReader.class,
          GenomeAssemblyXmlWriter.class,
          "Genome assembly"),
  transcriptome(
          TranscriptomeManifest.class,
          TranscriptomeAssemblyManifestReader.class,
          TranscriptomeAssemblyXmlWriter.class,
          "Transcriptome assembly"),
  sequence(
          SequenceManifest.class,
          SequenceAssemblyManifestReader.class,
          SequenceAssemblyXmlWriter.class,
          "Sequence assembly"),
  reads(ReadsManifest.class, RawReadsManifestReader.class, RawReadsXmlWriter.class, "Raw reads");

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

  public WebinCliExecutor<?> createExecutor(WebinCliParameters parameters) {
    return createExecutor(parameters, createManifestReader(parameters));
  }

  public WebinCliExecutor<?> createExecutor(
          WebinCliParameters parameters, ManifestReader<?> manifestReader) {
    if (manifestClass.equals(ReadsManifest.class)) {
      // TODO: remove RawReadsWebinCliExecutor
      return new RawReadsWebinCliExecutor(parameters);
    } else {
      try {
        return new WebinCliExecutorEx<>(this, parameters, manifestReader, createXmlWriter());

      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }

  public ManifestReader createManifestReader(WebinCliParameters parameters) {
    return createManifestReader(
            manifestReaderClass,
            ManifestReader.DEFAULT_PARAMETERS,
            new MetadataProcessorFactory(parameters));
  }

  public static ManifestReader createManifestReader(
          Class<? extends ManifestReader<? extends Manifest>> manifestReaderClass,
          ManifestReaderParameters manifestReaderParameters,
          MetadataProcessorFactory metadataProcessorFactory) {
    try {
      return manifestReaderClass
              .getDeclaredConstructor(ManifestReaderParameters.class, MetadataProcessorFactory.class)
              .newInstance(manifestReaderParameters, metadataProcessorFactory);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public XmlWriter createXmlWriter() {
    try {
      return xmlWriterClass.newInstance();
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public String getTitlePrefix() {
    return titlePrefix;
  }
}
