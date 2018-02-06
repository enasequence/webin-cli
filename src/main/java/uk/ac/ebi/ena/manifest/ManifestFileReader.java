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
		final StringBuilder index = new StringBuilder("1");
		try (Stream<String> stream = Files.lines(Paths.get(manifestFile))) {
			stream.forEach(line -> {
				int currentIndex = Integer.valueOf(index.toString());
				String[] lineA = line.split(regexSpace);
				if (line.isEmpty() || lineA.length != 2)
					result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR, InvalidNoOfColumns, currentIndex)));
				else
					manifestObjList.add(new ManifestObj(lineA[0], lineA[1]));
				index.setLength(0);
				currentIndex++;
				index.append((currentIndex));
			});
		}
		return result;
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