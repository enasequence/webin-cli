package uk.ac.ebi.ena.webin.cli.xml;

import org.jdom2.Element;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

import java.nio.file.Path;

public class XmlWriterHelper {

  public static Element createTextElement(String name, String text) {
    Element e = new Element(name);
    e.setText(text);
    return e;
  }

  private static Element createFileElement(
      String fileName, String fileType, String digest, String checksum) {
    Element e = new Element("FILE");
    e.setAttribute("filename", fileName);
    e.setAttribute("filetype", String.valueOf(fileType));
    e.setAttribute("checksum_method", digest);
    e.setAttribute("checksum", checksum);
    return e;
  }

  public static Element createFileElement(
      Path inputDir, Path uploadDir, Path file, String fileType) {
    String path =
        file.startsWith(inputDir) ? inputDir.relativize(file).toString() : file.toFile().getName();

    return createFileElement(
        String.valueOf(uploadDir.resolve(path)).replaceAll("\\\\+", "/"),
        String.valueOf(fileType),
        "MD5",
        FileUtils.calculateDigest("MD5", file.toFile()));
  }
}
