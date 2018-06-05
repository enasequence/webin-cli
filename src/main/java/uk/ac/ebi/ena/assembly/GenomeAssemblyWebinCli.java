package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

import org.jdom2.Element;

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
import uk.ac.ebi.embl.api.entry.qualifier.QualifierFactory;
import uk.ac.ebi.embl.api.validation.FileType;
import uk.ac.ebi.embl.api.validation.SequenceEntryUtils;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessage;
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
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class 
GenomeAssemblyWebinCli extends SequenceWebinCli 
{
	private List<String> chromosomeEntryNames = new ArrayList<String>();
	private List<ChromosomeEntry> chromosomeEntries = null;
	private HashMap<String,Long> fastaEntryNames = new HashMap<String,Long>();
	private HashMap<String,Long> flatfileEntryNames = new HashMap<String,Long>();
	private HashSet<String> agpEntrynames = new HashSet<String>();
	private HashMap<String, List<Qualifier>> chromosomeQualifierMap = new HashMap<String, List<Qualifier>>();
	private String sequencelessChromosomesCheck= "ChromosomeListSequenelessCheck";
	private String molType = "genomic DNA";
	private boolean test;
    private boolean valid;

	
    public 
    GenomeAssemblyWebinCli()
    {
        this( false );
    }

	
	public 
	GenomeAssemblyWebinCli( boolean test_mode )
	{
	    this.test = test_mode;
	}
	
	
	@Override public void
	init( WebinCliParameters parameters ) throws ValidationEngineException
	{
	    super.init( parameters );
	}
	
	
	void 
	__init( ManifestFileReader manifestFileReader, 
	        Sample sample, 
	        Study study, 
	        String molType )
	{
//		this.manifestFileReader = manifestFileReader;
		setSample( sample );
		setStudy( study );
		this.molType = molType == null ? this.molType : molType;
	}

	
	void
	__init( ManifestFileReader manifestFileReader, 
	        Sample sample, 
	        Study study, 
	        String molType,
	        boolean test )
	{
		__init( manifestFileReader, sample, study, molType );
		this.test = test;
	}

	
    @Override public boolean 
    validateInternal() throws ValidationEngineException 
    {
		boolean valid = true;
		try 
		{
            EmblEntryValidationPlanProperty property = getValidationProperties();
            
			TaxonHelper taxonHelper = new TaxonHelperImpl();

			valid = valid && validateChromosomeList( property, chromosomeListFile );
			valid = valid && validateUnlocalisedList( property );
			getChromosomeEntryNames( taxonHelper.isChildOf( getSample().getOrganism(), "Virus" ) );
		    valid = valid && readAGPfiles();
			valid = valid && validateFastaFiles( property, fastaFiles );
			valid = valid && validateFlatFiles( property );
			
			HashMap<String,Long> entryNames= new HashMap<String,Long>();
			entryNames.putAll( fastaEntryNames );
			entryNames.putAll( flatfileEntryNames );
    		
			property.contigEntryNames.set( entryNames );
			
			valid = valid && validateAgpFiles( property );
		    valid = valid && validateSequenceLessChromosomes( chromosomeListFile );

			if( !test )
				deleteFixedFiles( valid );
			
			this.valid = valid; 
			    
			return valid;
		} catch (IOException e) 
		{
			throw new ValidationEngineException(e.getMessage());
		}

	}


    @Override public SubmissionBundle
    getSubmissionBundle()
    {
        if( valid )
            prepareSubmissionBundle();
        return super.getSubmissionBundle();
    }
    
    
    EmblEntryValidationPlanProperty 
    getValidationProperties() 
    {
        EmblEntryValidationPlanProperty property = new EmblEntryValidationPlanProperty();
        
        property.isFixMode.set( true );
        property.isRemote.set( true );
        property.locus_tag_prefixes.set( getStudy().getLocusTagsList() );
        return property;
    }
	   
	
	boolean 
    validateChromosomeList( EmblEntryValidationPlanProperty property, File chromosomeListFile ) throws IOException, ValidationEngineException 
    {
        if( chromosomeListFile == null )
            return true;
        
        ValidationResult parseResult = new ValidationResult();

        File f = getReportFile( FileFormat.CHROMOSOME_LIST, chromosomeListFile.getName() );
        
        chromosomeEntries = getChromosomeEntries( chromosomeListFile, parseResult );
        FileUtils.writeReport( f, parseResult, chromosomeListFile.getName() );
        
        if( null == chromosomeEntries || chromosomeEntries.isEmpty() )
        {
            FileUtils.writeReport( f, Severity.ERROR, "File " + chromosomeListFile.getPath() + " has no valid chromosome entries" );
            return false;
        }

        if( !parseResult.isValid() )
        {
            FileUtils.writeReport( f, parseResult, chromosomeListFile.getName() );
            return false;
        }
        
        boolean valid = true;
        if( chromosomeEntries != null )
        {
            property.fileType.set( FileType.CHROMOSOMELIST );
            
            for( ChromosomeEntry chromosomeEntry : chromosomeEntries )
            {
                ValidationPlanResult vpr = validateEntry( chromosomeEntry, property );
                valid &= vpr.isValid();
                FileUtils.writeReport( f, vpr.getMessages(), chromosomeListFile.getName() );
            }
        }
        return valid;
    }


	private boolean 
    validateUnlocalisedList( EmblEntryValidationPlanProperty property ) throws IOException, ValidationEngineException
    {
        if( unlocalisedListFile == null )
            return true;
        
        ValidationResult parseResult = new ValidationResult();
        List<UnlocalisedEntry> unlocalisedEntries = getUnlocalisedEntries( unlocalisedListFile, parseResult );
        File f = getReportFile( FileFormat.UNLOCALISED_LIST, unlocalisedListFile.getName() );
        boolean valid = parseResult.isValid();
        FileUtils.writeReport( f, parseResult, unlocalisedListFile.getName() );
        
        if( parseResult.isValid() )
        {
            if( unlocalisedEntries != null ) 
            {
                property.fileType.set( FileType.UNLOCALISEDLIST );
                for( UnlocalisedEntry unlocalisedEntry : unlocalisedEntries ) 
                {
                    ValidationPlanResult vpr = validateEntry( unlocalisedEntry, property );
                    valid &= vpr.isValid();
                    FileUtils.writeReport( f, vpr.getMessages(), unlocalisedListFile.getName() );
                }
            }
        }           
        
        return valid;
    }
	

	boolean 
    validateFastaFiles( EmblEntryValidationPlanProperty property, List<File> fastaFiles ) throws IOException, ValidationEngineException 
    {
        boolean valid = true;
        boolean valid_entries = false;
        
        for( File file : fastaFiles ) 
        {
            property.fileType.set( FileType.FASTA );
            File f = getReportFile( FileFormat.FASTA, file.getName() ); 
            FlatFileReader<?> reader = getFileReader( FileFormat.FASTA, file );
            ValidationResult parseResult = reader.read();
            valid &= parseResult.isValid();
            FileUtils.writeReport( f, parseResult, file.getName() );
            if( parseResult.isValid() )
            {
                while( reader.isEntry() )
                {
                    SourceFeature source = ( new FeatureFactory() ).createSourceFeature();
                    source.setScientificName( getSample().getOrganism() );
                    source.addQualifier( Qualifier.MOL_TYPE_QUALIFIER_NAME, molType );
                    
                    Entry entry = (Entry) reader.getEntry();
                    if( getSample().getBiosampleId() != null )
                        entry.addXRef( new XRef( "BioSample", getSample().getBiosampleId() ) );
                    
                    if( getStudy().getProjectId() != null )
                        entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
                    
                    if( entry.getSubmitterAccession() != null && chromosomeEntryNames.contains( entry.getSubmitterAccession().toUpperCase() ) ) 
                    {
                        List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get( entry.getSubmitterAccession().toUpperCase() );
                        source.addQualifiers( chromosomeQualifiers );
                        property.validationScope.set( ValidationScope.ASSEMBLY_CHROMOSOME );
                        entry.setDataClass( Entry.STD_DATACLASS );
                    } else 
                    {
                        property.validationScope.set( ValidationScope.ASSEMBLY_CONTIG );
                        entry.setDataClass( Entry.WGS_DATACLASS );
                    }

                    Order<Location> featureLocation = new Order<Location>();
                    featureLocation.addLocation( new LocationFactory().createLocalRange(1l, entry.getSequence().getLength() ) );
                    source.setLocations( featureLocation);
                    entry.addFeature( source);
                    entry.getSequence().setMoleculeType( molType );

					ValidationPlanResult vpr = getValidationPlan( entry, property ).execute( entry );
					valid &= vpr.isValid();
		            FileUtils.writeReport( f, vpr.getMessages(), file.getName() );
		            
					fastaEntryNames.put( entry.getSubmitterAccession().toUpperCase(), entry.getSequence().getLength() );
                    valid_entries = true;
                    parseResult = reader.read();
                }

                if( !valid_entries )
                {
                    valid &= false;
                    FileUtils.writeReport( f, Severity.ERROR, "File " + file.getName() + " does not contain valid FASTA entries" );
                }   
            }
        }
        return valid;
    }
	
	
	private boolean 
	readAGPfiles() throws IOException
	{
		boolean valid = true;
		
	    for( File file : agpFiles ) 
        {
           FlatFileReader<?> reader = getFileReader( FileFormat.AGP, file );
            
            ValidationResult vr = reader.read();
            FileUtils.writeReport( getReportFile(  FileFormat.AGP, file.getName() ), vr );
            
            while( ( valid &= vr.isValid() ) && reader.isEntry() )
            {
                agpEntrynames.add( ( (Entry) reader.getEntry() ).getSubmitterAccession().toUpperCase() );
                vr = reader.read();
            }
            
        }
	    return valid;
	}
      

    private boolean 
    validateFlatFiles( EmblEntryValidationPlanProperty property ) throws ValidationEngineException, IOException 
    {
        boolean valid = true;
        for( File file : flatFiles ) 
        {
            property.fileType.set( FileType.EMBL );
            File f = getReportFile( FileFormat.FLATFILE, file.getName() );
            
            FlatFileReader<?> reader = getFileReader( FileFormat.FLATFILE, file );
            ValidationResult parseResult = reader.read();
            valid &= parseResult.isValid();
            FileUtils.writeReport( f, parseResult, file.getName() );
            
            if( parseResult.isValid() )
            {
                while( reader.isEntry() ) 
                {
                    Entry entry = (Entry) reader.getEntry();
                    if(entry.getSubmitterAccession()!=null)
                    flatfileEntryNames.put(entry.getSubmitterAccession().toUpperCase(),entry.getSequence().getLength());
                    entry.removeFeature( entry.getPrimarySourceFeature() );
                    SourceFeature source = ( new FeatureFactory() ).createSourceFeature();
                    source.addQualifier( Qualifier.MOL_TYPE_QUALIFIER_NAME, molType );
                    source.setScientificName( getSample().getOrganism() );
                    
                    if( getSample().getBiosampleId() != null )
                        entry.addXRef( new XRef( "BioSample", getSample().getBiosampleId() ) );
                    
                    if( getStudy().getProjectId() != null )
                        entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
                    
                    if( entry.getSubmitterAccession() != null && chromosomeEntryNames.contains( entry.getSubmitterAccession().toUpperCase() ) ) 
                    {
                        List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get( entry.getSubmitterAccession().toUpperCase() );
                        source.addQualifiers( chromosomeQualifiers );
                        property.validationScope.set( ValidationScope.ASSEMBLY_CHROMOSOME );
                        if( entry.getSubmitterAccession() != null && agpEntrynames.contains( entry.getSubmitterAccession().toUpperCase() ) )
                        {
                            entry.setDataClass( Entry.CON_DATACLASS );
                        } else
                        {
                            entry.setDataClass( Entry.STD_DATACLASS );
                        }
                    } else if( entry.getSubmitterAccession() != null && agpEntrynames.contains( entry.getSubmitterAccession().toUpperCase() ) ) 
                    {
                        entry.setDataClass( Entry.CON_DATACLASS );
                        property.validationScope.set( ValidationScope.ASSEMBLY_SCAFFOLD );
                    } else 
                    {
                        property.validationScope.set( ValidationScope.ASSEMBLY_CONTIG );
                        entry.setDataClass( Entry.WGS_DATACLASS );
                    }
                    Order<Location> featureLocation = new Order<Location>();
                    featureLocation.addLocation( new LocationFactory().createLocalRange( 1l, entry.getSequence().getLength() ) );
                    source.setLocations( featureLocation );
                    entry.addFeature( source );
                    entry.getSequence().setMoleculeType( molType );

                    ValidationPlanResult validationPlanResult = getValidationPlan( entry, property ).execute( entry );
                    valid &= validationPlanResult.isValid();
                    FileUtils.writeReport( f, validationPlanResult.getMessages(), file.getName() );

                    parseResult = reader.read();
                    if( !parseResult.isValid() )
                    {
                        valid = false;
                        FileUtils.writeReport( f, parseResult, file.getName() );
                    }
                }
            }
        }
        return valid;
    }


    private boolean 
    validateAgpFiles( EmblEntryValidationPlanProperty property ) throws IOException, ValidationEngineException 
    {
        boolean valid = true;

        for( File file : agpFiles ) 
        {
            property.fileType.set( FileType.AGP );
            File f = getReportFile( FileFormat.AGP, file.getName() );
            
            FlatFileReader<?> reader = getFileReader( FileFormat.AGP, file );
            
            ValidationResult parseResult = reader.read();
            valid &= parseResult.isValid();
            if( !valid ) //TODO: discuss file format errors, AgpFileReader.isEntry
            {
                FileUtils.writeReport( f, parseResult, file.getName() );
            } else
            {
                while( reader.isEntry() ) 
                {
                    SourceFeature source = ( new FeatureFactory() ).createSourceFeature();
                    source.setScientificName( getSample().getOrganism() );
                    source.addQualifier( Qualifier.MOL_TYPE_QUALIFIER_NAME, molType );
                    Entry entry = (Entry) reader.getEntry();
                    if( getSample().getBiosampleId() != null )
                        entry.addXRef( new XRef( "BioSample", getSample().getBiosampleId() ) );
                    
                    if( getStudy().getProjectId() != null )
                        entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
                    
                    entry.setDataClass( Entry.CON_DATACLASS );
                    if( entry.getSubmitterAccession() != null && chromosomeEntryNames.contains( entry.getSubmitterAccession().toUpperCase() ) ) 
                    {
                        List<Qualifier> chromosomeQualifiers = chromosomeQualifierMap.get( entry.getSubmitterAccession().toUpperCase() );
                        source.addQualifiers( chromosomeQualifiers );
                        property.validationScope.set( ValidationScope.ASSEMBLY_CHROMOSOME );
                    } else
                    {
                        property.validationScope.set( ValidationScope.ASSEMBLY_SCAFFOLD );
                    }
                    
                    Order<Location> featureLocation = new Order<Location>();
                    featureLocation.addLocation( new LocationFactory().createLocalRange( 1l, entry.getSequence().getLength() ) );
                    source.setLocations( featureLocation );
                    entry.addFeature( source );
                    entry.getSequence().setMoleculeType( molType );
                    ValidationPlanResult vpr = getValidationPlan( entry, property ).execute( entry );
                    valid &= vpr.isValid();
                    FileUtils.writeReport( f, vpr.getMessages(), file.getName() );
                    parseResult = reader.read();
                }
            }
        }
        return valid;
    }

    
    private boolean 
    validateSequenceLessChromosomes( File chromosomeListFile )
    {
    	List<String> sequencelessChromosomes = new ArrayList<String>();
    	for( String chromosome : chromosomeEntryNames )
    	{
    		if( agpEntrynames.contains( chromosome ) )//IWGSC_CSS_6DL_scaff_3330716
    			continue;
    
    		if( fastaEntryNames.containsKey( chromosome ) )//IWGSC_CSS_6DL_SCAFF_3330716
    			continue;
            
    		if( flatfileEntryNames.containsKey( chromosome ) )
            	continue;
            else
            	sequencelessChromosomes.add( chromosome );
      	}
    	
    	if( !sequencelessChromosomes.isEmpty() )
    	{    			
    	    File f = getReportFile( FileFormat.CHROMOSOME_LIST, chromosomeListFile.getName() );
    	    ValidationResult result = new ValidationResult();
            result.append( new ValidationResult().append( new ValidationMessage<>( Severity.ERROR, sequencelessChromosomesCheck, sequencelessChromosomes.stream().collect( Collectors.joining( "," ) ) ) ) );
            FileUtils.writeReport( f, result.getMessages(), chromosomeListFile.getName() );
            return false;
    	}
    	return true;
    }
    
	public ValidationPlan getValidationPlan(Object entry, EmblEntryValidationPlanProperty property) 
	{
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

	private List<ChromosomeEntry> getChromosomeEntries(File chromosomeFile, ValidationResult parseResult)
			throws IOException 
	{
		if (chromosomeFile == null)
			return null;
		ChromosomeListFileReader reader = (ChromosomeListFileReader)getFileReader(FileFormat.CHROMOSOME_LIST, chromosomeFile);
		parseResult.append(reader.read());
		if (reader.isEntry())
			return reader.getentries();
		return null;
	}

	private List<UnlocalisedEntry> getUnlocalisedEntries(File unlocalisedFile, ValidationResult parseResult)
			throws IOException 
	{
		if (unlocalisedFile == null)
			return null;
		UnlocalisedListFileReader reader = (UnlocalisedListFileReader)getFileReader(FileFormat.UNLOCALISED_LIST, unlocalisedFile);
		parseResult.append(reader.read());
		if (reader.isEntry())
			return reader.getentries();
		return null;
	}

	private ValidationPlanResult validateEntry(Object entry, EmblEntryValidationPlanProperty property)
			throws ValidationEngineException 
	{
		ValidationPlan validationPlan = getValidationPlan(entry, property);
		return validationPlan.execute(entry);
	}

	private List<String> getChromosomeEntryNames(boolean isVirus) 
	{
		if (chromosomeEntries == null)
			return chromosomeEntryNames;

		for (ChromosomeEntry chromosomeEntry : chromosomeEntries) 
		{
			chromosomeEntryNames.add(chromosomeEntry.getObjectName().toUpperCase());
			chromosomeQualifierMap.put(chromosomeEntry.getObjectName().toUpperCase(),
					getChromosomeQualifier(chromosomeEntry, isVirus));
		}

		return chromosomeEntryNames;
	}


	private void 
	deleteFixedFiles( boolean valid ) throws IOException
	{
        List<File> files = new ArrayList<>();
        files.addAll( fastaFiles );
        files.addAll( flatFiles );
        files.addAll( agpFiles );
        
	    for( File f : files )
	    {
            if( !valid ) 
            {
                File fixedFile = new File( f.getPath() + ".fixed" );
                if( fixedFile.exists() )
                    fixedFile.delete();
            }
	    }
	}


    @Override ContextE
    getContext()
    {
        return ContextE.genome;
    }


    @Override boolean 
    getTestMode()
    {
        return test;
    }
    
    public static List<Qualifier> getChromosomeQualifier(ChromosomeEntry entry,boolean isVirus)
	{
		String chromosomeType = entry.getChromosomeType();
		String chromosomeLocation = entry.getChromosomeLocation();
		String chromosomeName = entry.getChromosomeName();
		List<Qualifier> chromosomeQualifiers = new ArrayList<Qualifier>();
		
		if (chromosomeLocation != null && !chromosomeLocation.isEmpty()&& !isVirus&&!chromosomeLocation.equalsIgnoreCase("Phage"))
		{
			String organelleValue =  SequenceEntryUtils.getOrganelleValue(chromosomeLocation);
			if (organelleValue != null)
			{									
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.ORGANELLE_QUALIFIER_NAME, SequenceEntryUtils.getOrganelleValue(chromosomeLocation)));
			}
		}	
		else if (chromosomeName != null && !chromosomeName.isEmpty())
		{
			if (Qualifier.PLASMID_QUALIFIER_NAME.equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.PLASMID_QUALIFIER_NAME, chromosomeName));
			}
			else if (Qualifier.CHROMOSOME_QUALIFIER_NAME.equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.CHROMOSOME_QUALIFIER_NAME, chromosomeName));
			}
			else if("segmented".equals(chromosomeType)||"multipartite".equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.SEGMENT_QUALIFIER_NAME, chromosomeName));

			}
			else if("monopartite".equals(chromosomeType))
			{
				chromosomeQualifiers.add((new QualifierFactory()).createQualifier(Qualifier.NOTE_QUALIFIER_NAME, chromosomeType));
			}
		}
		return chromosomeQualifiers;
	}
}
