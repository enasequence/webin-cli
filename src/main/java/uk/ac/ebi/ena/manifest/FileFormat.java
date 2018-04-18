package uk.ac.ebi.ena.manifest;

public enum FileFormat {
	  FASTA,
	  AGP,
	  FLATFILE,
	  UNLOCALISED_LIST,
	  INFO,
	  CHROMOSOME_LIST,
	  TSV;
	
	public static FileFormat getFormat(String fileFormat) {
		try {
			return	FileFormat.valueOf(fileFormat);
		} catch(Exception e)	{
			return null;
		}
	}
}
