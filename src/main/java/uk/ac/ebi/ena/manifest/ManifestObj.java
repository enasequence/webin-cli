package uk.ac.ebi.ena.manifest;

import java.io.File;

public class ManifestObj {
	private String fileFormat;
	private String fileName;
	private String md5chkSum;
	
	public ManifestObj(String fileFormat,String fileName) {
       this.fileFormat= fileFormat;
       this.fileName =fileName;
	}
	
	public FileFormat getFileFormat() {
		return FileFormat.getFormat(fileFormat.toUpperCase());
	}

	public String getFileFormatString()
	{
		return fileFormat;
	}
	public String getFileName() 
	{
		return fileName;
	}
	
	public void setFileFormat(String fileFormat) {
		this.fileFormat = fileFormat;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public String getMd5chkSum() {
		return md5chkSum;
	}

	public void setMd5chkSum(String md5chkSum) {
		this.md5chkSum = md5chkSum;
	}

	@Override
	public String toString() {
		return this.getFileFormat().toString()+"\t"+new File(this.getFileName()).getName()+"\t"+this.getMd5chkSum();
	}
		
}
