package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;

import uk.ac.ebi.embl.agp.reader.AGPFileReader;
import uk.ac.ebi.embl.agp.reader.AGPLineReader;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.EntryReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.ChromosomeListFileReader;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.UnlocalisedListFileReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;

public abstract class 
SequenceWebinCli extends AbstractWebinCli
{
    protected static String REPORT_FILE_SUFFIX = ".report";
    
    protected File   validationDir;
    protected File   submitDir;
    
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
        setValidationDir(
                          File validationDir )
    {
        this.validationDir = validationDir;
    }


    public void
        setSubmitDir(
                      File submitDir )
    {
        this.submitDir = submitDir;
    }

}
