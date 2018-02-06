package uk.ac.ebi.ena.assembly;

import java.io.IOException;
import java.nio.file.Files;

import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.entry.location.Location;
import uk.ac.ebi.embl.api.entry.location.LocationFactory;
import uk.ac.ebi.embl.api.entry.location.Order;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliInterface;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;

public class TranscriptomeAssemblyWebinCli implements WebinCliInterface {
	private static final String VALIDATION_MESSAGES_BUNDLE = "ValidationSequenceMessages";
	private static final String STANDARD_VALIDATION_BUNDLE = "uk.ac.ebi.embl.api.validation.ValidationMessages";
	private static final String STANDARD_FIXER_BUNDLE = "uk.ac.ebi.embl.api.validation.FixerMessages";
	private final static String ASSEMBLY_NAME = "Assembly Name:";
	private final static String ORGANISM = "Organism:";
	private String reportFile = "TRANSCRIPTOME.report";
	private String outputDir;
	private String submittedFile;
	private AssemblyInfoEntry assemblyInfoEntr;
	private String organism;
	private List<String> locusTagsList;
	private ValidationPlan validationPlan;
	private boolean FLAILED_VALIDATION;
	private ManifestFileReader manifestFileReader;

	public TranscriptomeAssemblyWebinCli(ManifestFileReader manifestFileReader, AssemblyInfoEntry assemblyInfoEntry, String organism, List<String> locusTagsList) {
		this.manifestFileReader = manifestFileReader;
		this.assemblyInfoEntr = assemblyInfoEntry;
		this.organism = organism;
		this.locusTagsList = locusTagsList;
		EmblEntryValidationPlanProperty emblEntryValidationProperty = new EmblEntryValidationPlanProperty();
		emblEntryValidationProperty.validationScope.set(ValidationScope.ASSEMBLY_TRANSCRIPTOME);
		emblEntryValidationProperty.isDevMode.set(false);
		emblEntryValidationProperty.isFixMode.set(true);
		emblEntryValidationProperty.isAssembly.set(false);
		emblEntryValidationProperty.minGapLength.set(0);
		validationPlan = new EmblEntryValidationPlan(emblEntryValidationProperty);
		validationPlan.addMessageBundle(VALIDATION_MESSAGES_BUNDLE);
		validationPlan.addMessageBundle(STANDARD_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(STANDARD_FIXER_BUNDLE);
	}

	@Override
	public void setOutputDir(String outputDir) {
		this.outputDir = outputDir;
	}

   @Override
	public int validate() throws ValidationEngineException {
		createReportFile();
		if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FLATFILE ))!= null)
			validateFlatFile();
		else if ((submittedFile = manifestFileReader.getFilenameFromManifest(FileFormat.FASTA ))!= null)
			validateFastaFile();
		else
			throw new ValidationEngineException("Manifest file: FASTA or FLATFILE must be present.");
		if (FLAILED_VALIDATION)
			return WebinCli.FLAILED_VALIDATION;
		return WebinCli.SUCCESS;
	}

	private void createReportFile() throws ValidationEngineException {
		try {
			reportFile = outputDir +  (outputDir.endsWith(File.separator) ? "" : File.separator) + reportFile;
			Path reportPath = Paths.get(reportFile);
			if (Files.exists(reportPath))
				Files.delete(reportPath);
			Files.createFile(reportPath);
		} catch (IOException e) {
			throw new ValidationEngineException("Unable to create report file.");
		}
	}

	private void validateFlatFile() throws ValidationEngineException {
		try  {
			Path path = Paths.get(submittedFile);
			if (!Files.exists(path))
				throw new Exception("Flat file " + submittedFile + " does not exist");
			File flatFileF = path.toFile();
			FlatFileReader flatFileReader = new EmblEntryReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(flatFileF)))), EmblEntryReader.Format.ASSEMBLY_FILE_FORMAT, flatFileF.getName());
			ValidationResult validationResult = flatFileReader.read();
			if (validationResult != null && validationResult.getMessages(Severity.ERROR) != null && !validationResult.getMessages(Severity.ERROR).isEmpty()) {
				FLAILED_VALIDATION = true;
				writeReport(validationResult);
			}
			ValidationPlanResult validationPlanResult;
			List<ValidationMessage<Origin>> validationMessagesList;
			while (flatFileReader.isEntry()) {
				Entry entry = (Entry)flatFileReader.getEntry();
				entry.getPrimarySourceFeature().setScientificName(organism);
				validationPlanResult = validationPlan.execute(entry);
				validationMessagesList = validationPlanResult.getMessages(Severity.ERROR);
				if (validationMessagesList != null && !validationMessagesList.isEmpty()) {
					FLAILED_VALIDATION = true;
					writeReport(validationMessagesList);
				}
				flatFileReader.read();
			}
		} catch (Exception e) {
			throw new ValidationEngineException(e);
		}
	}

	private void validateFastaFile() throws ValidationEngineException {
		try {
			Path path = Paths.get(submittedFile);
			if (!Files.exists(path))
				throw new Exception("Fasta file " + submittedFile + " does not exist");
			FastaFileReader fastaFileReader = new FastaFileReader(new FastaLineReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile()))))));
			ValidationResult validationResult = fastaFileReader.read();
			if (validationResult != null && validationResult.getMessages(Severity.ERROR) != null && !validationResult.getMessages(Severity.ERROR).isEmpty()) {
				FLAILED_VALIDATION = true;
				writeReport(validationResult);
			}
			ValidationPlanResult validationPlanResult;
			List<ValidationMessage<Origin>> validationMessagesList;
			while (fastaFileReader.isEntry()) {
				Entry entry = fastaFileReader.getEntry();
				SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
				sourceFeature.setScientificName(organism);
				sourceFeature.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, assemblyInfoEntr.getMoleculeType());
				Order<Location> featureLocation = new Order<Location>();
				featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				sourceFeature.setLocations(featureLocation);
				entry.addFeature(sourceFeature);
				validationPlanResult = validationPlan.execute(entry);
				validationMessagesList = validationPlanResult.getMessages(Severity.ERROR);
				if (validationMessagesList != null && !validationMessagesList.isEmpty()) {
					FLAILED_VALIDATION = true;
					writeReport(validationMessagesList);
				}
				fastaFileReader.read();
			}
		} catch (Exception e) {
			throw new ValidationEngineException(e);
		}
	}

	private void writeReport(ValidationResult validationResult) throws Exception {
		Collection<ValidationMessage<Origin>> validationMessagesList =  validationResult.getMessages();
		for (ValidationMessage validationMessage: validationMessagesList)
		Files.write(Paths.get(reportFile), validationMessage.getMessage().getBytes(), StandardOpenOption.APPEND);
	}

	private void writeReport(List<ValidationMessage<Origin>> validationMessagesList) throws Exception {
		for (ValidationMessage validationMessage: validationMessagesList)
		Files.write(Paths.get(reportFile), (validationMessage.getMessage() + "\n").getBytes(), StandardOpenOption.APPEND);
	}
}
