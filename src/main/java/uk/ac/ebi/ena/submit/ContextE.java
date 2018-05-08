package uk.ac.ebi.ena.submit;

import java.util.Arrays;

import org.apache.commons.lang3.StringUtils;

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
    		  new FileFormat[] { FileFormat.TSV, 
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
		   "RUN_PROCESS",
		   new FileFormat[] {  },
		   RawReadsWebinCli.class );
	
    private String analysisTitle;
    private String analysisType;
    private FileFormat[] fileFormats;
    private Class<? extends AbstractWebinCli> klass;
    
    private 
    ContextE( String analysisTitle, 
    		  String analysisType, 
    		  FileFormat[] fileFormats,
    		  Class<? extends AbstractWebinCli> klass ) 
    {
        this.analysisTitle = analysisTitle;
        this.analysisType  = analysisType;
        this.fileFormats   = fileFormats;
        this.klass         = klass;
    }

    
    public Class<? extends AbstractWebinCli>
    getValidatorClass()
    {
    	return this.klass;
    }

    
    public String 
    getAnalysisTitle( String name )
    {
        return analysisTitle.replace( "ASSEMBLYNAME", name );
    }

    public String getAnalysisType() {
        return analysisType;
    }
    
    public FileFormat[] getFileFormats()
    {
    	return fileFormats;
    }
    
    public String getFileFormatString()
    {
    	StringBuilder fileFormats = new StringBuilder();
    	for(FileFormat fileFormat:Arrays.asList(this.fileFormats))
    	{
    		fileFormats.append(fileFormat.name()+",");
    	}
    	return StringUtils.stripEnd(fileFormats.toString(),",");
    }
    public static  ContextE getContext(String context)
    {
    	try
    	{
    	  return ContextE.valueOf(context);
    	}catch(Exception e)
    	{
    		return null;
    	}
    }
    
}
