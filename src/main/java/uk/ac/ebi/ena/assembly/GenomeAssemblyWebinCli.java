package uk.ac.ebi.ena.assembly;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import uk.ac.ebi.embl.agp.writer.AGPFileWriter;
import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.entry.Text;
import uk.ac.ebi.embl.api.entry.XRef;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.entry.genomeassembly.ChromosomeEntry;
import uk.ac.ebi.embl.api.entry.genomeassembly.UnlocalisedEntry;
import uk.ac.ebi.embl.api.entry.location.Location;
import uk.ac.ebi.embl.api.entry.location.LocationFactory;
import uk.ac.ebi.embl.api.entry.location.Order;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.FileType;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.embl.api.validation.ValidationScope;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelper;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelperImpl;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.GenomeAssemblyValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.embl.fasta.writer.FastaFileWriter;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.embl.flatfile.writer.embl.EmblEntryWriter;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.webin.cli.WebinCliInterface;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestObj;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.utils.FileUtils;

public class GenomeAssemblyWebinCli implements WebinCliInterface {
	protected FlatFileReader reader = null;
	private static List<String> chromosomeEntryNames = new ArrayList<String>();
	private static List<ChromosomeEntry> chromosomeEntries = null;
	private static HashSet<String> fastaEntryNames = new HashSet<String>();
	private static HashSet<String> agpEntrynames = new HashSet<String>();
	private static HashMap<String, List<Qualifier>> chromosomeQualifierMap = new HashMap<String, List<Qualifier>>();
	private static File chromosomeListFile;
	private static File unlocalisedListFile;
	private static List<File> fastaFiles;
	private static List<File> flatFiles;
	private static List<File> agpFiles;
	private String molType = "genomic DNA";
	private Sample sample = null;
	private boolean test;
	private ManifestFileReader manifestFileReader;
	private Study study = null;
	private String reportDir;

	public GenomeAssemblyWebinCli(ManifestFileReader manifestFileReader, Sample sample, Study study, String molType) {
		this.manifestFileReader = manifestFileReader;
		this.sample = sample;
		chromosomeListFile = null;
		unlocalisedListFile = null;
		fastaFiles = new ArrayList<File>();
		flatFiles = new ArrayList<File>();
		agpFiles = new ArrayList<File>();
		this.study = study;
		this.molType = molType == null ? this.molType : molType;
	}

	public GenomeAssemblyWebinCli(ManifestFileReader manifestFileReader, Sample sample, Study study, String molType, boolean test) {
		this(manifestFileReader, sample, study, molType);
		this.test = test;
	}

	@Override
	public void setReportsDir(String reportDir) {
		this.reportDir = reportDir;
	}

	public int validate() throws ValidationEngineException {
		boolean valid = true;
		try {
			EmblEntryValidationPlanProperty property = new EmblEntryValidationPlanProperty();
			property.isFixMode.set(true);
			property.isRemote.set(true);
			property.locus_tag_prefixes.set(study.getLocusTagsList());
			TaxonHelper taxonHelper = new TaxonHelperImpl();
			defineFileTypes();
			valid = valid & validateChromosomeList(property);
			valid = valid & validateUnlocalisedList(property);
			getChromosomeEntryNames(taxonHelper.isChildOf(sample.getOrganism(), "Virus"));
			valid = valid & validateFastaFiles(property);
			property.contigEntryNames.set(fastaEntryNames);
			valid = valid & validateAgpFiles(property);
			valid = valid & validateFlatFiles(property);
			if (!test)
				moveFiles(valid);
			return valid ? 0 : 3;
		} catch (IOException e) {
			throw new ValidationEngineException(e.getMessage());
		}

	}


	private boolean validateChromosomeList(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException {
		if (chromosomeListFile == null)
			return true;
		ValidationResult chromosomeListParseResult = new ValidationResult();
		chromosomeEntries = getChromosomeEntries(chromosomeListFile, chromosomeListParseResult);
		Writer chromosomeListRepoWriter = new PrintWriter(reportDir + File.separator + chromosomeListFile.getName() + ".report", "UTF-8");
		boolean valid=GenomeAssemblyFileUtils.writeValidationResult(chromosomeListParseResult, chromosomeListRepoWriter,chromosomeListFile.getName());
		if (chromosomeEntries != null) {
			property.fileType.set(FileType.CHROMOSOMELIST);

			for (ChromosomeEntry chromosomeEntry : chromosomeEntries) {
				valid=valid&&GenomeAssemblyFileUtils.writeValidationPlanResult(validateEntry(chromosomeEntry, property),chromosomeListRepoWriter,chromosomeListFile.getName());
			}
		}
		chromosomeListRepoWriter.flush();
		return valid;
	}

	private boolean validateUnlocalisedList(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		if (unlocalisedListFile == null)
			return true;
		ValidationResult unlocalisedParseResult = new ValidationResult();
		List<UnlocalisedEntry> unlocalisedEntries = getUnlocalisedEntries(unlocalisedListFile, unlocalisedParseResult);
		Writer unlocalisedListRepoWriter = new PrintWriter(reportDir + File.separator + unlocalisedListFile.getName() + ".report", "UTF-8");
		boolean valid=GenomeAssemblyFileUtils.writeValidationResult(unlocalisedParseResult, unlocalisedListRepoWriter,unlocalisedListFile.getName());
		if (unlocalisedEntries != null) 
		{
			property.fileType.set(FileType.UNLOCALISEDLIST);
			for (UnlocalisedEntry unlocalisedEntry : unlocalisedEntries) 
			{
				valid=valid&&GenomeAssemblyFileUtils.writeValidationPlanResult(validateEntry(unlocalisedEntry, property),unlocalisedListRepoWriter,unlocalisedListFile.getName());
			}
		}
		unlocalisedListRepoWriter.flush();
       return valid;
   }

	private boolean validateFastaFiles(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException {
		boolean valid = true;
		for (File file : fastaFiles) 
		{
			property.fileType.set(FileType.FASTA);
			try (/*Writer fixedFileWriter = new PrintWriter(file.getAbsolutePath() + ".fixed"); */Writer reportWriter = new PrintWriter(reportDir + File.separator + file.getName() + ".report", "UTF-8"); BufferedReader bf = FileUtils.getBufferedReader(file)) {
				FlatFileReader reader = GenomeAssemblyFileUtils.getFileReader(FileFormat.FASTA, file, bf);
				ValidationResult parseResult = reader.read();
				while (reader.isEntry()) {
					SourceFeature source = (new FeatureFactory()).createSourceFeature();
					source.setScientificName(sample.getOrganism());
					source.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, molType);
					Entry entry = (Entry) reader.getEntry();
					if (sample.getBiosampleId() != null)
						entry.addXRef(new XRef("BioSample", sample.getBiosampleId()));
					if (study.getProjectId() != null)
						entry.addProjectAccession(new Text(study.getProjectId()));
					if (entry.getSubmitterAccession() != null && chromosomeEntryNames.contains(entry.getSubmitterAccession().toUpperCase())) 
					{
						List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get(entry.getSubmitterAccession().toUpperCase());
						source.addQualifiers(chromosomeQualifiers);
						property.validationScope.set(ValidationScope.ASSEMBLY_CHROMOSOME);
						entry.setDataClass(Entry.STD_DATACLASS);
					} else 
					{
						property.validationScope.set(ValidationScope.ASSEMBLY_CONTIG);
						entry.setDataClass(Entry.WGS_DATACLASS);
					}

					Order<Location> featureLocation = new Order<Location>();
					featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
					source.setLocations(featureLocation);
					entry.addFeature(source);
					entry.getSequence().setMoleculeType(molType);
					ValidationPlan validationPlan = getValidationPlan(entry, property);
					valid=valid && GenomeAssemblyFileUtils.writeValidationResult(validationPlan.execute(entry), parseResult, reportWriter, file.getName());
					//FastaFileWriter writer = new FastaFileWriter(entry, fixedFileWriter);
					//writer.write();
					fastaEntryNames.add(entry.getSubmitterAccession());
					parseResult = reader.read();
				}
				//fixedFileWriter.flush();
			}
		}
		return valid;
	}

	private boolean validateFlatFiles(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException {
		boolean valid = true;
		for (File file : flatFiles) {
			property.fileType.set(FileType.EMBL);
			try (/*Writer fixedFileWriter = new PrintWriter(file.getAbsolutePath() + ".fixed");*/ Writer reportWriter = new PrintWriter(reportDir + File.separator + file.getName() + ".report", "UTF-8"); BufferedReader bf = FileUtils.getBufferedReader(file)) {
				FlatFileReader reader = GenomeAssemblyFileUtils.getFileReader(FileFormat.FLATFILE, file, bf);
				ValidationResult parseResult = reader.read();
				while (reader.isEntry()) 
				{
					Entry entry = (Entry) reader.getEntry();
					entry.removeFeature(entry.getPrimarySourceFeature());
					SourceFeature source = (new FeatureFactory()).createSourceFeature();
					source.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, molType);
					source.setScientificName(sample.getOrganism());
					if (sample.getBiosampleId() != null)
						entry.addXRef(new XRef("BioSample", sample.getBiosampleId()));
					if (study.getProjectId() != null)
						entry.addProjectAccession(new Text(study.getProjectId()));
					if (entry.getSubmitterAccession() != null && chromosomeEntryNames.contains(entry.getSubmitterAccession().toUpperCase())) 
					{
						List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get(entry.getSubmitterAccession().toUpperCase());
						source.addQualifiers(chromosomeQualifiers);
						property.validationScope.set(ValidationScope.ASSEMBLY_CHROMOSOME);
						if (entry.getSubmitterAccession() != null && agpEntrynames.contains(entry.getSubmitterAccession().toUpperCase()))
							entry.setDataClass(Entry.CON_DATACLASS);
						else
							entry.setDataClass(Entry.STD_DATACLASS);
					} else if (entry.getSubmitterAccession() != null && agpEntrynames.contains(entry.getSubmitterAccession().toUpperCase())) 
					{
						entry.setDataClass(Entry.CON_DATACLASS);
						property.validationScope.set(ValidationScope.ASSEMBLY_SCAFFOLD);
					} else {
						property.validationScope.set(ValidationScope.ASSEMBLY_CONTIG);
						entry.setDataClass(Entry.WGS_DATACLASS);
					}
					Order<Location> featureLocation = new Order<Location>();
					featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
					source.setLocations(featureLocation);
					entry.addFeature(source);
					entry.getSequence().setMoleculeType(molType);
					ValidationPlan validationPlan = getValidationPlan(entry, property);
					valid=valid && GenomeAssemblyFileUtils.writeValidationResult(validationPlan.execute(entry), parseResult, reportWriter, file.getName());
					//EmblEntryWriter writer = new EmblEntryWriter(entry);
					//writer.write(fixedFileWriter);
					parseResult = reader.read();
				}

				//fixedFileWriter.flush();
			}
		}
		return valid;
	}

	private boolean validateAgpFiles(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException {
		boolean valid = true;

		for (File file : agpFiles) 
		{
			property.fileType.set(FileType.AGP);
			try (/*Writer fixedFileWriter = new PrintWriter(file.getAbsolutePath() + ".fixed");*/ Writer reportWriter = new PrintWriter(reportDir + File.separator + file.getName() + ".report", "UTF-8"); BufferedReader bf = FileUtils.getBufferedReader(file)) {
				FlatFileReader reader = GenomeAssemblyFileUtils.getFileReader(FileFormat.AGP, file, bf);
				
				ValidationResult parseResult = reader.read();
				while (reader.isEntry()) {
					SourceFeature source = (new FeatureFactory()).createSourceFeature();
					source.setScientificName(sample.getOrganism());
					source.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, molType);
					Entry entry = (Entry) reader.getEntry();
					if (sample.getBiosampleId() != null)
						entry.addXRef(new XRef("BioSample", sample.getBiosampleId()));
					if (study.getProjectId() != null)
						entry.addProjectAccession(new Text(study.getProjectId()));
					entry.setDataClass(Entry.CON_DATACLASS);
					if (entry.getSubmitterAccession() != null && chromosomeEntryNames.contains(entry.getSubmitterAccession().toUpperCase())) 
					{
						List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get(entry.getSubmitterAccession().toUpperCase());
						source.addQualifiers(chromosomeQualifiers);
						property.validationScope.set(ValidationScope.ASSEMBLY_CHROMOSOME);
					} else
						property.validationScope.set(ValidationScope.ASSEMBLY_SCAFFOLD);
					Order<Location> featureLocation = new Order<Location>();
					featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
					source.setLocations(featureLocation);
					entry.addFeature(source);
					entry.getSequence().setMoleculeType(molType);
					ValidationPlan validationPlan = getValidationPlan(entry, property);
					valid=valid && GenomeAssemblyFileUtils.writeValidationResult(validationPlan.execute(entry), parseResult, reportWriter, file.getName());
					//AGPFileWriter writer = new AGPFileWriter(entry, fixedFileWriter);
				//	writer.write();
					agpEntrynames.add(entry.getSubmitterAccession());
					parseResult = reader.read();
				}
				//fixedFileWriter.flush();
			}

		}
		return valid;
	}

	public ValidationPlan getValidationPlan(Object entry, EmblEntryValidationPlanProperty property) {
		ValidationPlan validationPlan = null;
		if (entry instanceof AssemblyInfoEntry || entry instanceof ChromosomeEntry || entry instanceof UnlocalisedEntry)
			validationPlan = new GenomeAssemblyValidationPlan(property);
		else
			validationPlan = new EmblEntryValidationPlan(property);
		validationPlan.addMessageBundle(ValidationMessageManager.STANDARD_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.STANDARD_FIXER_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_FIXER_BUNDLE);
		return validationPlan;
	}

	private List<ChromosomeEntry> getChromosomeEntries(File chromosomeFile, ValidationResult parseResult) throws IOException {
		if (chromosomeFile == null)
			return null;
		ChromosomeListFileReader reader = (ChromosomeListFileReader) GenomeAssemblyFileUtils.getFileReader(FileFormat.CHROMOSOME_LIST, chromosomeFile, null);
		parseResult = reader.read();
		if (reader.isEntry())
			return reader.getentries();
		return null;
	}

	private List<UnlocalisedEntry> getUnlocalisedEntries(File unlocalisedFile, ValidationResult parseResult) throws IOException {
		if (unlocalisedFile == null)
			return null;
		UnlocalisedListFileReader reader = (UnlocalisedListFileReader) GenomeAssemblyFileUtils.getFileReader(FileFormat.UNLOCALISED_LIST, unlocalisedFile, null);
		parseResult = reader.read();
		if (reader.isEntry())
			return reader.getentries();
		return null;
	}

	private ValidationPlanResult validateEntry(Object entry, EmblEntryValidationPlanProperty property) throws ValidationEngineException {
		ValidationPlan validationPlan = getValidationPlan(entry, property);
		return validationPlan.execute(entry);
	}

	private List<String> getChromosomeEntryNames(boolean isVirus) {
		if (chromosomeEntries == null)
			return chromosomeEntryNames;

		for (ChromosomeEntry chromosomeEntry : chromosomeEntries) {
			chromosomeEntryNames.add(chromosomeEntry.getObjectName().toUpperCase());
			chromosomeQualifierMap.put(chromosomeEntry.getObjectName().toUpperCase(), GenomeAssemblyFileUtils.getChromosomeQualifier(chromosomeEntry, isVirus));
		}

		return chromosomeEntryNames;
	}

	private void defineFileTypes() throws ValidationEngineException, IOException {
		for (ManifestObj obj : manifestFileReader.getManifestFileObjects()) {
			String fileName = obj.getFileName();
			if (test)
				fileName = GenomeAssemblyFileUtils.getFile(fileName);
			File file = new File(fileName);
			switch (obj.getFileFormat()) {
				case CHROMOSOME_LIST:
					chromosomeListFile = file;
					break;
				case UNLOCALISED_LIST:
					unlocalisedListFile = file;
					break;
				case FASTA:
					fastaFiles.add(file);
					break;
				case FLATFILE:
					flatFiles.add(file);
					break;
				case AGP:
					agpFiles.add(file);
					break;
				default:
					break;
			}
		}
	}

	private void moveFiles(boolean valid) throws IOException {
		for (ManifestObj mo : manifestFileReader.getManifestFileObjects()) {
			switch (mo.getFileFormat()) {
				case FASTA:
				case FLATFILE:
				case AGP:
					if (!valid) {
						File fixedFile = new File(mo.getFileName() + ".fixed");
						if (fixedFile.exists())
							fixedFile.delete();
					}
					break;
				default:
					break;
			}
		}
	}
}
