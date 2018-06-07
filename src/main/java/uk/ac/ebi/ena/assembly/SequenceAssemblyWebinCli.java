package uk.ac.ebi.ena.assembly;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.entry.reference.Person;
import uk.ac.ebi.embl.api.entry.reference.Publication;
import uk.ac.ebi.embl.api.entry.reference.Reference;
import uk.ac.ebi.embl.api.entry.reference.ReferenceFactory;
import uk.ac.ebi.embl.api.entry.reference.Submission;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.embl.api.validation.ValidationScope;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.template.expansion.CSVLine;
import uk.ac.ebi.ena.template.expansion.CSVReader;
import uk.ac.ebi.ena.template.expansion.TemplateEntryProcessor;
import uk.ac.ebi.ena.template.expansion.TemplateInfo;
import uk.ac.ebi.ena.template.expansion.TemplateLoader;
import uk.ac.ebi.ena.template.expansion.TemplateProcessor;
import uk.ac.ebi.ena.template.expansion.TemplateUserError;
import uk.ac.ebi.ena.utils.FileUtils;

public class SequenceAssemblyWebinCli extends SequenceWebinCli {
    private static final String TEMPLATE_ID_PATTERN = "(ERT[0-9]+)";
    private final static String TEMPLATE_ACCESSION_LINE = "#template_accession";
    private boolean FAILED_VALIDATION;
    private ManifestFileReader manifestFileReader;
    private String submittedFile;
    private File reportFile;
    private File reportDir;
    private Study study;
    private final static int MAX_SEQUENCE_COUNT = 100000;
    private boolean TEST;
    private StringBuilder resultsSb;

    public SequenceAssemblyWebinCli(ManifestFileReader manifestFileReader, Study study) {
        this.manifestFileReader = manifestFileReader;
        this.study = study;
    }

    public SequenceAssemblyWebinCli() {
    }

    public StringBuilder validateTestTsv(String submittedFile) throws ValidationEngineException {
        this.submittedFile = submittedFile;
        resultsSb = new StringBuilder();
        TEST = true;
        validateTsvFile();
        return resultsSb;
    }


    @Override protected boolean validateInternal() throws ValidationEngineException {
        if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FLATFILE ))!= null) {
            reportFile = FileUtils.createReportFile( reportDir, submittedFile );
            validateFlatFile();
        } else if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.TAB))!= null) {
            reportFile = FileUtils.createReportFile( reportDir, submittedFile );
            validateTsvFile();
        } else
            throw new ValidationEngineException("Manifest file: TAB or FLATFILE must be pre55t4444eeeeeszsent.");
        return !FAILED_VALIDATION;
    }

    public void setReportsDir(String reportDir) {
        this.reportDir = new File( reportDir );
    }

    private void validateTsvFile()  throws ValidationEngineException {
        Path templatePath = getTemplateAndWriteToValidateDir();
        try (FileInputStream submittedDataFis = new FileInputStream(submittedFile)) {
            TemplateInfo templateInfo = new TemplateLoader().loadTemplateFromFile(templatePath.toFile());
            TemplateProcessor templateProcessor = new TemplateProcessor(templateInfo, null);
            BufferedInputStream bufferedInputStremSubmittedData = new BufferedInputStream(new GZIPInputStream(submittedDataFis));
            CSVReader csvReader = new CSVReader(bufferedInputStremSubmittedData, templateInfo.getTokens(), 0);
            CSVLine csvLine;
            int lineCount = 0;
            while ((csvLine = csvReader.readTemplateSpreadsheetLine()) != null) {
                lineCount = csvLine.getLineNumber();
                if (lineCount == MAX_SEQUENCE_COUNT)
                    throw new ValidationEngineException("Data file has exceeded the maximum permitted number of sequencies (" + MAX_SEQUENCE_COUNT + ")" + " that are allowed in one data file.");
                ValidationPlanResult validationPlanResult = templateProcessor.process(csvLine.getEntryTokenMap()).getValidationPlanResult();
                if (!validationPlanResult.isValid()) {
                    List<ValidationMessage<Origin>> validationMessagesList = validationPlanResult.getMessages(Severity.ERROR);
                    if (validationMessagesList != null && !validationMessagesList.isEmpty()) {
                        FAILED_VALIDATION = true;
                        if (TEST) {
                            for( ValidationMessage<?> validationMessage: validationMessagesList)
                                resultsSb.append("ERROR: Sequence " + csvLine.getLineNumber().toString() + ": " + validationMessage.getMessage() + "\n");
                        } else
                            FileUtils.writeReport(reportFile, validationMessagesList, "ERROR: Sequence: " + csvLine.getLineNumber().toString() + " ");
                    }
                }
            }
        } catch (TemplateUserError e) {
            FAILED_VALIDATION = true;
            if (TEST)
                resultsSb.append(e.getMessage());
            else
                FileUtils.writeReport(reportFile, Severity.ERROR, e.getMessage());
        } catch (Exception e) {
            throw new ValidationEngineException(e.getMessage());
        }
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
                addDefaultCitationForOfflineValisation(entry);
                if (entry.getProjectAccessions() != null && !entry.getProjectAccessions().isEmpty())
                    entry.getProjectAccessions().clear();
                TemplateEntryProcessor templateEntryProcessor = new TemplateEntryProcessor(ValidationScope.EMBL);
                ValidationPlanResult validationPlanResult = templateEntryProcessor.validateSequenceUploadEntry(entry);
                if (!validationPlanResult.isValid()) {
                    FAILED_VALIDATION = true;
                    FileUtils.writeReport(reportFile, validationPlanResult.getMessages(Severity.ERROR), "ERROR: Entry " + ((EmblEntryReader)flatFileReader).getLineReader().getCurrentLineNumber() + " ");
                }
                validationResult = flatFileReader.read();
            }
        } catch (Exception e) {
            throw new ValidationEngineException(e);
        }
    }

    private Path getTemplateAndWriteToValidateDir() throws ValidationEngineException {
        try {
            String templateId = getTemplateIdFromTsvFile();
            String template = new TemplateProcessor().getTemplate(templateId);
            Path path = Paths.get(reportDir + File.separator + templateId + ".xml");
            Files.deleteIfExists(path);
            Files.createFile(path);
            Files.write(path, template.getBytes());
            return path;
        } catch (ValidationEngineException e) {
            throw e;
        } catch (Exception e) {
            throw new ValidationEngineException(e.getMessage());
        }
    }

    private String getTemplateIdFromTsvFile() throws ValidationEngineException {
        String templateId = "";
        try {
            Optional<String> optional =  new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(Paths.get(submittedFile).toFile())))).lines()
                    .filter(line -> line.startsWith(TEMPLATE_ACCESSION_LINE))
                    .findFirst();
            if (optional.isPresent()) {
                templateId = optional.get().replace(TEMPLATE_ACCESSION_LINE, "").trim();
                if (templateId.isEmpty() || !templateId.matches(TEMPLATE_ID_PATTERN))
                    throw new ValidationEngineException(TEMPLATE_ACCESSION_LINE + " template id '" + templateId + " is missing or not in the correct format. Example id is ERT000003");
            } else
                throw new ValidationEngineException("File " + submittedFile + " is missing the '" +  TEMPLATE_ACCESSION_LINE + "' line, please add it followed by the template id");
        } catch (IOException e) {
            e.printStackTrace();
        }
        return templateId;
    }

    private void addDefaultCitationForOfflineValisation(Entry entry) {
        ReferenceFactory referenceFactory = new ReferenceFactory();
        Reference reference = referenceFactory.createReference();
        Publication publication = new Publication();
        Person person = referenceFactory.createPerson("CLELAND");
        publication.addAuthor(person);
        reference.setAuthorExists(true);
        Submission submission = referenceFactory.createSubmission(publication);
        submission.setSubmitterAddress(", The European Bioinformatics Institute (EMBL-EBI), Wellcome Genome Campus, CB10 1SD, United Kingdom");
        submission.setDay(Calendar.getInstance().getTime());
        publication = submission;
        reference.setPublication(publication);
        reference.setLocationExists(true);
        reference.setReferenceNumber(1);
        entry.addReference(reference);
    }

	
    @Override ContextE
    getContext()
    {
        return ContextE.sequence;
    }

    
    @Override boolean
    getTestMode()
    {
        return TEST;
    }
}
