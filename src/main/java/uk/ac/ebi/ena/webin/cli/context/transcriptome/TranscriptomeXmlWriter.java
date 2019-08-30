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
package uk.ac.ebi.ena.webin.cli.context.transcriptome;

import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest.FileType;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createTextElement;

public class TranscriptomeXmlWriter extends SequenceToolsXmlWriter<TranscriptomeManifest, ValidationResponse> {

  @Override
  protected Element createXmlAnalysisTypeElement(TranscriptomeManifest manifest) {
    Element e = new Element("TRANSCRIPTOME_ASSEMBLY");

    e.addContent(createTextElement("NAME", manifest.getName()));
    e.addContent(createTextElement("PROGRAM", manifest.getProgram()));
    e.addContent(createTextElement("PLATFORM", manifest.getPlatform()));

    if (manifest.isTpa()) e.addContent(createTextElement("TPA", String.valueOf(manifest.isTpa())));

    if (null != manifest.getAuthors() && null != manifest.getAddress()) {
      e.addContent(createTextElement("AUTHORS", manifest.getAuthors()));
      e.addContent(createTextElement("ADDRESS", manifest.getAddress()));
    }
    return e;
  }

  @Override
  protected List<Element> createXmlFileElements(
      TranscriptomeManifest manifest, Path inputDir, Path uploadDir) {

    List<Element> list = new ArrayList<>();

    manifest.files(FileType.FASTA).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "fasta")));

    manifest.files(FileType.FLATFILE).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "flatfile")));

    return list;
  }
}
