package uk.ac.ebi.ena.submit;

import java.util.Arrays;
import java.util.stream.Collectors;

import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCli;
import uk.ac.ebi.ena.assembly.SequenceAssemblyWebinCli;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.rawreads.RawReadsWebinCli;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;

public enum
ContextE
{
 sequence( "Sequence assembly: %s",
           "SEQUENCE_FLATFILE",
           new FileFormat[] { FileFormat.TAB,
                              FileFormat.FLATFILE, 
                              FileFormat.INFO },
           SequenceAssemblyWebinCli.class ),

 transcriptome( "Transcriptome assembly: %s",
                "TRANSCRIPTOME_ASSEMBLY",
                new FileFormat[] { FileFormat.FASTA, 
                                   FileFormat.FLATFILE, 
                                   FileFormat.INFO },
                TranscriptomeAssemblyWebinCli.class ),

 genome( "Genome assembly: %s",
         "SEQUENCE_ASSEMBLY",
         new FileFormat[] { FileFormat.FASTA, 
                            FileFormat.AGP, 
                            FileFormat.FLATFILE, 
                            FileFormat.CHROMOSOME_LIST, 
                            FileFormat.UNLOCALISED_LIST, 
                            FileFormat.INFO },
         GenomeAssemblyWebinCli.class ),
 
 reads( "Raw reads: %s",
        "RUN",
        new FileFormat[] {},
        RawReadsWebinCli.class );

    private String                            title;
    private String                            type;
    private FileFormat[]                      fileFormats;
    private Class<? extends AbstractWebinCli> klass;


    private ContextE( String title, 
                      String type, 
                      FileFormat[] fileFormats, 
                      Class<? extends AbstractWebinCli> klass )
    {
        this.title = title;
        this.type = type;
        this.fileFormats = fileFormats;
        this.klass = klass;
    }


    public Class<? extends AbstractWebinCli>
    getValidatorClass()
    {
        return this.klass;
    }


    public String
    getTitle( String name )
    {
        return String.format( this.title, name );
    }


    public String
    getType()
    {
        return type;
    }


    public FileFormat[]
    getFileFormats()
    {
        return fileFormats;
    }


    public String
    getFileFormatString()
    {
        return Arrays.asList( this.fileFormats ).stream().map( e -> String.valueOf( e ) ).collect( Collectors.joining( "," ) );
    }


    public static ContextE
    getContext( String context )
    {
        try
        {
            return ContextE.valueOf( context );
        
        } catch( Exception e )
        {
            return null;
        }
    }
}
