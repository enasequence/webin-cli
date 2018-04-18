package uk.ac.ebi.ena.assembly;

import org.apache.commons.io.IOUtils;
import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.ena.core.SubmissionFileLocationE;
import uk.ac.ebi.ena.core.exception.SystemException;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.sra.pipeline.configuration.Configuration;
import uk.ac.ebi.ena.template.expansion.TemplateEntryProcessor;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliInterface;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.zip.GZIPInputStream;

public class SequenceAssemblyWebinCli implements WebinCliInterface {
    private boolean FAILED_VALIDATION;
    private ManifestFileReader manifestFileReader;
    private String submittedFile;
    private String reportFile;
    private String reportDir;
    private int sequenceCount;

    public SequenceAssemblyWebinCli(ManifestFileReader manifestFileReader) {
        this.manifestFileReader = manifestFileReader;
    }

    @Override
    public int validate() throws ValidationEngineException {
        if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FLATFILE ))!= null) {
            FileUtils.createReportFile(submittedFile, reportFile, reportDir);
            validateFlatFile();
        } else if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FASTA ))!= null) {
            FileUtils.createReportFile(submittedFile, reportFile, reportDir);
            validateTsvFile();
        } else
            throw new ValidationEngineException("Manifest file: TSV or FLATFILE must be present.");
        if (FAILED_VALIDATION)
            return WebinCli.VALIDATION_ERROR;
        return WebinCli.SUCCESS;
    }

    @Override
    public void setReportsDir(String reportDir) {
        this.reportDir = reportDir;
    }

    private void validateTsvFile()  throws ValidationEngineException {
        getTemplateFromDatabaseAndWriteToProcessDir();
    }

    private void validateFlatFile() throws ValidationEngineException {
        try {
            File submittedFileF = new File(submittedFile);
            if (!submittedFileF.exists()) {
                FAILED_VALIDATION = true;
                FileUtils.writeReport(reportFile, Severity.ERROR, submittedFile + " does not exist.");
                return;
            }
            FlatFileReader flatFileReader = new EmblEntryReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(submittedFileF)))), EmblEntryReader.Format.EMBL_FORMAT, submittedFileF.getName());
            ValidationResult validationResult = flatFileReader.read();
            if (!flatFileReader.isEntry()) {
                FAILED_VALIDATION = true;
                FileUtils.writeReport(reportFile, validationResult);
                return;
            }
            while (flatFileReader.isEntry()) {
                if (validationResult != null && validationResult.getMessages(Severity.ERROR) != null && !validationResult.getMessages(Severity.ERROR).isEmpty()) {
                    FAILED_VALIDATION = true;
                    FileUtils.writeReport(reportFile, validationResult);
                }
                Entry entry = (Entry)flatFileReader.getEntry();
                entry.getSequence().setVersion(1);
                if (entry.getProjectAccessions() != null && !entry.getProjectAccessions().isEmpty())
                    entry.getProjectAccessions().clear();
                TemplateEntryProcessor templateEntryProcessor = new TemplateEntryProcessor(ValidationScope.EMBL);
                ValidationPlanResult validationPlanResult = templateEntryProcessor.validateSequenceUploadEntry(entry);
                if (!validationPlanResult.isValid()) {
                    FAILED_VALIDATION = true;
                    FileUtils.writeReport(reportFile, validationPlanResult.getMessages(Severity.ERROR));
                }
                sequenceCount++;
                validationResult = flatFileReader.read();
            }
        } catch (Exception e) {
            throw new ValidationEngineException(e);
        }
    }

    private void getTemplateFromDatabaseAndWriteToProcessDir(String templateId) throws Exception {
        String template = IOUtils.toString(getClass().getClassLoader().getResourceAsStream("template/" + templateId + ".xml"));
        /*
        if (template == null || template.isEmpty())
            throw  new SystemException("- GenerateFlatFile.getTemplateFromDatabaseAndWriteToProcessDir(): no template found for analysis id: " + analysisId);
        if (template.contains("encoding=\"\""))
            template = template.replace("encoding=\"\"", "encoding=\"UTF-8\"");
        PrintWriter out = null;
        try {
            out = new PrintWriter(Configuration.getFileLocation(analysisId, TEMPLATE_FILE_NAME + templateId + Configuration.XML_SUFFIX, SubmissionFileLocationE.PROCESS_DIR));
            out.print(template);
        } catch (Exception e) {
            throw new SystemException(e.getMessage());
        } finally {
            if (out != null)
                out.close();
        }
        */
    }

    private String getTemplateIdFromTsvFile() {
        Files.readAllLines(Paths.get(submittedFile)).
        return "";
    }
}
