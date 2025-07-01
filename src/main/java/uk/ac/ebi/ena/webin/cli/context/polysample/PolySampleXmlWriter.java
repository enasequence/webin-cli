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
package uk.ac.ebi.ena.webin.cli.context.polysample;

import static uk.ac.ebi.ena.webin.cli.validator.manifest.PolySampleManifest.FileType;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createTextElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.PolySampleManifest;

public class PolySampleXmlWriter
    extends SequenceToolsXmlWriter<PolySampleManifest, ValidationResponse> {

  @Override
  protected Element createXmlAnalysisTypeElement(PolySampleManifest manifest) {
    String analysisType = "ENVIRONMENTAL_SEQUENCE_SET";

    Element element = new Element(analysisType);

    if (null != manifest.getAuthors() && null != manifest.getAddress()) {
      element.addContent(createTextElement("AUTHORS", manifest.getAuthors()));
      element.addContent(createTextElement("ADDRESS", manifest.getAddress()));
      return element;
    }
    return element;
  }

  @Override
  protected List<Element> createXmlFileElements(
      PolySampleManifest manifest, Path inputDir, Path uploadDir) {
    List<Element> list = new ArrayList<>();

    manifest.files(FileType.FASTA).stream()
        .map(file -> file.getFile().toPath())
        .forEach(
            file ->
                list.add(
                    createFileElement(
                        inputDir,
                        uploadDir,
                        file,
                        FileUtils.calculateDigest("MD5", file.toFile()),
                        "fasta")));
    manifest.files(FileType.SAMPLE_TSV).stream()
        .map(file -> file.getFile().toPath())
        .forEach(
            file ->
                list.add(
                    createFileElement(
                        inputDir,
                        uploadDir,
                        file,
                        FileUtils.calculateDigest("MD5", file.toFile()),
                        "sample_tsv")));
    manifest.files(FileType.TAX_TSV).stream()
        .map(file -> file.getFile().toPath())
        .forEach(
            file ->
                list.add(
                    createFileElement(
                        inputDir,
                        uploadDir,
                        file,
                        FileUtils.calculateDigest("MD5", file.toFile()),
                        "tax_tsv")));

    return list;
  }
}
