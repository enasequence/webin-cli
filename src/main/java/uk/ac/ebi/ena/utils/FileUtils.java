package uk.ac.ebi.ena.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.xml.bind.DatatypeConverter;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.ena.assembly.GenomeAssemblyFileUtils;
import uk.ac.ebi.ena.manifest.FileFormat;

public class FileUtils {

	public static BufferedReader getBufferedReader(File file) throws FileNotFoundException, IOException 
	{
		if (file.getName().matches("^.+\\.gz$") || file.getName().matches("^.+\\.gzip$")) 
		{
			GZIPInputStream gzip = new GZIPInputStream(new FileInputStream(file));
			return new BufferedReader(new InputStreamReader(gzip));
		} 
		else if (file.getName().matches("^.+\\.bz2$") || file.getName().matches("^.+\\.bzip2$")) 
		{
			BZip2CompressorInputStream bzIn = new BZip2CompressorInputStream(new FileInputStream(file));
			return new BufferedReader(new InputStreamReader(bzIn));
		}
		else 
		{
			return new BufferedReader(new FileReader(file));
		}
	}
	
	public static boolean isZipped(String fileName)
	{
		if (fileName.matches("^.+\\.gz$") || fileName.matches("^.+\\.gzip$")||fileName.matches("^.+\\.bz2$") || fileName.matches("^.+\\.bzip2$"))
		{
			return true;
		}
		return false;
	}
	
	
	public static String  gZipFile(File file) throws IOException {
		String fileName = file.getName();
        if(isZipped(file.getName()))
        	return file.getAbsolutePath();
		byte[] buffer = new byte[1024];
		try (GZIPOutputStream gzos = new GZIPOutputStream(new FileOutputStream(file.getAbsolutePath() + ".gz"));
				FileInputStream in = new FileInputStream(file)){
			fileName= file.getAbsolutePath() + ".gz";
			int len;
			while ((len = in.read(buffer)) > 0) 
				gzos.write(buffer, 0, len);
		}
		file .delete();
		return fileName;
	}
	
	
	public static String md5CheckSum(String filePath) throws IOException, NoSuchAlgorithmException {
		byte[] b = Files.readAllBytes(Paths.get(filePath));
		return DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(b));
	}

	public static AssemblyInfoEntry getAssemblyEntry(File assemblyInfoFile, ValidationResult parseResult) throws IOException {
		if (assemblyInfoFile == null)
			return null;
		AssemblyInfoReader reader = (AssemblyInfoReader) GenomeAssemblyFileUtils.getFileReader(FileFormat.INFO, assemblyInfoFile, null);
		parseResult.append(reader.read());
		if (reader.isEntry())
			return (AssemblyInfoEntry) reader.getEntry();
		return null;
	}
	
	public static boolean emptyDirectory(File dir) {
	    if (dir.exists()) {
	        File[] files = dir.listFiles();
	        for (int i = 0; i < files.length; i++) {
	            if (files[i].isDirectory()) {
	            	emptyDirectory(files[i]);
	            } else {
	                files[i].delete();
	            }
	        }
	    }
	    return dir.listFiles().length==0;
	}

	public static void createReportFile(String submittedFile, String reportFile, String reportDir) throws ValidationEngineException {
		try {
			Path submittedFilePath = Paths.get(submittedFile);
			if (!Files.exists(submittedFilePath))
				throw new ValidationEngineException("Flat file " + submittedFile + " does not exist");
			reportFile = reportDir + File.separator + submittedFilePath.getFileName().toString() + ".report";
			Path reportPath = Paths.get(reportFile);
			if (Files.exists(reportPath))
				Files.delete(reportPath);
			Files.createFile(reportPath);
		} catch (IOException e) {
			throw new ValidationEngineException("Unable to create report file.");
		}
	}

	public static void writeReport(String reportFile, Severity severity, String message) {
		try {
			message = severity.name() + ": " + message;
			Files.write(Paths.get(reportFile), message.getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {}
	}

	public static void writeReport(String reportFile, ValidationResult validationResult) {
		try {
			Collection<ValidationMessage<Origin>> validationMessagesList =  validationResult.getMessages();
			for (ValidationMessage validationMessage: validationMessagesList)
                Files.write(Paths.get(reportFile), validationMessage.getMessage().getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {}
	}

	public static void writeReport(String reportFile, List<ValidationMessage<Origin>> validationMessagesList) {
		try {
			for (ValidationMessage validationMessage: validationMessagesList)
                Files.write(Paths.get(reportFile), (validationMessage.getMessage() + "\n").getBytes(), StandardOpenOption.APPEND);
		} catch (IOException e) {}
	}
}

