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
package uk.ac.ebi.ena.webin.cli.context;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Analysis;
import uk.ac.ebi.ena.webin.cli.validator.reference.Run;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public abstract class SequenceToolsXmlWriter<M extends Manifest, R extends ValidationResponse> implements XmlWriter<M, R> {

  protected abstract Element createXmlAnalysisTypeElement(M manifest);

  protected abstract List<Element> createXmlFileElements(M manifest, Path inputDir, Path uploadDir);

  @Override
  public Map<SubmissionBundle.SubmissionXMLFileType, String> createXml(
      M manifest,
      R response,
      String centerName,
      String submissionTitle,
      String submissionAlias,
      Path inputDir,
      Path uploadDir) {

    Element analysisSetE = new Element("ANALYSIS_SET");
    Element analysisE = new Element("ANALYSIS");
    analysisSetE.addContent(analysisE);

    Document doc = new Document(analysisSetE);
    analysisE.setAttribute("alias", submissionAlias);

    if (null != centerName && !centerName.isEmpty())
      analysisE.setAttribute("center_name", centerName);

    analysisE.addContent(new Element("TITLE").setText(submissionTitle));

    if (null != manifest.getDescription() && !manifest.getDescription().isEmpty())
      analysisE.addContent(new Element("DESCRIPTION").setText(manifest.getDescription()));

    Element studyRefE = new Element("STUDY_REF");
    analysisE.addContent(studyRefE);
    studyRefE.setAttribute("accession", manifest.getStudy().getBioProjectId());
    if (manifest.getSample() != null
        && manifest.getSample().getBioSampleId() != null
        && !manifest.getSample().getBioSampleId().isEmpty()) {
      Element sampleRefE = new Element("SAMPLE_REF");
      analysisE.addContent(sampleRefE);
      sampleRefE.setAttribute("accession", manifest.getSample().getBioSampleId());
    }

    if (null != manifest.getRun()) {
      List<Run> run = manifest.getRun();
      for (Run r : run) {
        Element runRefE = new Element("RUN_REF");
        analysisE.addContent(runRefE);
        runRefE.setAttribute("accession", r.getRunId());
      }
    }

    if (null != manifest.getAnalysis()) {
      List<Analysis> analysis = manifest.getAnalysis();
      for (Analysis a : analysis) {
        Element analysisRefE = new Element("ANALYSIS_REF");
        analysisE.addContent(analysisRefE);
        analysisRefE.setAttribute("accession", a.getAnalysisId());
      }
    }

    Element analysisTypeE = new Element("ANALYSIS_TYPE");
    analysisE.addContent(analysisTypeE);
    Element typeE = createXmlAnalysisTypeElement(manifest);
    analysisTypeE.addContent(typeE);

    Element filesE = new Element("FILES");
    analysisE.addContent(filesE);

    for (Element e : createXmlFileElements(manifest, inputDir, uploadDir)) filesE.addContent(e);

    Element analysisAttributesE = new Element("ANALYSIS_ATTRIBUTES");

    if (manifest.getSubmissionTool() != null && !manifest.getSubmissionTool().isEmpty()) {
      Element submissionToolAnalysisAttributeTagE = new Element("TAG").setText("SUBMISSION_TOOL");
      Element submissionToolAnalysisAttributeValueE = new Element("VALUE").setText(manifest.getSubmissionTool());

      Element submissionToolAnalysisAttributeE = new Element("ANALYSIS_ATTRIBUTE");
      submissionToolAnalysisAttributeE.addContent(submissionToolAnalysisAttributeTagE);
      submissionToolAnalysisAttributeE.addContent(submissionToolAnalysisAttributeValueE);

      analysisAttributesE.addContent(submissionToolAnalysisAttributeE);
    }

    if (manifest.getSubmissionToolVersion() != null && !manifest.getSubmissionToolVersion().isEmpty()) {
      Element submissionToolVersionAnalysisAttributeTagE = new Element("TAG").setText("SUBMISSION_TOOL_VERSION");
      Element submissionToolVersionAnalysisAttributeValueE = new Element("VALUE").setText(manifest.getSubmissionToolVersion());

      Element submissionToolVersionAnalysisAttributeE = new Element("ANALYSIS_ATTRIBUTE");
      submissionToolVersionAnalysisAttributeE.addContent(submissionToolVersionAnalysisAttributeTagE);
      submissionToolVersionAnalysisAttributeE.addContent(submissionToolVersionAnalysisAttributeValueE);

      analysisAttributesE.addContent(submissionToolVersionAnalysisAttributeE);
    }

    if (analysisAttributesE.getContentSize() > 0) {
      analysisE.addContent(analysisAttributesE);
    }

    XMLOutputter xmlOutput = new XMLOutputter();
    xmlOutput.setFormat(Format.getPrettyFormat());
    StringWriter stringWriter = new StringWriter();

    try {
      xmlOutput.output(doc, stringWriter);
    } catch (IOException ex) {
      throw WebinCliException.systemError(ex);
    }

    Map<SubmissionBundle.SubmissionXMLFileType, String> xmls = new HashMap<>();
    xmls.put(SubmissionBundle.SubmissionXMLFileType.ANALYSIS, stringWriter.toString());
    return xmls;
  }
}
