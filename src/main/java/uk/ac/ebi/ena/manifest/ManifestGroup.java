package uk.ac.ebi.ena.manifest;

import java.util.Arrays;
import java.util.List;

import uk.ac.ebi.ena.manifest.FileFormat;

public enum ManifestGroup {

	genome
	(
			FileFormat.FASTA, 
			FileFormat.AGP, 
			FileFormat.FLATFILE, 
			FileFormat.CHROMOSOME_LIST,
			FileFormat.UNLOCALISED_LIST, 
			FileFormat.INFO
	),
	transcriptome
	(	
			FileFormat.FASTA, 
			FileFormat.FLATFILE,
			FileFormat.INFO
	),
	template
	(
			FileFormat.TSV, 
			FileFormat.FLATFILE
	),
	read
	(
			
	);
	
	List<FileFormat> fileFormats;

	ManifestGroup(FileFormat... fileFormats) 
	{
		this.fileFormats = Arrays.asList(fileFormats);
	}

	public List<FileFormat> getFileFormats() 
	{
		return fileFormats;
	}

	
}
