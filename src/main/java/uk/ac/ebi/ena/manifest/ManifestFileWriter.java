package uk.ac.ebi.ena.manifest;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import uk.ac.ebi.ena.utils.FileUtils;

public class ManifestFileWriter {
	private boolean test =false;
	public ManifestFileWriter()
	{
       this(false);
	}
	public ManifestFileWriter(boolean test) 
	{
	    this.test =test;	
	}

	public void write(File outputFile, List<ManifestObj> manifestObjects) throws IOException, NoSuchAlgorithmException {
		if(outputFile == null)
			return;
		if(!outputFile.exists())
			outputFile.createNewFile();
		if(manifestObjects==null||manifestObjects.isEmpty())
			return;
		try (FileWriter fileWriter = new FileWriter(outputFile,false);PrintWriter writer = new PrintWriter(fileWriter,false)) {
			for(ManifestObj obj: manifestObjects) {
			  if(!test)
			  obj.setMd5chkSum(FileUtils.md5CheckSum(obj.getFileName()));
			  writer.write(obj.toString()+"\n");
		    }
			 writer.flush();
			 fileWriter.flush();
		}
		File manifestChecksumFile= new File(outputFile.getAbsolutePath()+".md5");
		try (FileWriter fileWriter = new FileWriter(manifestChecksumFile,false);PrintWriter writer = new PrintWriter(fileWriter,false))	{
			  if(!test)
		         writer.write(outputFile.getName()+"\t"+ FileUtils.md5CheckSum(outputFile.getAbsolutePath()));
			 writer.flush();
			 fileWriter.flush();
		}
	}
}
