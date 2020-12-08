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
package uk.ac.ebi.ena.webin.cli.context.sequence;

import static uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest.FileType;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createTextElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;

public class SequenceXmlWriter extends SequenceToolsXmlWriter<SequenceManifest, ValidationResponse> {

  @Override
  protected Element createXmlAnalysisTypeElement(SequenceManifest manifest) {

    Element element = new Element("SEQUENCE_FLATFILE");
    if (null != manifest.getAuthors() && null != manifest.getAddress()) {
      element.addContent(createTextElement("AUTHORS", manifest.getAuthors()));
      element.addContent(createTextElement("ADDRESS", manifest.getAddress()));
      return element;
    }
    return element;
  }

  @Override
  protected List<Element> createXmlFileElements(
      SequenceManifest manifest, Path inputDir, Path uploadDir) {

    List<Element> list = new ArrayList<>();

    manifest.files(FileType.FLATFILE).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "flatfile")));

    manifest.files(FileType.TAB).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "tab")));

    return list;
  }
}
