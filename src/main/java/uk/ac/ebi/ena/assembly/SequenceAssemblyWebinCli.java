package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.jdom2.Element;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;
import uk.ac.ebi.ena.submit.ContextE;


public class SequenceAssemblyWebinCli extends SequenceWebinCli<SequenceAssemblyManifest> {
    @Override
    public ContextE getContext() {
        return ContextE.sequence;
    }

    @Override
    protected SequenceAssemblyManifest createManifestReader() {
        // Create manifest parser which will also set the study field.

        return new SequenceAssemblyManifest(
                isFetchStudy() ? new StudyProcessor(getParameters(), this::setStudy ) : null);
    }

    @Override
    public void readManifest(Path inputDir, File manifestFile) 
    {
        getManifestReader().readManifest(inputDir, manifestFile);
		setSubmissionOptions(getManifestReader().getSubmissionOptions());
        if(getSubmissionOptions().assemblyInfoEntry.isPresent())
        {
         if (getStudy() != null)
            getSubmissionOptions().assemblyInfoEntry.get().setStudyId(getStudy().getProjectId());
        this.setAssemblyInfo(getSubmissionOptions().assemblyInfoEntry.get());
        }
	}

    @Override protected boolean 
    validateInternal() throws ValidationEngineException 
    {
	   	getSubmissionOptions().reportDir = Optional.of(getValidationDir().getAbsolutePath());
		return new SubmissionValidator(getSubmissionOptions()).validate();

    }


    @Override
    Element makeAnalysisType( AssemblyInfoEntry entry )
    {
        Element typeE = new Element( ContextE.sequence.getType() );
        return typeE;
    }
}
