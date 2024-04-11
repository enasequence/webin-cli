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
package uk.ac.ebi.ena.webin.cli.xml;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.validator.file.SubmissionFileAttribute;

public class XmlWriterHelper {

  public static Element createTextElement(String name, String text) {
    Element e = new Element(name);
    e.setText(text);
    return e;
  }

  private static Element createFileElement(
      String fileName,
      String fileType,
      String digest,
      String checksum,
      List<SubmissionFileAttribute> attributes) {
    if (checksum == null) {
      throw new IllegalArgumentException("File checksum cannot be null.");
    }

    Element e = new Element("FILE");
    e.setAttribute("filename", fileName);
    e.setAttribute("filetype", String.valueOf(fileType));
    e.setAttribute("checksum_method", digest);
    e.setAttribute("checksum", checksum);

    if (attributes != null && !attributes.isEmpty()) {
      attributes.stream()
          .map(att -> createAttributeElement(att.getName(), att.getValue()))
          .filter(Objects::nonNull)
          .forEach(attElement -> e.addContent(attElement));
    }

    return e;
  }

  public static Element createFileElement(
      Path inputDir, Path uploadDir, Path file, String fileMd5, String fileType) {
    return createFileElement(inputDir, uploadDir, file, fileMd5, fileType, null);
  }

  public static Element createFileElement(
      Path inputDir,
      Path uploadDir,
      Path file,
      String fileMd5,
      String fileType,
      List<SubmissionFileAttribute> attributes) {
    String fileName = file.toFile().getName();

    return createFileElement(
        FileUtils.replaceIncompatibleFileSeparators(String.valueOf(uploadDir.resolve(fileName))),
        String.valueOf(fileType),
        "MD5",
        fileMd5,
        attributes);
  }

  private static Element createAttributeElement(String attName, String attValue) {
    switch (attName) {
      case "READ_TYPE":
        {
          Element e = new Element(attName);
          e.addContent(attValue);

          return e;
        }

      default:
        return null;
    }
  }
}
