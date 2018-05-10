package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.FileType;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationMessageManager;
import uk.ac.ebi.embl.api.validation.ValidationPlanResult;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.embl.api.validation.ValidationScope;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.GenomeAssemblyValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.embl.flatfile.reader.genomeassembly.AssemblyInfoReader;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.manifest.ManifestObj;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.utils.FileUtils;

public class InfoFileValidator {
	
	private AssemblyInfoEntry assemblyInfoEntry=null;
	private File reportFile =null;
	
    @Deprecated public AssemblyInfoEntry 
    getAssemblyEntry( File assemblyInfoFile, ValidationResult parseResult) throws IOException
    {
        if (assemblyInfoFile == null)
            return null;
        AssemblyInfoReader reader = (AssemblyInfoReader) GenomeAssemblyFileUtils.getFileReader(FileFormat.INFO, assemblyInfoFile, null);
        parseResult.append(reader.read());
        if (reader.isEntry())
            return (AssemblyInfoEntry) reader.getEntry();
        return null;
    }

	
	public boolean validate(ManifestFileReader manifestFileReader,String reportDir,String context ) throws IOException, ValidationEngineException {
		ValidationResult assemblyInfoParseResult= new ValidationResult();
		Optional<ManifestObj> obj=manifestFileReader.getManifestFileObjects().stream().filter(p->(FileFormat.INFO.equals(p.getFileFormat()))).findFirst();
		assemblyInfoEntry = getAssemblyEntry(new File(obj.get().getFileName()), assemblyInfoParseResult);
		reportFile=new File(reportDir+File.separator+ new File(obj.get().getFileName()).getName() + ".report");

		boolean valid = assemblyInfoParseResult.isValid();
		
		FileUtils.writeReport( reportFile, assemblyInfoParseResult.getMessages(), reportFile.getName() );
		if (assemblyInfoEntry != null) {
			EmblEntryValidationPlanProperty property= new EmblEntryValidationPlanProperty();
			property.isRemote.set(true);
			property.fileType.set(FileType.ASSEMBLYINFO);
			ValidationPlan validationPlan = getValidationPlan(assemblyInfoEntry, property);
			ValidationPlanResult vpr = validationPlan.execute( assemblyInfoEntry );
			
			valid &= vpr.isValid(); 
			FileUtils.writeReport( reportFile, vpr.getMessages(), reportFile.getName() );
			if(ContextE.transcriptome.equals(ContextE.getContext(context)))
				property.validationScope.set(ValidationScope.ASSEMBLY_TRANSCRIPTOME);
		}
     	return valid;
   	}
	

	public ValidationPlan getValidationPlan(Object entry,EmblEntryValidationPlanProperty property)
	{
		ValidationPlan validationPlan = new GenomeAssemblyValidationPlan(property);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_VALIDATION_BUNDLE);
		validationPlan.addMessageBundle(ValidationMessageManager.GENOMEASSEMBLY_FIXER_BUNDLE);
		return validationPlan;
	}
	
	public AssemblyInfoEntry getentry()
	{
		return assemblyInfoEntry;
	}
	
	public File getReportFile()
	{
		return reportFile;
	}
}
