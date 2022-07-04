/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.reads;

import static uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest.FileType;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;

import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.manifest.ReadsManifest;
import uk.ac.ebi.ena.webin.cli.validator.response.ReadsValidationResponse;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;

public class ReadsXmlWriter implements XmlWriter<ReadsManifest, ReadsValidationResponse> {

  @Override
  public Map<SubmissionBundle.SubmissionXMLFileType, String> createXml(
      ReadsManifest manifest,
      ReadsValidationResponse response,
      String centerName,
      String submissionTitle,
      String submissionAlias,
      Path inputDir,
      Path uploadDir) {

    Map<SubmissionBundle.SubmissionXMLFileType, String> xmls = new LinkedHashMap<>();
    xmls.put(
        SubmissionBundle.SubmissionXMLFileType.EXPERIMENT,
        createExperimentXml(manifest, response, submissionTitle, submissionAlias, centerName));
    xmls.put(
        SubmissionBundle.SubmissionXMLFileType.RUN,
        createRunXml(manifest, submissionTitle, submissionAlias, centerName, inputDir, uploadDir));

    return xmls;
  }

  private String createExperimentXml(
      ReadsManifest manifest,
      ReadsValidationResponse response,
      String submissionTitle,
      String submissionAlias,
      String centerName) {

    String instrument = manifest.getInstrument();
    String description =
        StringUtils.isBlank(manifest.getDescription()) ? "unspecified" : manifest.getDescription();
    String libraryStrategy = manifest.getLibraryStrategy();
    String librarySource = manifest.getLibrarySource();
    String librarySelection = manifest.getLibrarySelection();
    String libraryName = manifest.getLibraryName();
    String platform = manifest.getPlatform();
    Integer insertSize = manifest.getInsertSize();

    try {
      String title = submissionTitle;
      Element experimentSetE = new Element("EXPERIMENT_SET");
      Element experimentE = new Element("EXPERIMENT");
      experimentSetE.addContent(experimentE);

      experimentE.setAttribute("alias", submissionAlias);

      if (null != centerName && !centerName.isEmpty()) {
        experimentE.setAttribute("center_name", centerName);
      }

      experimentE.addContent(new Element("TITLE").setText(title));

      Element studyRefE = new Element("STUDY_REF");
      experimentE.addContent(studyRefE);

      if (manifest.getStudy() != null) {
        studyRefE.setAttribute("accession", manifest.getStudy().getBioProjectId());
      }

      Element designE = new Element("DESIGN");
      experimentE.addContent(designE);

      Element designDescriptionE = new Element("DESIGN_DESCRIPTION");
      designDescriptionE.setText(description);
      designE.addContent(designDescriptionE);

      Element sampleDescriptorE = new Element("SAMPLE_DESCRIPTOR");

      if (manifest.getStudy() != null) {
        sampleDescriptorE.setAttribute("accession", manifest.getSample().getBioSampleId());
      }

      designE.addContent(sampleDescriptorE);

      Element libraryDescriptorE = new Element("LIBRARY_DESCRIPTOR");
      designE.addContent(libraryDescriptorE);

      if (null != libraryName) {
        Element libraryNameE = new Element("LIBRARY_NAME");
        libraryNameE.setText(libraryName);
        libraryDescriptorE.addContent(libraryNameE);
      }

      Element libraryStrategyE = new Element("LIBRARY_STRATEGY");
      libraryStrategyE.setText(libraryStrategy);
      libraryDescriptorE.addContent(libraryStrategyE);

      Element librarySourceE = new Element("LIBRARY_SOURCE");
      librarySourceE.setText(librarySource);
      libraryDescriptorE.addContent(librarySourceE);

      Element librarySelectionE = new Element("LIBRARY_SELECTION");
      librarySelectionE.setText(librarySelection);
      libraryDescriptorE.addContent(librarySelectionE);

      Element libraryLayoutE = new Element("LIBRARY_LAYOUT");

      if (!response.isPaired()) {
        libraryLayoutE.addContent(new Element("SINGLE"));
      } else {
        Element pairedE = new Element("PAIRED");
        libraryLayoutE.addContent(pairedE);

        if (null != insertSize) {
          pairedE.setAttribute("NOMINAL_LENGTH", String.valueOf(insertSize));
        }
      }

      libraryDescriptorE.addContent(libraryLayoutE);

      Element platformE = new Element("PLATFORM");
      experimentE.addContent(platformE);

      Element platformRefE = new Element(platform);
      platformE.addContent(platformRefE);
      Element instrumentModelE = new Element("INSTRUMENT_MODEL");
      instrumentModelE.setText(instrument);
      platformRefE.addContent(instrumentModelE);

      Element expAttributesE = new Element("EXPERIMENT_ATTRIBUTES");

      if (manifest.getSubmissionTool() != null && !manifest.getSubmissionTool().isEmpty()) {
        Element submissionToolExpAttributeTagE = new Element("TAG").setText("SUBMISSION_TOOL");
        Element submissionToolExpAttributeValueE = new Element("VALUE").setText(manifest.getSubmissionTool());

        Element submissionToolExpAttributeE = new Element("EXPERIMENT_ATTRIBUTE");
        submissionToolExpAttributeE.addContent(submissionToolExpAttributeTagE);
        submissionToolExpAttributeE.addContent(submissionToolExpAttributeValueE);

        expAttributesE.addContent(submissionToolExpAttributeE);
      }

      if (manifest.getSubmissionToolVersion() != null && !manifest.getSubmissionToolVersion().isEmpty()) {
        Element submissionToolVersionExpAttributeTagE = new Element("TAG").setText("SUBMISSION_TOOL_VERSION");
        Element submissionToolVersionExpAttributeValueE = new Element("VALUE").setText(manifest.getSubmissionToolVersion());

        Element submissionToolVersionExpAttributeE = new Element("EXPERIMENT_ATTRIBUTE");
        submissionToolVersionExpAttributeE.addContent(submissionToolVersionExpAttributeTagE);
        submissionToolVersionExpAttributeE.addContent(submissionToolVersionExpAttributeValueE);

        expAttributesE.addContent(submissionToolVersionExpAttributeE);
      }

      if (expAttributesE.getContentSize() > 0) {
        experimentE.addContent(expAttributesE);
      }

      XMLOutputter xmlOutput = new XMLOutputter();
      xmlOutput.setFormat(Format.getPrettyFormat());
      StringWriter stringWriter = new StringWriter();
      xmlOutput.output(experimentSetE, stringWriter);
      return stringWriter.toString();

    } catch (IOException ex) {
      throw WebinCliException.systemError(ex);
    }
  }

  String createRunXml(
      ReadsManifest manifest,
      String submissionTitle,
      String submissionAlias,
      String centerName,
      Path inputDir,
      Path uploadDir) {
    try {
      String title = submissionTitle;
      Element runSetE = new Element("RUN_SET");
      Element runE = new Element("RUN");
      runSetE.addContent(runE);

      runE.setAttribute("alias", submissionAlias);

      if (null != centerName && !centerName.isEmpty()) {
        runE.setAttribute("center_name", centerName);
      }

      runE.addContent(new Element("TITLE").setText(title));
      Element experimentRefE = new Element("EXPERIMENT_REF");
      runE.addContent(experimentRefE);
      experimentRefE.setAttribute("refname", submissionAlias);

      Element dataBlockE = new Element("DATA_BLOCK");
      runE.addContent(dataBlockE);
      Element filesE = new Element("FILES");
      dataBlockE.addContent(filesE);

      manifest.files(FileType.BAM).stream()
          .map(file -> file.getFile().toPath())
          .forEach(file -> filesE.addContent(createFileElement(inputDir, uploadDir, file, "bam", null)));

      manifest.files(FileType.CRAM).stream()
          .map(file -> file.getFile().toPath())
          .forEach(file -> filesE.addContent(createFileElement(inputDir, uploadDir, file, "cram", null)));

      manifest.files(FileType.FASTQ).stream()
          .forEach(
              file -> filesE.addContent(createFileElement(
                      inputDir, uploadDir, file.getFile().toPath(), "fastq", file.getAttributes())));

      Element runAttributesE = new Element("RUN_ATTRIBUTES");

      if (manifest.getSubmissionTool() != null && !manifest.getSubmissionTool().isEmpty()) {
        Element submissionToolRunAttributeTagE = new Element("TAG").setText("SUBMISSION_TOOL");
        Element submissionToolRunAttributeValueE = new Element("VALUE").setText(manifest.getSubmissionTool());

        Element submissionToolRunAttributeE = new Element("RUN_ATTRIBUTE");
        submissionToolRunAttributeE.addContent(submissionToolRunAttributeTagE);
        submissionToolRunAttributeE.addContent(submissionToolRunAttributeValueE);

        runAttributesE.addContent(submissionToolRunAttributeE);
      }

      if (manifest.getSubmissionToolVersion() != null && !manifest.getSubmissionToolVersion().isEmpty()) {
        Element submissionToolVersionRunAttributeTagE = new Element("TAG").setText("SUBMISSION_TOOL_VERSION");
        Element submissionToolVersionRunAttributeValueE = new Element("VALUE").setText(manifest.getSubmissionToolVersion());

        Element submissionToolVersionRunAttributeE = new Element("RUN_ATTRIBUTE");
        submissionToolVersionRunAttributeE.addContent(submissionToolVersionRunAttributeTagE);
        submissionToolVersionRunAttributeE.addContent(submissionToolVersionRunAttributeValueE);

        runAttributesE.addContent(submissionToolVersionRunAttributeE);
      }

      if (runAttributesE.getContentSize() > 0) {
        runE.addContent(runAttributesE);
      }

      XMLOutputter xmlOutput = new XMLOutputter();
      xmlOutput.setFormat(Format.getPrettyFormat());
      StringWriter stringWriter = new StringWriter();
      xmlOutput.output(runSetE, stringWriter);
      return stringWriter.toString();

    } catch (IOException ex) {
      throw WebinCliException.systemError(ex);
    }
  }
}
