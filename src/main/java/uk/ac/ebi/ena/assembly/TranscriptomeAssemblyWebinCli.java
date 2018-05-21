package uk.ac.ebi.ena.assembly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.GZIPInputStream;

import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.entry.Text;
import uk.ac.ebi.embl.api.entry.XRef;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.location.Location;
import uk.ac.ebi.embl.api.entry.location.LocationFactory;
import uk.ac.ebi.embl.api.entry.location.Order;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.Origin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.embl.api.validation.ValidationScope;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;

public class TranscriptomeAssemblyWebinCli extends AbstractWebinCli {
	private static final String VALIDATION_MESSAGES_BUNDLE = "ValidationSequenceMessages";
	private static final String STANDARD_VALIDATION_BUNDLE = "uk.ac.ebi.embl.api.validation.ValidationMessages";
	private static final String STANDARD_FIXER_BUNDLE = "uk.ac.ebi.embl.api.validation.FixerMessages";
	private final String MOL_TYPE = "transcribed RNA";
	private File reportFile;
	private File reportDir;
	private String submittedFile;
	private Sample sample;
    private Study study;
	private ValidationPlan validationPlan;
	private boolean FLAILED_VALIDATION;
	private ManifestFileReader manifestFileReader;

	public TranscriptomeAssemblyWebinCli(ManifestFileReader manifestFileReader, Sample sample, Study study) {
		this.manifestFileReader = manifestFileReader;
		this.sample = sample;
		this.study = study;
		EmblEntryValidationPlanProperty emblEntryValidationProperty = new EmblEntryValidationPlanProperty();
		emblEntryValidationProperty.validationScope.set(ValidationScope.ASSEMBLY_TRANSCRIPTOME);
		emblEntryValidationProperty.isDevMode.set(false);
		emblEntryValidationProperty.isFixMode.set(true);
		emblEntryValidationProperty.isAssembly.set(false);
		emblEntryValidationProperty.minGapLength.set(0);
		emblEntryValidationProperty.locus_tag_prefixes.set(study.getLocusTagsList());
		validationPlan = new EmblEntryValidationPlan(emblEntryValidationProperty);
		validationPlan.addMessageBundle(VALIDATION_MESSAGES_BUNDLE);
		validationPlan.addMessageBundle(STANDARD_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(STANDARD_FIXER_BUNDLE);
	}


	public void setReportsDir(String reportDir) {
		this.reportDir = new File( reportDir );
	}

	@Override
	public boolean validate() throws ValidationEngineException {
		if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FLATFILE ))!= null) {
			reportFile = FileUtils.createReportFile( reportDir, submittedFile );
			validateFlatFile();
		} else if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FASTA ))!= null) {
			reportFile = FileUtils.createReportFile( reportDir, submittedFile );
			validateFastaFile();
		} else
			throw new ValidationEngineException("Manifest file: FASTA or FLATFILE must be present.");
		return !FLAILED_VALIDATION;
	}

	private void validateFlatFile() throws ValidationEngineException {
		try  {
			Path path = Paths.get(submittedFile);
			if (!Files.exists(path))
				throw new ValidationEngineException("Flat file " + submittedFile + " does not exist");
			File flatFileF = path.toFile();
			FlatFileReader flatFileReader = new EmblEntryReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(flatFileF)))), EmblEntryReader.Format.ASSEMBLY_FILE_FORMAT, flatFileF.getName());
			ValidationResult validationResult = flatFileReader.read();
			if (validationResult != null && validationResult.getMessages(Severity.ERROR) != null && !validationResult.getMessages(Severity.ERROR).isEmpty()) {
				FLAILED_VALIDATION = true;
                FileUtils.writeReport(reportFile, validationResult);
			}
			ValidationPlanResult validationPlanResult;
			List<ValidationMessage<Origin>> validationMessagesList;
			while (flatFileReader.isEntry()) {
				Entry entry = (Entry)flatFileReader.getEntry();
				entry.getPrimarySourceFeature().setScientificName(sample.getOrganism());
				if(sample.getBiosampleId()!=null)
				entry.addXRef(new XRef("BioSample", sample.getBiosampleId()));
				if(study.getProjectId()!=null)
					 entry.addProjectAccession(new Text(study.getProjectId()));
				validationPlanResult = validationPlan.execute(entry);
				validationMessagesList = validationPlanResult.getMessages(Severity.ERROR);
				if (validationMessagesList != null && !validationMessagesList.isEmpty()) {
					FLAILED_VALIDATION = true;
                    FileUtils.writeReport(reportFile, validationMessagesList);
				}
				flatFileReader.read();
			}
		} catch (IOException e) {
			throw new ValidationEngineException(e);
		}
	}

	private void validateFastaFile() throws ValidationEngineException {
		try {
			Path path = Paths.get(submittedFile);
			if (!Files.exists(path))
				throw new ValidationEngineException("Fasta file " + submittedFile + " does not exist");
			FastaFileReader fastaFileReader = new FastaFileReader(new FastaLineReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile()))))));
			ValidationResult validationResult = fastaFileReader.read();
			if (validationResult != null && validationResult.getMessages(Severity.ERROR) != null && !validationResult.getMessages(Severity.ERROR).isEmpty()) {
				FLAILED_VALIDATION = true;
                FileUtils.writeReport(reportFile, validationResult);
			}
			ValidationPlanResult validationPlanResult;
			List<ValidationMessage<Origin>> validationMessagesList;
			while (fastaFileReader.isEntry()) {
				Entry entry = fastaFileReader.getEntry();
				SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
				sourceFeature.setScientificName(sample.getOrganism());
				if(sample.getBiosampleId()!=null)
				entry.addXRef(new XRef("BioSample", sample.getBiosampleId()));
				if(study.getProjectId()!=null)
				 entry.addProjectAccession(new Text(study.getProjectId()));
				sourceFeature.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, MOL_TYPE);
				Order<Location> featureLocation = new Order<Location>();
				featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				sourceFeature.setLocations(featureLocation);
				entry.addFeature(sourceFeature);
				validationPlanResult = validationPlan.execute(entry);
				validationMessagesList = validationPlanResult.getMessages(Severity.ERROR);
				if (validationMessagesList != null && !validationMessagesList.isEmpty()) {
					FLAILED_VALIDATION = true;
                    FileUtils.writeReport(reportFile, validationMessagesList);
				}
				fastaFileReader.read();
			}
		} catch (IOException e) {
			throw new ValidationEngineException(e);
		}
	}


	@Override
	public void prepareSubmissionBundle() throws IOException {
		// TODO Auto-generated method stub
		
	}
}
