/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.assembly;

import java.io.File;
import java.nio.file.Path;
import java.util.Optional;

import org.jdom2.Element;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;


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
	   	getSubmissionOptions().processDir = Optional.of(getInputDir().getAbsolutePath());

	    new SubmissionValidator(getSubmissionOptions()).validate();
    }


    @Override
    Element makeAnalysisType( AssemblyInfoEntry entry )
    {
        return new Element( WebinCliContext.sequence.getXmlElement() );
    }
}
