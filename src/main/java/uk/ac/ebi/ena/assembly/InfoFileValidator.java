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
	
	public boolean validate(ManifestFileReader manifestFileReader, String reportDir, String context) throws IOException, ValidationEngineException {
		ValidationResult assemblyInfoParseResult= new ValidationResult();
		Optional<ManifestObj> obj=manifestFileReader.getManifestFileObjects().stream().filter(p->(FileFormat.INFO.equals(p.getFileFormat()))).findFirst();
		assemblyInfoEntry = FileUtils.getAssemblyEntry(new File(obj.get().getFileName()), assemblyInfoParseResult);
		reportFile=new File(reportDir+File.separator+ new File(obj.get().getFileName()).getName() + ".report");
		Writer assemblyInforepoWriter = new PrintWriter(reportFile, "UTF-8");
		boolean valid=GenomeAssemblyFileUtils.writeValidationResult(assemblyInfoParseResult, assemblyInforepoWriter,reportFile.getName());
		if (assemblyInfoEntry != null) {
			EmblEntryValidationPlanProperty property= new EmblEntryValidationPlanProperty();
			property.isRemote.set(true);
			property.fileType.set(FileType.ASSEMBLYINFO);
			ValidationPlan validationPlan = getValidationPlan(assemblyInfoEntry, property);
			valid=valid&&GenomeAssemblyFileUtils.writeValidationPlanResult( validationPlan.execute(assemblyInfoEntry),assemblyInforepoWriter, new File(obj.get().getFileName()).getName());
			if(ContextE.transcriptome.equals(ContextE.getContext(context)))
				property.validationScope.set(ValidationScope.ASSEMBLY_TRANSCRIPTOME);
		}
     	return valid;
   	}

	public ValidationPlan getValidationPlan(Object entry,EmblEntryValidationPlanProperty property) {
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
