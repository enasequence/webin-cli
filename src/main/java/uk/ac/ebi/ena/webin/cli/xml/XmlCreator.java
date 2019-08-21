package uk.ac.ebi.ena.webin.cli.xml;

import uk.ac.ebi.ena.webin.cli.submit.SubmissionBundle;

import java.nio.file.Path;
import java.util.Map;

// TODO M extends Manifest
public interface XmlCreator<M> {
  Map<SubmissionBundle.SubmissionXMLFileType, String> createXml(
      M manifest,
      String centerName,
      String submissionTitle,
      String submissionAlias,
      Path inputDir,
      Path uploadDir);
}
