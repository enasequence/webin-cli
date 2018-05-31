package uk.ac.ebi.ena.manifest;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.ena.utils.FileUtils;

public class ManifestFileReader
{
    static final Pattern p = Pattern.compile( "^(#|;|\\/\\/|--).*$|^(\\S+)\\s+(\\S+)$" );
	private List<ManifestObj> manifestObjList = new ArrayList<ManifestObj>();
	private static String InvalidNoOfColumns= "InvalidNumberOfColumns";
    private static final String MANIFESTMESSAGEBUNDLE = "uk.ac.ebi.ena.manifest.ManifestValidationMessages";
    ValidationPlanResult result=null;

	public List<ManifestObj> getManifestFileObjects() {
		return manifestObjList;
	}
	
	public boolean isValid() {
		return result.isValid();
	}

	public ValidationPlanResult read(String manifestFile) throws IOException {
		 result= new ValidationPlanResult();
		  ValidationMessageManager.addBundle(MANIFESTMESSAGEBUNDLE);
			int i=0;
			try (BufferedReader br = FileUtils.getBufferedReader(new File(manifestFile))) {

				String line;
				while( ( line = br.readLine() ) != null )
				{
					i++;  
					line = line.trim();

					if( line.isEmpty() )
					    continue;

					Matcher m = p.matcher( line );
					if( m.find() )
					{
					    if( null == m.group( 1 ) )
					        manifestObjList.add( new ManifestObj( m.group( 2 ), m.group( 3 ) ) );
					} else
					{
						result.append(new ValidationResult().append(new ValidationMessage<>(Severity.ERROR,InvalidNoOfColumns,i)));
					} 
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