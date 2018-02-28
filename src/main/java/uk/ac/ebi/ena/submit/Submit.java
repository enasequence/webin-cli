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
import uk.ac.ebi.ena.webin.cli.WebinCliException;
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
    private final static String RECEIPT_XML = "receipt.xml";
    private String userName;
    private String password;
    private ContextE contextE;
    private String outputDir;
    private String study;
    private String sample;
    private String assemblyName;
    private final boolean TEST;
    private Path submitDir;
    private String SUBMIT_DIR = "submit";

    private final static String SYSTEM_ERROR_INTERNAL = "An internal server error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_UNAVAILABLE = "A service unavailable error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_BAD_REQUEST = "A bad request error occurred when attempting to submit. ";
    private final static String SYSTEM_ERROR_OTHER = "A server error occurred when when attempting to submit. ";

    public Submit(WebinCli.Params params,String outputDir, AssemblyInfoEntry assemblyInfoEntry) throws SubmitException {
        try {
            this.contextE = ContextE.valueOf(params.context);
        } catch (IllegalArgumentException e) {
            throw new SubmitException(WebinCli.INVALID_CONTEXT + params.context, WebinCliException.ErrorType.USER_ERROR);
        }
        this.TEST = params.test;
        this.userName = params.userName;
        this.password = params.password;
        this.outputDir = outputDir;
        this.study = assemblyInfoEntry.getStudyId();
        this.sample = assemblyInfoEntry.getSampleId();
        this.assemblyName = assemblyInfoEntry.getName().trim().replaceAll("\\s+", "_");
    }

    public void doSubmission() throws SubmitException {
        try {
            createSubmitDir();
            CloseableHttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost((TEST ? "https://wwwdev.ebi.ac.uk/ena/submit/drop-box/submit/" : "https://www.ebi.ac.uk/ena/submit/drop-box/submit/"));
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
                    throw (new SubmitException(WebinCli.INVALID_CREDENTIALS, WebinCliException.ErrorType.USER_ERROR));
                case HttpStatus.SC_INTERNAL_SERVER_ERROR:
                    throw (new SubmitException(SYSTEM_ERROR_INTERNAL, WebinCliException.ErrorType.SYSTEM_ERROR));
                case HttpStatus.SC_BAD_REQUEST:
                    throw (new SubmitException(SYSTEM_ERROR_BAD_REQUEST, WebinCliException.ErrorType.SYSTEM_ERROR));
                case HttpStatus.SC_SERVICE_UNAVAILABLE:
                    throw (new SubmitException(SYSTEM_ERROR_UNAVAILABLE, WebinCliException.ErrorType.SYSTEM_ERROR));
                default:
                    throw (new SubmitException(SYSTEM_ERROR_OTHER, WebinCliException.ErrorType.SYSTEM_ERROR));
            }
        } catch (SubmitException e) {
            throw e;
        } catch (Exception e) {
            throw (new SubmitException(SYSTEM_ERROR_OTHER + e.getMessage(), WebinCliException.ErrorType.SYSTEM_ERROR));
        }
    }

    private void extractReceipt(List<String> resultsList) throws SubmitException {
        StringBuilder errorsSb = new StringBuilder();
        try {
            String receipt = resultsList.stream()
                  .collect(Collectors.joining());
            Path receiptFile = Paths.get(submitDir + File.separator + RECEIPT_XML);
            if (Files.exists(receiptFile))
                Files.delete(receiptFile);
            Files.createFile(receiptFile);
            SAXBuilder builder = new SAXBuilder();
            Document doc = builder.build(new StringReader(receipt));
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output(doc, stringWriter);
            Files.write(receiptFile, stringWriter.toString().getBytes());
            Element rootNode = doc.getRootElement();
            if (Boolean.valueOf(rootNode.getAttributeValue("success"))) {
                String accession = rootNode.getChild("ANALYSIS").getAttributeValue("accession");
                if (accession != null && !accession.isEmpty())
                    System.out.println(WebinCli.SUBMIT_SUCCESS + " The following analysis accession was assigned to the submission: " + accession);
                else
                    System.out.println(WebinCli.SUBMIT_SUCCESS + " No accession was assigned to the analysis XML submission. Please contact the helpdesk.");
            } else {
                List<Element> childrenList = rootNode.getChildren("MESSAGES");
                for (Element child : childrenList) {
                    List<Element> errorList = child.getChildren("ERROR");
                    if (errorList != null && !errorList.isEmpty())
                        errorList.stream().forEach(e -> errorsSb.append(e.getValue()));
                    else
                        errorsSb.append("The submission failed because of an analysis XML submission error.");
                }
            }
            if (errorsSb.length() != 0)
                throw new SubmitException(errorsSb.toString(), WebinCliException.ErrorType.SYSTEM_ERROR);
        } catch (Exception e) {
            throw new SubmitException(e.getMessage(), WebinCliException.ErrorType.SYSTEM_ERROR);
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
            // Using unix path separator in the Analysis XML.
            fileE.setAttribute("filename", contextE + "/" + assemblyName + "/" + assemblyName + ".manifest");
            fileE.setAttribute("filetype", "manifest");
            fileE.setAttribute("checksum_method", "MD5");
            fileE.setAttribute("checksum", "");
            XMLOutputter xmlOutput = new XMLOutputter();
            xmlOutput.setFormat(Format.getPrettyFormat());
            StringWriter stringWriter = new StringWriter();
            xmlOutput.output(doc, stringWriter);
            Path analysisFile = Paths.get(submitDir + File.separator + ANALYSIS_XML);
            if (Files.exists(analysisFile))
                Files.delete(analysisFile);
            Files.createFile(analysisFile);
            Files.write(analysisFile, stringWriter.toString().getBytes());
            return analysisFile;
        } catch (Exception e) {
            throw new SubmitException(e.getMessage(), WebinCliException.ErrorType.SYSTEM_ERROR);
        }
    }

    private void createSubmitDir() throws SubmitException {
        try {
            submitDir = Paths.get(Paths.get(outputDir) + File.separator + contextE + File.separator + assemblyName + File.separator + SUBMIT_DIR);
            if (!Files.exists(submitDir))
                Files.createDirectory(submitDir);
        } catch (IOException e) {
            throw new SubmitException(e.getMessage(), WebinCliException.ErrorType.SYSTEM_ERROR);
        }
    }
}
