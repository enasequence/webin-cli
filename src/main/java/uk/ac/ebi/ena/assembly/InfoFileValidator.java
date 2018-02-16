package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.GenomeAssemblyValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.ena.manifest.FileFormat;
import uk.ac.ebi.ena.manifest.ManifestFileReader;
import uk.ac.ebi.ena.manifest.ManifestObj;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.utils.FileUtils;

public class InfoFileValidator {
	
	private AssemblyInfoEntry assemblyInfoEntry=null;
	private File reportFile =null;
	private static String platform_missing = "AssemblyInfoPlatformMissingCheck";
	private static String program_missing = "AssemblyInfoProgramMissingCheck";
	
	public boolean validate(ManifestFileReader manifestFileReader,String reportDir,String context ) throws IOException, ValidationEngineException {
		List<ValidationPlanResult> assemblyInfoPlanResults = new ArrayList<ValidationPlanResult>();
		List<ValidationResult> assemblyInfoParseResults = new ArrayList<ValidationResult>();
		ValidationResult assemblyInfoParseResult= new ValidationResult();
		Optional<ManifestObj> obj=manifestFileReader.getManifestFileObjects().stream().filter(p->(FileFormat.INFO.equals(p.getFileFormat()))).findFirst();
		assemblyInfoEntry = FileUtils.getAssemblyEntry(new File(obj.get().getFileName()), assemblyInfoParseResult);
		assemblyInfoParseResults.add(assemblyInfoParseResult);
		reportFile=new File(reportDir+File.separator+ new File(obj.get().getFileName()).getName() + ".report");
		Writer assemblyInforepoWriter = new PrintWriter(reportFile, "UTF-8");
		if (assemblyInfoEntry != null) {
			EmblEntryValidationPlanProperty property= new EmblEntryValidationPlanProperty();
			property.isRemote.set(true);
			property.fileType.set(FileType.ASSEMBLYINFO);
			ValidationPlan validationPlan = getValidationPlan(assemblyInfoEntry, property);
			assemblyInfoPlanResults.add(validationPlan.execute(assemblyInfoEntry));
			if(ContextE.transcriptome.equals(ContextE.getContext(context)))
				property.validationScope.set(ValidationScope.ASSEMBLY_TRANSCRIPTOME);
		}
     	return GenomeAssemblyFileUtils.writeValidationResult(assemblyInfoParseResults,assemblyInfoPlanResults, assemblyInforepoWriter,obj.get().getFileName());
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
