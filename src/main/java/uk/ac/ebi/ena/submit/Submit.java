package uk.ac.ebi.ena.submit;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.ena.webin.cli.WebinCli;

import java.io.StringReader;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

public class Submit {
    private final static String ANALYSIS_XML = "analysis.xml";
    private String userName;
    private String password;
    private ContextE contextE;
    private String manifest;
    private String study;
    private String sample;
    private String assemblyName;

    public Submit(WebinCli.Params params, AssemblyInfoEntry assemblyInfoEntry) throws SubmitException {
        try {
            this.contextE = ContextE.valueOf(params.context);
        } catch (IllegalArgumentException e) {
            throw new SubmitException("Invalid context: " + params.context);
        }
        this.userName = params.userName;
        this.password = params.password;
        this.manifest = params.manifest;
        this.study = assemblyInfoEntry.getStudyId();
        this.sample = assemblyInfoEntry.getSampleId();
        this.assemblyName = assemblyInfoEntry.getName().trim().replaceAll("\\s+", "_");
    }

    public void doSubmission() throws SubmitException {
        try {
            CloseableHttpClient httpClient = HttpClients.createDefault();
//            HttpPost httpPost = new HttpPost("https://www.ebi.ac.uk/ena/submit/drop-box/submit");
            HttpPost httpPost = new HttpPost("https://www-test.ebi.ac.uk/ena/submit/drop-box/submit/");
            String encoding = Base64.getEncoder().encodeToString((userName + ":" + password).getBytes());
            httpPost.setHeader("Authorization", "Basic " + encoding);
            MultipartEntityBuilder builder = MultipartEntityBuilder.create();
            File analysisXmlF = createAnalysisXml().toFile();
            builder.addBinaryBody("ANALYSIS", new FileInputStream(analysisXmlF), ContentType.APPLICATION_OCTET_STREAM, analysisXmlF.getName());
            builder.addTextBody("ACTION", "ADD");
            HttpEntity multipart = builder.build();
            httpPost.setEntity(multipart);
            CloseableHttpResponse response = httpClient.execute(httpPost);
            int responsecode = response.getStatusLine().getStatusCode();
            List<String> resultsList = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8)).lines().collect(Collectors.toList());
            switch (responsecode) {
                case HttpStatus.SC_OK:
                    extractReceipt(resultsList);
                    break;
                case HttpStatus.SC_UNAUTHORIZED:
                    throw (new SubmitException("Unable to logon to submission service with supplied credentials."));
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw (new SubmitException("An internal error occurred during the subission process."));
                case HttpStatus.SC_BAD_REQUEST:
                    throw (new SubmitException("Submission request rejected due to it not being correct."));
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw (new SubmitException("Submission service is currently unavailable, please try again later."));
                default:
                    throw (new SubmitException("Unknown error occurred during submissione, please try again later."));
            }
        } catch (Exception e) {
            throw (new SubmitException(("Error occurred while attempting to submit file: " + e.toString())));
        }
    }

    private void extractReceipt(List<String> resultsList) throws SubmitException {
        StringBuilder errorsSb = new StringBuilder();
        try {
            String receipt = resultsList.stream()
                  .collect(Collectors.joining());
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(receipt));
            Element rootNode = doc.getRootElement();
            if (Boolean.valueOf(rootNode.getAttributeValue("success"))) {
                String accession = rootNode.getChild("ANALYSIS").getAttributeValue("accession");
                if (accession != null && !accession.isEmpty())
                    System.out.println("Submission successful - analysis id: " + accession);
                else
                    errorsSb.append("Submission was successful, but Analysis accession returned.");
            } else {
                List<Element> childrenList = rootNode.getChildren("MESSAGES");
                for (Element child : childrenList) {
                    List<Element> errorList = child.getChildren("ERROR");
                    if (errorList != null && !errorList.isEmpty())
                        errorList.stream().forEach(e -> errorsSb.append(e.getValue()));
                    else
                        errorsSb.append("Submission failed, no error reported");
                }
            }
            if (errorsSb.length() != 0)
                throw new SubmitException(errorsSb.toString());
        } catch (Exception e) {
            throw new SubmitException(e.getMessage());
        }
    }

    private Path createAnalysisXml() throws SubmitException {
        try {
            Element analysisE = new Element("ANALYSIS");
            Document doc = new Document(analysisE);
            analysisE.setAttribute("alias", "ena-ANALYSIS-" + System.currentTimeMillis());
            analysisE.addContent(new Element("TITLE").setText(contextE.getAnalysisTitle(assemblyName)));
            Element studyRefE = new Element("STUDY_REF");
            analysisE.addContent(studyRefE);
            studyRefE.setAttribute("accession", study);
            Element sampleRefE = new Element("SAMPLE_REF");
            analysisE.addContent(sampleRefE);
            sampleRefE.setAttribute("accession", sample);
            Element analysisTypeE = new Element("ANALYSIS_TYPE");
            analysisE.addContent(analysisTypeE);
            analysisTypeE.addContent(new Element(contextE.getAnalysisType()));
            Element filesE = new Element("FILES");
            analysisE.addContent(filesE);
            Element fileE = new Element("FILE");
            filesE.addContent(fileE);
            fileE.setAttribute("filename", contextE + File.separator + assemblyName + File.separator + assemblyName + ".manifest");
            fileE.setAttribute("filetype", "manifest");
            fileE.setAttribute("checksum_method", "MD5");
            fileE.setAttribute("checksum", "");
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output(doc, stringWriter);
            Path path = Paths.get(Paths.get(manifest).getParent() + File.separator + ANALYSIS_XML);
            if (Files.exists(path))
                Files.delete(path);
            Files.createFile(path);
            Files.write(path, stringWriter.toString().getBytes());
            return path;
        } catch (Exception e) {
            throw new SubmitException(e.getMessage());
        }
    }
}
