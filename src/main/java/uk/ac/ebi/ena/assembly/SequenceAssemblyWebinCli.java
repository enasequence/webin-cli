package uk.ac.ebi.ena.assembly;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;
import org.jdom2.Element;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;


public class SequenceAssemblyWebinCli extends SequenceWebinCli<SequenceAssemblyManifest> {
    @Override
    public WebinCliContext getContext() {
        return WebinCliContext.sequence;
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
        setDescription( getManifestReader().getDescription() );
        
    	if(getSubmissionOptions().assemblyInfoEntry.isPresent())
    	{
    		if (getStudy() != null)
    	  	  getSubmissionOptions().assemblyInfoEntry.get().setStudyId(getStudy().getProjectId());
    	    this.setAssemblyInfo(getSubmissionOptions().assemblyInfoEntry.get());
    	}
		if(getStudy()!=null&&getStudy().getLocusTags()!=null)
 			getSubmissionOptions().locusTagPrefixes = Optional.of( getStudy().getLocusTags());
	}

    @Override protected void
    validateInternal() throws ValidationEngineException 
    {
	   	getSubmissionOptions().reportDir = Optional.of(getValidationDir().getAbsolutePath());
        getSubmissionOptions().ignoreErrors = getIgnoreErrorsMode();
	    new SubmissionValidator(getSubmissionOptions()).validate();
    }


    @Override
    Element makeAnalysisType( AssemblyInfoEntry entry )
    {
        Element typeE = new Element( WebinCliContext.sequence.getXmlElement() );
        return typeE;
    }
}
