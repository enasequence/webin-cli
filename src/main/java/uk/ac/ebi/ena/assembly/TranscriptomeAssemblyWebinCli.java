package uk.ac.ebi.ena.assembly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.jdom2.Element;

import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.entry.Text;
import uk.ac.ebi.embl.api.entry.XRef;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
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
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
TranscriptomeAssemblyWebinCli extends SequenceWebinCli 
{
	private static final String VALIDATION_MESSAGES_BUNDLE = "ValidationSequenceMessages";
	private static final String STANDARD_VALIDATION_BUNDLE = "uk.ac.ebi.embl.api.validation.ValidationMessages";
	private static final String STANDARD_FIXER_BUNDLE = "uk.ac.ebi.embl.api.validation.FixerMessages";
	private final String MOL_TYPE = "transcribed RNA";
	private File reportFile;
	private String submittedFile;
	private ValidationPlan validationPlan;
	private boolean FLAILED_VALIDATION;
	private ManifestFileReader manifestFileReader;
	private StringBuilder resultsSb;

	public 
	TranscriptomeAssemblyWebinCli()
	{
	}

	
	private ValidationPlan
	getValidationPlan()
	{
	    ValidationPlan validationPlan;
	    EmblEntryValidationPlanProperty emblEntryValidationProperty = new EmblEntryValidationPlanProperty();
	    emblEntryValidationProperty.validationScope.set( ValidationScope.ASSEMBLY_TRANSCRIPTOME );
	    emblEntryValidationProperty.isDevMode.set( false );
	    emblEntryValidationProperty.isFixMode.set( true );
	    emblEntryValidationProperty.isAssembly.set( false );
	    emblEntryValidationProperty.minGapLength.set( 0 );
	    emblEntryValidationProperty.locus_tag_prefixes.set( getStudy().getLocusTagsList() );

	    validationPlan = new EmblEntryValidationPlan( emblEntryValidationProperty );
	    validationPlan.addMessageBundle( VALIDATION_MESSAGES_BUNDLE );
	    validationPlan.addMessageBundle( STANDARD_VALIDATION_BUNDLE );
	    validationPlan.addMessageBundle( STANDARD_FIXER_BUNDLE );
	    return validationPlan;
	}

	
	@Override public void
	init( WebinCliParameters parameters ) throws ValidationEngineException
	{
	    super.init( parameters );
	    this.validationPlan = getValidationPlan();
	}
		
	
	@Deprecated public 
	TranscriptomeAssemblyWebinCli(ManifestFileReader manifestFileReader, Sample sample, Study study) throws ValidationEngineException {
		this.manifestFileReader = manifestFileReader;
		setSample( sample );
		setStudy( study );
		this.validationPlan = getValidationPlan();
		flatFiles  = null == manifestFileReader.getFilenameFromManifest( FileFormat.FLATFILE ) ? Collections.emptyList() 
		                                                                                       : Arrays.asList( new File( manifestFileReader.getFilenameFromManifest(FileFormat.FLATFILE ) ) );
        fastaFiles = null == manifestFileReader.getFilenameFromManifest( FileFormat.FASTA )    ? Collections.emptyList() 
                                                                                               : Arrays.asList( new File( manifestFileReader.getFilenameFromManifest(FileFormat.FASTA ) ) );  
	}

	public StringBuilder validateTestFlatfile(String submittedFile) throws ValidationEngineException {
		resultsSb = new StringBuilder();
		setTestMode( true );
		this.submittedFile = submittedFile;
		validateFlatFile();
		return resultsSb;
	}

	
	@Override protected boolean 
	validateInternal() throws ValidationEngineException 
	{
		if( !flatFiles.isEmpty() )
		{
		    submittedFile = flatFiles.get( 0 ).getPath();
		    reportFile = getReportFile( FileFormat.FLATFILE, submittedFile );
			validateFlatFile();
		} else if( !fastaFiles.isEmpty() )
		{   submittedFile = fastaFiles.get( 0 ).getPath();
		    reportFile = getReportFile( FileFormat.FASTA, submittedFile );
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
				entry.getPrimarySourceFeature().setScientificName( getSample().getOrganism());
				if( getSample().getBiosampleId()!=null)
				entry.addXRef(new XRef("BioSample", getSample().getBiosampleId()));
				if( getStudy().getProjectId() != null )
					 entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
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
				sourceFeature.setScientificName( getSample().getOrganism() );
				
				if( getSample().getBiosampleId() != null )
					entry.addXRef( new XRef( "BioSample", getSample().getBiosampleId() ) );
				
				if( getStudy().getProjectId() != null )
                    entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
				
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

    
    @Override public SubmissionBundle
    getSubmissionBundle()
    {
        if( !FLAILED_VALIDATION )
            prepareSubmissionBundle();
        return super.getSubmissionBundle();
    }
    
    
	@Override ContextE 
	getContext()
	{
		return ContextE.transcriptome;
	}
	
	
    protected List<File>
    getUploadFiles() throws IOException
    {
        List<File> uploadFileList = super.getUploadFiles();
        infoFile = FileUtils.gZipFile( infoFile );
        uploadFileList.add( infoFile );
        return uploadFileList;
    }
    
    
    protected List<Element>
    getXMLFiles( Path uploadDir ) throws IOException
    {
        List<Element> eList = super.getXMLFiles( uploadDir );
        infoFile = FileUtils.gZipFile( infoFile );
        eList.add( createfileElement( uploadDir, infoFile, "info" ) );
        return eList;
    }

    
    Element 
    makeAnalysisType( AssemblyInfoEntry entry )
    {
        Element typeE = new Element( ContextE.transcriptome.getType() );
        return typeE;
    }
}
