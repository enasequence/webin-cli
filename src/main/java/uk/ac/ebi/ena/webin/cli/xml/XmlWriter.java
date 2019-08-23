package uk.ac.ebi.ena.webin.cli.xml;

import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.validator.manifest.Manifest;

import java.nio.file.Path;
import java.util.Map;

public interface XmlWriter<M extends Manifest> {
  Map<SubmissionBundle.SubmissionXMLFileType, String> createXml(
      M manifest,
      String centerName,
      String submissionTitle,
      String submissionAlias,
      Path inputDir,
      Path uploadDir);
}
