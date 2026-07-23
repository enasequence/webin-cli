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

import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.AnnotationManifest;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

public class AnnotationXmlWriter
    extends SequenceToolsXmlWriter<AnnotationManifest, ValidationResponse> {

  @Override
  protected Element createXmlAnalysisTypeElement(AnnotationManifest manifest) {
    return new Element(manifest.getAnalysisType());
  }

  @Override
  protected List<Element> createXmlFileElements(
      AnnotationManifest manifest, Path inputDir, Path uploadDir) {
    List<Element> list = new ArrayList<>();

    manifest.files(AnnotationManifest.FileType.GFF3).stream()
        .map(file -> file.getFile().toPath())
        .forEach(
            file ->
                list.add(
                    createFileElement(
                        inputDir,
                        uploadDir,
                        file,
                        FileUtils.calculateDigest("MD5", file.toFile()),
                        "gff3")));

    return list;
  }

  @Override
  protected <M extends Manifest> List<Element> createAdditionalAnalysisElements(M manifest) {
    AnnotationManifest annotationManifest = (AnnotationManifest) manifest;
    return List.of(new Element("PRIMARY_ID").setText(annotationManifest.getPrimaryId()));
  }

  @Override
  protected <M extends Manifest> void addCustomAttributes(M manifest, Element analysisAttributesE) {
    AnnotationManifest annotationManifest = (AnnotationManifest) manifest;

    for (Map.Entry<String, String> attribute : annotationManifest.getAttributes().entrySet()) {
      Element tagE = new Element("TAG").setText(attribute.getKey());
      Element valueE = new Element("VALUE").setText(attribute.getValue());

      Element attributeE = new Element("ANALYSIS_ATTRIBUTE");
      attributeE.addContent(tagE);
      attributeE.addContent(valueE);

      analysisAttributesE.addContent(attributeE);
    }
  }
}
