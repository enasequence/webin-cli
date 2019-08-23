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
package uk.ac.ebi.ena.webin.cli.context.genome;

import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest;

import static uk.ac.ebi.ena.webin.cli.validator.manifest.GenomeManifest.FileType;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createTextElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class GenomeXmlWriter extends SequenceToolsXmlWriter<GenomeManifest> {

  @Override
  protected Element createXmlAnalysisTypeElement(GenomeManifest manifest) {

    Element e = new Element("SEQUENCE_ASSEMBLY");

    e.addContent(createTextElement("NAME", manifest.getName()));
    if (null != manifest.getAssemblyType() && !manifest.getAssemblyType().isEmpty())
      e.addContent(createTextElement("TYPE", manifest.getAssemblyType()));
    e.addContent(
        createTextElement(
            "PARTIAL", String.valueOf(Boolean.FALSE))); // as per SraAnalysisParser.setAssemblyInfo
    e.addContent(createTextElement("COVERAGE", manifest.getCoverage()));
    e.addContent(createTextElement("PROGRAM", manifest.getProgram()));
    e.addContent(createTextElement("PLATFORM", manifest.getPlatform()));

    if (null != manifest.getMinGapLength())
      e.addContent(createTextElement("MIN_GAP_LENGTH", String.valueOf(manifest.getMinGapLength())));

    if (null != manifest.getMoleculeType() && !manifest.getMoleculeType().isEmpty())
      e.addContent(createTextElement("MOL_TYPE", manifest.getMoleculeType()));

    if (manifest.isTpa()) e.addContent(createTextElement("TPA", String.valueOf(manifest.isTpa())));
    if (null != manifest.getAuthors() && null != manifest.getAddress()) {
      e.addContent(createTextElement("AUTHORS", manifest.getAuthors()));
      e.addContent(createTextElement("ADDRESS", manifest.getAddress()));
    }

    return e;
  }

  @Override
  protected List<Element> createXmlFileElements(
      GenomeManifest manifest, Path inputDir, Path uploadDir) {

    List<Element> list = new ArrayList<>();

    manifest.files(FileType.CHROMOSOME_LIST).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "chromosome_list")));

    manifest.files(FileType.UNLOCALISED_LIST).stream()
        .map(file -> file.getFile().toPath())
        .forEach(
            file -> list.add(createFileElement(inputDir, uploadDir, file, "unlocalised_list")));
    manifest.files(FileType.FASTA).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "fasta")));
    manifest.files(FileType.FLATFILE).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "flatfile")));
    manifest.files(FileType.AGP).stream()
        .map(file -> file.getFile().toPath())
        .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "agp")));

    return list;
  }
}
