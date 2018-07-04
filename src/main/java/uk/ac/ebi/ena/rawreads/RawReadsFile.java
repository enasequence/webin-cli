package uk.ac.ebi.ena.rawreads;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.jdom2.Element;

public class 
RawReadsFile
{
    public enum 
    Filetype
    {
        fastq( "fastq" ),
        bam( "bam", ".bam" ),
        cram( "cram", ".cram" ),
        info( "info file" );
        
        
        public final String xml_name;
        public final String[] permitted_suffixes;
        Filetype( String xml_name, String... permitted_suffixes )
        {
            this.xml_name = xml_name;
            this.permitted_suffixes = permitted_suffixes;
        }
        
    }
    
    
    public enum
    ChecksumMethod
    {
        MD5( "MD5" ),
        SHA256( "SHA-256" );
        
        public final String xml_name; 
        ChecksumMethod( String xml_name )
        {
            this.xml_name = xml_name;
        } 
    }
    
    
    public enum
    QualityScoringSystem
    {
        phred( "phred" ), 
        log_odds( "log-odds" );
        
        public final String xml_name;
        QualityScoringSystem( String xml_name )
        {
            this.xml_name = xml_name;
        }
    }
    

    public enum
    AsciiOffset
    {
        FROM33( "!" ),
        FROM64( "@" );

        public final String xml_name;
        AsciiOffset( String xml_name )
        {
            this.xml_name = xml_name;
        }
    }
    
    
    public enum
    QualityEncoding
    {
        ascii( "ascii" );
        
        public final String xml_name;
        QualityEncoding( String xml_name )
        {
            this.xml_name = xml_name;
        }
    }
    
    
    public enum
    Compression
    {
        NONE,
        GZ, 
        GZIP, 
        BZ2,
        ZIP;
    }
    
    
    private Compression          compression;
    private String               filename; 
    private Filetype             filetype; 
    private String               checksum; 
    private ChecksumMethod       checksum_method; 
    private QualityScoringSystem quality_scoring_system; 
    private AsciiOffset          ascii_offset; 
    private QualityEncoding      quality_encoding;
    private Path                 inputDir;
    
    public Element
    toElement( String element_name, Path uploadDir )
    {
        Element e = new Element( element_name );
        e.setAttribute( "filename", uploadDir.resolve( inputDir.relativize( Paths.get( filename ) ) ).toString() /*.replaceAll( "\\\\+", "/" )*/ );
        e.setAttribute( "filetype", filetype.xml_name );
        e.setAttribute( "checksum", checksum );
        e.setAttribute( "checksum_method", checksum_method.xml_name );
        if( null != quality_scoring_system ) 
            e.setAttribute( "quality_scoring_system", quality_scoring_system.xml_name );
        
        if( null != ascii_offset )
            e.setAttribute( "ascii_offset", ascii_offset.xml_name );
        
        if( null != quality_encoding )
            e.setAttribute( "quality_encoding", quality_encoding.xml_name );
 
        return e;
    }
    
    
    @Override public String 
    toString()
    {
        return String.format( "filename=\"%s\" filetype=\"%s\"%s%s%s%s%s",
                              filename,
                              filetype.xml_name,
                              null == checksum ?               "" : String.format( " checksum=\"%s\"", checksum ),
                              null == checksum_method ?        "" : String.format( " checksum_method=\"%s\"", checksum_method ),
                              null == quality_scoring_system ? "" : String.format( " quality_scoring_system=\"%s\"", quality_scoring_system.xml_name ),
                              null == ascii_offset           ? "" : String.format( " ascii_offset=\"%s\"", ascii_offset.xml_name ),
                              null == quality_encoding       ? "" : String.format( " quality_encoding=\"%s\"", quality_encoding.xml_name )
                            );
    }


    public String
    getFilename()
    {
        return filename;
    }


    public Filetype
    getFiletype()
    {
        return filetype;
    }


    public String
    getChecksum()
    {
        return checksum;
    }


    public ChecksumMethod
    getChecksumMethod()
    {
        return checksum_method;
    }


    public QualityScoringSystem
    getQualityScoringSystem()
    {
        return quality_scoring_system;
    }


    public AsciiOffset
    getAsciiOffset()
    {
        return ascii_offset;
    }


    public QualityEncoding
    getQualityEncoding()
    {
        return quality_encoding;
    }


    public void
    setFilename( String filename )
    {
        this.filename = filename;
    }


    public void
    setFiletype( Filetype filetype )
    {
        this.filetype = filetype;
    }


    public void
    setChecksum( String checksum )
    {
        this.checksum = checksum;
    }


    public void
    setChecksumMethod( ChecksumMethod checksum_method )
    {
        this.checksum_method = checksum_method;
    }


    public void
    setQualityScoringSystem( QualityScoringSystem quality_scoring_system )
    {
        this.quality_scoring_system = quality_scoring_system;
    }


    public void
    setAsciiOffset( AsciiOffset ascii_offset )
    {
        this.ascii_offset = ascii_offset;
    }


    public void
    setQualityEncoding( QualityEncoding quality_encoding )
    {
        this.quality_encoding = quality_encoding;
    }


    public Path
    getInputDir()
    {
        return inputDir;
    }


    public void
    setInputDir( Path inputDir )
    {
        this.inputDir = inputDir;
    }


    public Compression
    getCompression()
    {
        return compression;
    }


    public void
    setCompression( Compression compression )
    {
        this.compression = compression;
    }
}
