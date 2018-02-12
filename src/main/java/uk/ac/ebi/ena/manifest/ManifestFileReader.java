package uk.ac.ebi.ena.manifest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.ena.utils.FileUtils;

public class ManifestFileReader
{
	public final static String regexSpace = "\\s+";
	public final static String regexColon = ":";
	private List<ManifestObj> manifestObjList = new ArrayList<ManifestObj>();
	private static String InvalidNoOfColumns= "InvalidNumberOfColumns";
    private static final String MANIFESTMESSAGEBUNDLE = "uk.ac.ebi.ena.manifest.ManifestValidationMessages";
	private ValidationPlanResult result= new ValidationPlanResult();

	public List<ManifestObj> getManifestFileObjects() {
		return manifestObjList;
	}
	
	public boolean isValid() {
		return result.isValid();
	}

	public ValidationPlanResult read(String manifestFile) throws IOException {
		  ValidationMessageManager.addBundle(MANIFESTMESSAGEBUNDLE);
			int i=0;
			try (BufferedReader br = FileUtils.getBufferedReader(new File(manifestFile))) {

				String line;
				while ((line = br.readLine()) != null) {
					i++;
					String[] lineTokens = line.split(regexSpace);
					if (line.contains(regexColon))
						lineTokens = line.split(regexColon);
					else
						lineTokens = line.split(regexSpace);
					if (lineTokens.length != 2) 
					{
						result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR,InvalidNoOfColumns,i)));
					}
					else
						manifestObjList.add(new ManifestObj(lineTokens[0], lineTokens[1]));
				}

			}
			
			return  result;
	}

	public String getFilenameFromManifest(FileFormat fileFormat) throws ValidationEngineException {
		Optional<ManifestObj> manifestObjOptrional = manifestObjList.stream()
				.filter(p -> fileFormat.equals(p.getFileFormat()))
				.findFirst();
		if (manifestObjOptrional.isPresent())
			return manifestObjOptrional.get().getFileName();
		else
			return null;
	}
}