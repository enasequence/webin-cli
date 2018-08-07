/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.manifest;

import java.io.File;

public class ManifestObj {
    private int    lineno;
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

 
	public void
	setLineNo( int lineno )
	{
	    this.lineno = lineno;
	}
	
	
    public int    
    getLineNo()
    {
        return this.lineno;
    }

    
    @Override public String 
	toString() 
	{
		return String.format( "%s\t%s%s", 
				              getFileFormatString(), 
				              new File( getFileName() ).getName(), 
				              null == getMd5chkSum() ? "" : String.format( "\t%s", getMd5chkSum() ) );
	}
		
}
