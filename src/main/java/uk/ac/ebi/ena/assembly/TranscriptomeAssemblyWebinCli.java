/*
 * Copyright 2018 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package uk.ac.ebi.ena.assembly;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.zip.GZIPInputStream;

import org.jdom2.Element;

import uk.ac.ebi.embl.api.entry.Entry;
import uk.ac.ebi.embl.api.entry.Text;
import uk.ac.ebi.embl.api.entry.XRef;
import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.entry.location.Location;
import uk.ac.ebi.embl.api.entry.location.LocationFactory;
import uk.ac.ebi.embl.api.entry.location.Order;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.entry.sequence.Sequence;
import uk.ac.ebi.embl.api.validation.*;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlan;
import uk.ac.ebi.embl.api.validation.plan.EmblEntryValidationPlanProperty;
import uk.ac.ebi.embl.api.validation.plan.ValidationPlan;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.ena.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.manifest.processor.SourceFeatureProcessor;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliReporter;

public class TranscriptomeAssemblyWebinCli extends SequenceWebinCli<TranscriptomeAssemblyManifest> {
	private static final String VALIDATION_MESSAGES_BUNDLE = "ValidationSequenceMessages";
	private static final String STANDARD_VALIDATION_BUNDLE = "uk.ac.ebi.embl.api.validation.ValidationMessages";
	private static final String STANDARD_FIXER_BUNDLE = "uk.ac.ebi.embl.api.validation.FixerMessages";
	private static final String MOL_TYPE = "transcribed RNA";


	@Override
	public ContextE getContext() {
		return ContextE.transcriptome;
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
		if(getSubmissionOptions().assemblyInfoEntry.isPresent())
		{
		if (getStudy() != null)
			getSubmissionOptions().assemblyInfoEntry.get().setStudyId(getStudy().getProjectId());
		if (getSample() != null)
			getSubmissionOptions().assemblyInfoEntry.get().setBiosampleId(getSample().getBiosampleId());
			this.setAssemblyInfo(getSubmissionOptions().assemblyInfoEntry.get());
		}
		if(getSource()!=null)
		getSubmissionOptions().source = Optional.of(getSource());
		getSubmissionOptions().reportDir = Optional.of(getValidationDir().getAbsolutePath());
	}

	

	@Override protected void 
	validateInternal() throws ValidationEngineException 
	{
	   	getSubmissionOptions().reportDir = Optional.of( getValidationDir().getAbsolutePath() );
		new SubmissionValidator( getSubmissionOptions() ).validate();
	}

	
	@Override	
	Element makeAnalysisType( AssemblyInfoEntry entry ) {
		Element typeE = new Element( ContextE.transcriptome.getType() );
		typeE.addContent( createTextElement( "NAME", entry.getName() ) );
		typeE.addContent( createTextElement( "PROGRAM",  entry.getProgram() ) );
		typeE.addContent( createTextElement( "PLATFORM", entry.getPlatform() ) );
		if ( entry.isTpa())
			typeE.addContent( createTextElement( "TPA", String.valueOf( entry.isTpa() ) ) );
		return typeE;
	}
}
