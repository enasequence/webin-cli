package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import uk.ac.ebi.embl.agp.reader.AGPFileReader;
import uk.ac.ebi.embl.agp.reader.AGPLineReader;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.EntryReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.manifest.ManifestObj;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public abstract class 
SequenceWebinCli extends AbstractWebinCli
{
    protected static String REPORT_FILE_SUFFIX = ".report";
    private final static String INVALID_INFO = "Info file validation failed. Please check the report file for errors: ";
    
    protected File   validationDir;
    protected File   submitDir;
    
    protected AssemblyInfoEntry assembly_info;
    protected File       infoFile;
    protected File       chromosomeListFile;
    protected File       unlocalisedListFile;
    protected List<File> fastaFiles;
    protected List<File> flatFiles;
    protected List<File> agpFiles;
    protected List<File> tsvFiles;
    private Study  study;
    private Sample sample;
    
    
    protected void 
    defineFileTypes( File manifest_file ) throws ValidationEngineException, IOException 
    {
        ManifestFileReader reader = new ManifestFileReader();
        reader.read( manifest_file.getPath() );

        List<File> fastaFiles = new ArrayList<>();
        List<File> flatFiles  = new ArrayList<>();
        List<File> agpFiles   = new ArrayList<>();
        List<File> tsvFiles   = new ArrayList<>();
        
        for( ManifestObj obj : reader.getManifestFileObjects() )
        {
            String fileName = obj.getFileName();
            File file = fileName.startsWith( "/" ) ? new File( fileName ) : new File( getParameters().getInputDir(), fileName );
            switch( obj.getFileFormat() ) 
            {
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
            case TSV:
                tsvFiles.add( file );
                break;
            case INFO:
                infoFile = file;
                break;                
            default:
                break;
            }
        }
        
        this.fastaFiles = fastaFiles;
        this.flatFiles  = flatFiles;
        this.agpFiles   = agpFiles;
        this.tsvFiles   = tsvFiles;
        
        
    }

    
    
    
    protected AssemblyInfoEntry
    defineInfo( File info_file ) throws IOException, ValidationEngineException
    {
        InfoFileValidator infoValidator = new InfoFileValidator();
        if( !infoValidator.validate( info_file, getParameters().getOutputDir().getPath(), getContext() ) )
            throw WebinCliException.createUserError( INVALID_INFO, infoValidator.getReportFile().getAbsolutePath() );
        return infoValidator.getentry();
    }
    
    
    private File 
    createValidateDir( File outputDir, String context, String name ) throws Exception 
    {
        File reportDirectory = new File( outputDir, new File( context , new File( name , VALIDATE_DIR ).getPath() ).getPath() );
        
        if( reportDirectory.exists() )
        {
            FileUtils.emptyDirectory( reportDirectory );
        } else if( !reportDirectory.mkdirs() ) 
        {
            throw WebinCliException.createSystemError( "Unable to create directory: " + reportDirectory.getPath() );
        }
        
        return reportDirectory;
    }

    
    abstract ContextE getContext();
    abstract boolean  getTestMode();
    
    
    @Override public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        try
        {
            super.init( parameters );

            defineFileTypes( getParameters().getManifestFile() );
            setAssemblyInfo( defineInfo( infoFile ) );
            setName( getAssemblyInfo().getName().trim().replaceAll( "\\s+", "_" ) );
            
            setValidationDir( createOutputDir( String.valueOf( getContext() ), getName(), VALIDATE_DIR ) );
            setSubmitDir( createOutputDir( String.valueOf( getContext() ), getName(), SUBMIT_DIR ) );
            
            setSample( fetchSample( getAssemblyInfo().getSampleId(), getTestMode() ) );
            setStudy( fetchStudy( getAssemblyInfo().getStudyId(), getTestMode() ) );

        } catch( Throwable t )
        {
            throw new ValidationEngineException( "Unable to init validator", t );
        }
    }
    
    
    
    protected void
    setStudy( Study study )
    { 
        this.study = study;
    }




    protected void
    setSample( Sample sample )
    {
        this.sample = sample;
    }


    protected Study
    getStudy()
    { 
        return this.study;
    }


    protected Sample
    getSample()
    {
        return this.sample;
    }
    

    public void 
    setReportsDir( String reportDir )
    {
        this.validationDir = new File( reportDir );
    }
       
    
    protected File
    getReportFile( FileFormat filetype, String filename )
    {
        if( null == validationDir )
            throw new RuntimeException( "Validation dir cannot be null" );
        
        if( validationDir.isFile() )
            throw new RuntimeException( "Validation dir cannot be file" );
        
        return new File( this.validationDir, /*filetype + "-" +*/ filename + REPORT_FILE_SUFFIX ); 
    }

    
    @SuppressWarnings( "unchecked" ) protected <T> T
    getFileReader( FileFormat format, File file ) throws IOException
    {
         if( !EntryReader.getBlockCounter().isEmpty() )
             EntryReader.getBlockCounter().clear();
         
          if( !EntryReader.getSkipTagCounter().isEmpty() )
              EntryReader.getSkipTagCounter().clear();
          
        switch( format )
        {
        default:
            return null;
        
        case FASTA:
            return (T) new FastaFileReader( new FastaLineReader( FileUtils.getBufferedReader( file ) ) );

        case AGP:
            return (T) new AGPFileReader( new AGPLineReader( FileUtils.getBufferedReader( file ) ) );
        
        case FLATFILE:
            EmblEntryReader emblReader = new EmblEntryReader( FileUtils.getBufferedReader( file ), EmblEntryReader.Format.EMBL_FORMAT, file.getName() );
            emblReader.setCheckBlockCounts( true );
            return (T) emblReader;

        case INFO:
            return (T) new AssemblyInfoReader( file );

        case CHROMOSOME_LIST:
            return (T) new ChromosomeListFileReader( file );
            
        case UNLOCALISED_LIST:
            return (T) new UnlocalisedListFileReader( file );
        }
    }


    public File
    getValidationDir()
    {
        return validationDir;
    }


    public File
    getSubmitDir()
    {
        return submitDir;
    }


    public void
    setValidationDir( File validationDir )
    {
        this.validationDir = validationDir;
    }


    public void
    setSubmitDir( File submitDir )
    {
        this.submitDir = submitDir;
    }


    public void 
    setInputDir( File inputDir )
    {
        getParameters().setInputDir( inputDir );
    }


    public File
    getInputDir()
    {
        return getParameters().getInputDir();
    }
    
    
    public AssemblyInfoEntry
    getAssemblyInfo()
    {
        return assembly_info;
    }




    public void
    setAssemblyInfo( AssemblyInfoEntry assembly_info )
    {
        this.assembly_info = assembly_info;
    }

}
