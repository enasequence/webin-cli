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
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.SourceFeatureProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;

public class TranscriptomeAssemblyWebinCli extends SequenceWebinCli<TranscriptomeAssemblyManifest> {

	@Override
	public WebinCliContext getContext() {
		return WebinCliContext.transcriptome;
	}

	@Override
	protected TranscriptomeAssemblyManifest createManifestReader() {
		// Create manifest parser which will also set the sample and study fields.

		return new TranscriptomeAssemblyManifest(isFetchSample() ? new SampleProcessor(getParameters(), this::setSample) : null,
				isFetchStudy() ? new StudyProcessor(getParameters(), this::setStudy) : null,
						isFetchSource()?new SourceFeatureProcessor(getParameters(), this::setSource ):null);
	}

	@Override
	public void readManifest(Path inputDir, File manifestFile) {
		getManifestReader().readManifest(inputDir, manifestFile);
		setSubmissionOptions(getManifestReader().getSubmissionOptions());
	    setDescription( getManifestReader().getDescription() );
		if(getSubmissionOptions().assemblyInfoEntry.isPresent())
		{
		if (getStudy() != null)
			getSubmissionOptions().assemblyInfoEntry.get().setStudyId(getStudy().getProjectId());
		if (getSample() != null)
			getSubmissionOptions().assemblyInfoEntry.get().setBiosampleId(getSample().getBiosampleId());
			this.setAssemblyInfo(getSubmissionOptions().assemblyInfoEntry.get());
		}
		if(getStudy()!=null&&getStudy().getLocusTags()!=null)
 			getSubmissionOptions().locusTagPrefixes = Optional.of( getStudy().getLocusTags());
		if(getSource()!=null)
		getSubmissionOptions().source = Optional.of(getSource());
	}

	@Override	
	Element makeAnalysisType( AssemblyInfoEntry entry ) {
		Element typeE = new Element( WebinCliContext.transcriptome.getXmlElement() );
		typeE.addContent( createTextElement( "NAME", entry.getName() ) );
		typeE.addContent( createTextElement( "PROGRAM",  entry.getProgram() ) );
		typeE.addContent( createTextElement( "PLATFORM", entry.getPlatform() ) );
		if ( entry.isTpa())
			typeE.addContent( createTextElement( "TPA", String.valueOf( entry.isTpa() ) ) );
		return typeE;
	}
}
