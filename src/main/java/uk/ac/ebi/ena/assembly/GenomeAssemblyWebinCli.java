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
import uk.ac.ebi.ena.utils.FileUtils;

public class GenomeAssemblyWebinCli implements WebinCliInterface
{
	protected FlatFileReader reader = null;
	private static List<String> chromosomeEntryNames = new ArrayList<String>();
	private static List<ChromosomeEntry> chromosomeEntries = null;
	private static HashSet<String> fastaEntryNames = new HashSet<String>();
	private static HashSet<String> agpEntrynames = new HashSet<String>();
	private static HashMap<String, List<Qualifier>> chromosomeQualifierMap = new HashMap<String, List<Qualifier>>();
	private static File assemblyInfoFile;
	private static File chromosomeListFile;
	private static File unlocalisedListFile;
	private static List<File> fastaFiles = new ArrayList<File>();
	private static List<File> flatFiles = new ArrayList<File>();
	private static List<File> agpFiles = new ArrayList<File>();
	private static String molType= "genomeDNA";
	private static String organism=null;
	private static String originalFileDir= null;;
	private static String reportDir=null;
	private boolean test;
	private ManifestFileReader manifestFileReader;
	private String assemblyName=null;
	private String outputDir=null;
	private List<String> locusTagsList;

	public GenomeAssemblyWebinCli(ManifestFileReader manifestFileReader, List<String> locusTagsList) {
		this.manifestFileReader = manifestFileReader;
		this.locusTagsList = locusTagsList;
	}
	
	public GenomeAssemblyWebinCli(ManifestFileReader manifestFileReader, List<String> locusTagsList, boolean test) {
		this(manifestFileReader, locusTagsList);
		this.test = test;
	}

	public int validate() throws ValidationEngineException
	{
		boolean valid =true;
		try
		{
			EmblEntryValidationPlanProperty property = new EmblEntryValidationPlanProperty();
			property.isFixMode.set(true);
			property.isRemote.set(true);
			TaxonHelper taxonHelper = new TaxonHelperImpl();
	        reportDir= outputDir+File.separator+"reports";
	        File rDir= new File(reportDir);
	        if(!rDir.exists())
	        	rDir.mkdirs();
	        originalFileDir=outputDir+File.separator+"originalSequenceFiles";
	        File originalDir= new File(originalFileDir);
	        if(!originalDir.exists())
	        	originalDir.mkdirs();
			defineFileTypes();
			if (assemblyInfoFile == null)
			{ 
				System.err.println("Assembly info file is missing - exiting");
				return 2;
			}
			valid= valid&validateAssemblyInfo(property);
			if(!valid)
			{ 
					System.err.println("Assembly info file validation failed - exiting");
					return 2;
			}
			valid = valid&validateChromosomeList(property);
			valid = valid&validateUnlocalisedList(property);
			getChromosomeEntryNames(taxonHelper.isChildOf(organism, "Virus"));
			valid = valid&validateFastaFiles(property);
			property.contigEntryNames.set(fastaEntryNames);
			valid = valid&validateAgpFiles(property);
			valid = valid&validateFlatFiles(property);
			if(!test)
				moveFiles(valid);
			return valid?0:3;
		} catch (Exception e)
		{
			throw new ValidationEngineException(e.getMessage());
		}

	}

	private boolean validateAssemblyInfo(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		List<ValidationPlanResult> assemblyInfoPlanResults = new ArrayList<ValidationPlanResult>();
		List<ValidationResult> assemblyInfoParseResults = new ArrayList<ValidationResult>();
		ValidationResult assemblyInfoParseResult= new ValidationResult();
		AssemblyInfoEntry assemblyInfoEntry = FileUtils.getAssemblyEntry(assemblyInfoFile, assemblyInfoParseResult);
		assemblyName= assemblyInfoEntry.getName().trim();
		assemblyInfoParseResults.add(assemblyInfoParseResult);
		Writer assemblyInforepoWriter = new PrintWriter(reportDir+File.separator+assemblyInfoFile.getName() + ".report", "UTF-8");
		if (assemblyInfoEntry != null)
		{
			property.fileType.set(FileType.ASSEMBLYINFO);
			assemblyInfoPlanResults.add(validateEntry(assemblyInfoEntry,property));
            molType=assemblyInfoEntry.getMoleculeType();
    		organism = assemblyInfoEntry.getSampleId();//get organism from sample id
		}
      return GenomeAssemblyFileUtils.writeValidationResult(assemblyInfoParseResults,assemblyInfoPlanResults, assemblyInforepoWriter,assemblyInfoFile.getName());
	}
	
	private boolean validateChromosomeList(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		if(chromosomeListFile==null)
			return true;
		List<ValidationPlanResult> chromosomeListPlanResults = new ArrayList<ValidationPlanResult>();
		List<ValidationResult> chromosomeListParseResults = new ArrayList<ValidationResult>();
		ValidationResult chromosomeListParseResult= new ValidationResult();		
		chromosomeEntries = getChromosomeEntries(chromosomeListFile,chromosomeListParseResult);
		chromosomeListParseResults.add(chromosomeListParseResult);
		Writer chromosomeListRepoWriter = new PrintWriter(reportDir+File.separator+chromosomeListFile.getName() + ".report", "UTF-8");
		if (chromosomeEntries != null)
		{
			property.fileType.set(FileType.CHROMOSOMELIST);

			for (ChromosomeEntry chromosomeEntry : chromosomeEntries)
			{
				chromosomeListPlanResults.add(validateEntry(chromosomeEntry, property));
			}
		}
		return GenomeAssemblyFileUtils.writeValidationResult(chromosomeListParseResults,chromosomeListPlanResults, chromosomeListRepoWriter,chromosomeListFile.getName());
	}
	
	private boolean validateUnlocalisedList(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		if(unlocalisedListFile==null)
			return true;
		ValidationResult unlocalisedParseResult= new ValidationResult();	
		List<ValidationPlanResult> unlocalisedListPlanResults = new ArrayList<ValidationPlanResult>();
		List<ValidationResult> unlocalisedListParseResults = new ArrayList<ValidationResult>();
		List<UnlocalisedEntry> unlocalisedEntries = getUnlocalisedEntries(unlocalisedListFile, unlocalisedParseResult);
		unlocalisedListParseResults.add(unlocalisedParseResult);
		Writer unlocalisedListRepoWriter = new PrintWriter(reportDir+File.separator+unlocalisedListFile.getName() + ".report", "UTF-8");
		if (unlocalisedEntries != null)
		{
			property.fileType.set(FileType.UNLOCALISEDLIST);
			for (UnlocalisedEntry unlocalisedEntry : unlocalisedEntries)
			{
				unlocalisedListPlanResults.add(validateEntry(unlocalisedEntry, property));
			}
		}
		return GenomeAssemblyFileUtils.writeValidationResult(unlocalisedListParseResults,unlocalisedListPlanResults, unlocalisedListRepoWriter,unlocalisedListFile.getName());
	}
	
	private  boolean validateFastaFiles(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		boolean valid =true;
		for (File file : fastaFiles)
		{
			property.fileType.set(FileType.FASTA);
			try (Writer fixedFileWriter= new PrintWriter(file.getAbsolutePath()+".fixed"); Writer reportWriter = new PrintWriter(reportDir+File.separator+file.getName()+ ".report", "UTF-8");BufferedReader bf=FileUtils.getBufferedReader(file))
			{
			FlatFileReader reader = GenomeAssemblyFileUtils.getFileReader(FileFormat.FASTA,file,bf);
			List<ValidationResult> parseResults = new ArrayList<ValidationResult>();
			List<ValidationPlanResult> planResults = new ArrayList<ValidationPlanResult>();
			ValidationResult parseResult = reader.read();
			if (parseResult != null)
				{
					parseResults.add(parseResult);
				}
			while (reader.isEntry())
			{
				parseResult = null;
				SourceFeature source = (new FeatureFactory()).createSourceFeature();
				source.setScientificName(organism);
				source.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, molType);
				Entry entry = (Entry) reader.getEntry();
				if (entry.getSubmitterAccession() != null&& chromosomeEntryNames.contains(entry.getSubmitterAccession().toUpperCase()))
				{
					List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get(entry.getSubmitterAccession().toUpperCase());
					source.addQualifiers(chromosomeQualifiers);
					property.validationScope.set(ValidationScope.ASSEMBLY_CHROMOSOME);
					entry.setDataClass(Entry.STD_DATACLASS);
				}
				else
				{
					property.validationScope.set(ValidationScope.ASSEMBLY_CONTIG);
					entry.setDataClass(Entry.WGS_DATACLASS);
				}
				
				Order<Location> featureLocation = new Order<Location>();
				featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				source.setLocations(featureLocation);
				entry.addFeature(source);
				entry.getSequence().setMoleculeType(molType);
				ValidationPlan validationPlan = getValidationPlan(entry,property);
				planResults.add(validationPlan.execute(entry));
				FastaFileWriter writer = new FastaFileWriter(entry, fixedFileWriter);
				writer.write();
				fastaEntryNames.add(entry.getSubmitterAccession());
				parseResult = reader.read();
				if(parseResult != null)
				  parseResults.add(parseResult);
			}
			valid=valid && GenomeAssemblyFileUtils.writeValidationResult(parseResults, planResults,reportWriter,file.getName());
			fixedFileWriter.flush();
			}
		}
		return valid;
	}
	
	private boolean validateFlatFiles(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		boolean valid =true;
		for (File file : flatFiles)
		{
			property.fileType.set(FileType.EMBL);
			try (Writer fixedFileWriter= new PrintWriter(file.getAbsolutePath()+".fixed"); Writer reportWriter = new PrintWriter(reportDir+File.separator+file.getName()+ ".report", "UTF-8");BufferedReader bf=FileUtils.getBufferedReader(file))
			{
			FlatFileReader reader = GenomeAssemblyFileUtils.getFileReader(FileFormat.FLATFILE,file,bf);
			List<ValidationResult> parseResults = new ArrayList<ValidationResult>();
			List<ValidationPlanResult> planResults = new ArrayList<ValidationPlanResult>();
			ValidationResult parseResult = reader.read();
			if (parseResult != null)
				{
					parseResults.add(parseResult);
				}
			while (reader.isEntry())
			{
				parseResult = null;
				Entry entry = (Entry) reader.getEntry();
				entry.removeFeature(entry.getPrimarySourceFeature());
				SourceFeature source = (new FeatureFactory()).createSourceFeature();
				source.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, molType);
				source.setScientificName(organism);
				if (entry.getSubmitterAccession() != null&& chromosomeEntryNames.contains(entry.getSubmitterAccession().toUpperCase()))
				{
					List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get(entry.getSubmitterAccession().toUpperCase());
					source.addQualifiers(chromosomeQualifiers);
					property.validationScope.set(ValidationScope.ASSEMBLY_CHROMOSOME);
					if (entry.getSubmitterAccession() != null&& agpEntrynames.contains(entry.getSubmitterAccession().toUpperCase()))
						entry.setDataClass(Entry.CON_DATACLASS);
					else
						entry.setDataClass(Entry.STD_DATACLASS);
				} 
				else 
				if (entry.getSubmitterAccession() != null&& agpEntrynames.contains(entry.getSubmitterAccession().toUpperCase()))
				{
					entry.setDataClass(Entry.CON_DATACLASS);
				    property.validationScope.set(ValidationScope.ASSEMBLY_SCAFFOLD);
				}
				else
				{
					property.validationScope.set(ValidationScope.ASSEMBLY_CONTIG);
					entry.setDataClass(Entry.WGS_DATACLASS);
				}
				Order<Location> featureLocation = new Order<Location>();
				featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				source.setLocations(featureLocation);
				entry.addFeature(source);
				entry.getSequence().setMoleculeType(molType);
				ValidationPlan validationPlan = getValidationPlan(entry,property);
				planResults.add(validationPlan.execute(entry));
				EmblEntryWriter writer = new EmblEntryWriter(entry);
				writer.write(fixedFileWriter);
				parseResult = reader.read();
				if (parseResult != null)
					parseResults.add(parseResult);
			}

			valid=valid  && GenomeAssemblyFileUtils.writeValidationResult(parseResults, planResults,reportWriter,file.getName());
			fixedFileWriter.flush();
			}
		}
		return valid;
	}
	private boolean validateAgpFiles(EmblEntryValidationPlanProperty property) throws IOException, ValidationEngineException
	{
		boolean valid= true;
		
		for (File file : agpFiles)
		{
			property.fileType.set(FileType.AGP);
			try (Writer fixedFileWriter= new PrintWriter(file.getAbsolutePath()+".fixed"); Writer reportWriter = new PrintWriter(reportDir+File.separator+file.getName()+ ".report", "UTF-8");BufferedReader bf=FileUtils.getBufferedReader(file))
			{
			FlatFileReader reader = GenomeAssemblyFileUtils.getFileReader(FileFormat.AGP,file,bf);
      		List<ValidationResult> parseResults = new ArrayList<ValidationResult>();
			List<ValidationPlanResult> planResults = new ArrayList<ValidationPlanResult>();
  			ValidationResult parseResult = reader.read();
			
				if (parseResult != null)
				{
 					parseResults.add(parseResult);
				}
			
            while (reader.isEntry())
			{
				parseResult = null;
				SourceFeature source = (new FeatureFactory()).createSourceFeature();
				source.setScientificName(organism);
				source.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, molType);
				Entry entry = (Entry) reader.getEntry();
				entry.setDataClass(Entry.CON_DATACLASS);
				if (entry.getSubmitterAccession() != null && chromosomeEntryNames.contains(entry.getSubmitterAccession().toUpperCase()))
				{
					List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get(entry.getSubmitterAccession().toUpperCase());
					source.addQualifiers(chromosomeQualifiers);
					property.validationScope.set(ValidationScope.ASSEMBLY_CHROMOSOME);
				}
				else
					property.validationScope.set(ValidationScope.ASSEMBLY_SCAFFOLD);
				Order<Location> featureLocation = new Order<Location>();
				featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				source.setLocations(featureLocation);
				entry.addFeature(source);
				entry.getSequence().setMoleculeType(molType);
				ValidationPlan validationPlan = getValidationPlan(entry,property);
				planResults.add(validationPlan.execute(entry));
				AGPFileWriter writer = new AGPFileWriter(entry, fixedFileWriter);
				writer.write();
				agpEntrynames.add(entry.getSubmitterAccession());
				parseResult = reader.read();
				if (parseResult != null)
					parseResults.add(parseResult);
			}
			valid =valid && GenomeAssemblyFileUtils.writeValidationResult(parseResults, planResults, reportWriter,file.getName());
			fixedFileWriter.flush();
			}

		}
		return valid;
	}
	
	public ValidationPlan getValidationPlan(Object entry,EmblEntryValidationPlanProperty property)
	{
		ValidationPlan validationPlan=null;
		if (entry instanceof AssemblyInfoEntry|| entry instanceof ChromosomeEntry|| entry instanceof UnlocalisedEntry)
			validationPlan = new GenomeAssemblyValidationPlan(property);
		else
			validationPlan = new EmblEntryValidationPlan(property);
		validationPlan.addMessageBundle(ValidationMessageManager.STANDARD_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.STANDARD_FIXER_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_FIXER_BUNDLE);
		return validationPlan;
	}

	private List<ChromosomeEntry> getChromosomeEntries(File chromosomeFile, ValidationResult parseResult)throws IOException
	{
		if (chromosomeFile == null)
			return null;
		ChromosomeListFileReader reader = (ChromosomeListFileReader) GenomeAssemblyFileUtils.getFileReader(FileFormat.CHROMOSOME_LIST,chromosomeFile,null);
		parseResult = reader.read();
		if (reader.isEntry())
			return reader.getentries();
		return null;
	}

	private List<UnlocalisedEntry> getUnlocalisedEntries(File unlocalisedFile, ValidationResult parseResult)	throws IOException
	{
		if (unlocalisedFile == null)
			return null;
		UnlocalisedListFileReader reader = (UnlocalisedListFileReader) GenomeAssemblyFileUtils.getFileReader(FileFormat.UNLOCALISED_LIST,unlocalisedFile,null);
		parseResult = reader.read();
		if (reader.isEntry())
			return reader.getentries();
		return null;
	}

	private  ValidationPlanResult validateEntry(Object entry,EmblEntryValidationPlanProperty property)throws ValidationEngineException
	{
		ValidationPlan validationPlan = getValidationPlan(entry, property);
		return validationPlan.execute(entry);
	}

	private  List<String> getChromosomeEntryNames(boolean isVirus)
	{
		if (chromosomeEntries == null)
			return chromosomeEntryNames;

		for (ChromosomeEntry chromosomeEntry : chromosomeEntries)
		{
			chromosomeEntryNames.add(chromosomeEntry.getObjectName().toUpperCase());
			chromosomeQualifierMap.put(chromosomeEntry.getObjectName().toUpperCase(), GenomeAssemblyFileUtils.getChromosomeQualifier(chromosomeEntry, isVirus));
		}

		return chromosomeEntryNames;
	}

	private void defineFileTypes() throws ValidationEngineException, IOException {
		for (ManifestObj obj : manifestFileReader.getManifestFileObjects()) {
			String fileName = obj.getFileName();
			if(test)
				fileName = GenomeAssemblyFileUtils.getFile(fileName);
			File file = new File(fileName);
			switch (obj.getFileFormat()) {
				case INFO:
					assemblyInfoFile = file;
					break;
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

	private void moveFiles(boolean valid) throws IOException	{
		for(ManifestObj mo: manifestFileReader.getManifestFileObjects())	{
			String fileName=mo.getFileName();
			if(test)
				fileName=GenomeAssemblyFileUtils.getFile(mo.getFileName());
			switch(mo.getFileFormat()) {
				case FASTA:
				case FLATFILE:
				case AGP:
					if(valid)
						GenomeAssemblyFileUtils.replaceOriginalFile(fileName,fileName+".fixed", originalFileDir);
					else {
						File fixedFile= new File(mo.getFileName()+".fixed");
						if(fixedFile.exists())
						fixedFile.delete();
					}
					break;
				default:
					break;
			}
		}
	}

	@Override
	public void setOutputDir(String outputDir) {
		this.outputDir=outputDir;
	}
}