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
import uk.ac.ebi.embl.fasta.reader.FastaFileReader;
import uk.ac.ebi.embl.fasta.reader.FastaLineReader;
import uk.ac.ebi.embl.flatfile.reader.FlatFileReader;
import uk.ac.ebi.embl.flatfile.reader.embl.EmblEntryReader;
import uk.ac.ebi.ena.manifest.processor.SampleProcessor;
import uk.ac.ebi.ena.manifest.processor.StudyProcessor;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.webin.cli.WebinCliReporter;

public class TranscriptomeAssemblyWebinCli extends SequenceWebinCli<TranscriptomeAssemblyManifest> {
	private static final String VALIDATION_MESSAGES_BUNDLE = "ValidationSequenceMessages";
	private static final String STANDARD_VALIDATION_BUNDLE = "uk.ac.ebi.embl.api.validation.ValidationMessages";
	private static final String STANDARD_FIXER_BUNDLE = "uk.ac.ebi.embl.api.validation.FixerMessages";
	private static final String MOL_TYPE = "transcribed RNA";
	private File reportFile;
	private String submittedFile;
	private boolean FAILED_VALIDATION;

	@Override
	public ContextE getContext() {
		return ContextE.transcriptome;
	}

	@Override
	protected TranscriptomeAssemblyManifest createManifestReader() {
		// Create manifest parser which will also set the sample and study fields.

		return new TranscriptomeAssemblyManifest(isFetchSample() ?
				new SampleProcessor(getParameters(), this::setSample) : null,
				isFetchStudy() ? new StudyProcessor(getParameters(), this::setStudy) : null);
	}

	@Override
	public void readManifest(Path inputDir, File manifestFile) {
		getManifestReader().readManifest(inputDir, manifestFile);

		if (getManifestReader().getFastaFile() != null)
			this.fastaFiles.add(getManifestReader().getFastaFile());
		if (getManifestReader().getFlatFile() != null)
			this.flatFiles.add(getManifestReader().getFlatFile());
		AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
		assemblyInfo.setName(getManifestReader().getName());
		if (getStudy() != null)
			assemblyInfo.setStudyId(getStudy().getProjectId());
		if (getSample() != null)
			assemblyInfo.setSampleId(getSample().getBiosampleId());
		assemblyInfo.setPlatform(getManifestReader().getPlatform());
		assemblyInfo.setProgram(getManifestReader().getProgram());
		assemblyInfo.setTpa(getManifestReader().getTpa());
		this.setAssemblyInfo(assemblyInfo);
	}

	private ValidationPlan getValidationPlan(FileType fileType) {
		EmblEntryValidationPlanProperty emblEntryValidationProperty = new EmblEntryValidationPlanProperty();
		emblEntryValidationProperty.validationScope.set( ValidationScope.ASSEMBLY_TRANSCRIPTOME );
		emblEntryValidationProperty.isDevMode.set(false);
		emblEntryValidationProperty.isFixMode.set(true);
		emblEntryValidationProperty.isAssembly.set(false);
		emblEntryValidationProperty.minGapLength.set(0);
		emblEntryValidationProperty.fileType.set(fileType);
		if (getStudy() != null)
			emblEntryValidationProperty.locus_tag_prefixes.set( getStudy().getLocusTagsList() );
		emblEntryValidationProperty.isFixCds.set(true);
		ValidationPlan validationPlan = new EmblEntryValidationPlan(emblEntryValidationProperty);
		validationPlan.addMessageBundle( VALIDATION_MESSAGES_BUNDLE );
		validationPlan.addMessageBundle( STANDARD_VALIDATION_BUNDLE );
		validationPlan.addMessageBundle( STANDARD_FIXER_BUNDLE );
		return validationPlan;
	}

	@Override
	protected boolean validateInternal() throws ValidationEngineException {
		if( !flatFiles.isEmpty()) {
			submittedFile = flatFiles.get( 0 ).getPath();
			reportFile = getReportFile(submittedFile);
			validateFlatFile();
		} else if( !fastaFiles.isEmpty()){
			submittedFile = fastaFiles.get( 0 ).getPath();
			reportFile = getReportFile(submittedFile);
			validateFastaFile();
		} else
			throw new ValidationEngineException("Manifest file: FASTA or FLATFILE must be present.");
		return !FAILED_VALIDATION;
	}

	private void validateFlatFile() throws ValidationEngineException {
		try  {
			Path path = Paths.get(submittedFile);
			if (!Files.exists(path))
				throw new ValidationEngineException("Flat file " + submittedFile + " does not exist");
			File flatFileF = path.toFile();
			FlatFileReader flatFileReader = new EmblEntryReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(flatFileF)))), EmblEntryReader.Format.ASSEMBLY_FILE_FORMAT, flatFileF.getName());
			ValidationResult validationResult = flatFileReader.read();
			if (!validationResult.isValid()) {
				FAILED_VALIDATION = true;
				WebinCliReporter.writeToFile(reportFile, validationResult);
			}
			ValidationPlanResult validationPlanResult;
			ValidationPlan validationPlan =  getValidationPlan(FileType.EMBL);
			while (flatFileReader.isEntry()) {
				Entry entry = (Entry)flatFileReader.getEntry();
				entry.getPrimarySourceFeature().setScientificName( getSample().getOrganism());
				if( getSample().getBiosampleId()!=null)
					entry.addXRef(new XRef("BioSample", getSample().getBiosampleId()));
				if( getStudy().getProjectId() != null )
					entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
				validationPlanResult = validationPlan.execute(entry);
				if (!validationPlanResult.isValid()) {
					FAILED_VALIDATION = true;
					WebinCliReporter.writeToFile(reportFile, validationResult);
				}
				flatFileReader.read();
			}
		} catch (IOException e) {
			throw new ValidationEngineException(e);
		}
	}

	private void validateFastaFile() throws ValidationEngineException {
		try {
			Path path = Paths.get(submittedFile);
			if (!Files.exists(path))
				throw new ValidationEngineException("Fasta file " + submittedFile + " does not exist");
			FastaFileReader fastaFileReader = new FastaFileReader(new FastaLineReader(new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(path.toFile()))))));
			ValidationResult validationResult = fastaFileReader.read();
			if (!validationResult.isValid()) {
				FAILED_VALIDATION = true;
				WebinCliReporter.writeToFile(reportFile, validationResult);
			}
			ValidationPlanResult validationPlanResult;
			ValidationPlan validationPlan =  getValidationPlan(FileType.FASTA);
			while (fastaFileReader.isEntry()) {
				Entry entry = fastaFileReader.getEntry();
				entry.getSequence().setVersion(1);
				entry.setDataClass(Entry.TSA_DATACLASS);
				entry.getSequence().setTopology(Sequence.Topology.LINEAR);
				SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
				sourceFeature.setScientificName( getSample().getOrganism() );
				if( getSample().getBiosampleId() != null )
					entry.addXRef( new XRef( "BioSample", getSample().getBiosampleId() ) );
				if( getStudy().getProjectId() != null )
					entry.addProjectAccession( new Text( getStudy().getProjectId() ) );
				sourceFeature.addQualifier(Qualifier.MOL_TYPE_QUALIFIER_NAME, MOL_TYPE);
				Order<Location> featureLocation = new Order<Location>();
				featureLocation.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				sourceFeature.setLocations(featureLocation);
				entry.addFeature(sourceFeature);
				entry.setDescription(new Text("TSA: " + entry.getPrimarySourceFeature().getScientificName() + " " + entry.getSubmitterAccession()));
				Order<Location> order = new Order<Location>();
				order.addLocation(new LocationFactory().createLocalRange(1l, entry.getSequence().getLength()));
				entry.getPrimarySourceFeature().setLocations(order);
				validationPlanResult = validationPlan.execute(entry);
				if (!validationPlanResult.isValid()) {
					WebinCliReporter.writeToFile(reportFile, validationPlanResult);
					FAILED_VALIDATION = true;
				}
				fastaFileReader.read();
			}
		} catch (IOException e) {
			throw new ValidationEngineException(e);
		}
	}

	@Override
	public SubmissionBundle getSubmissionBundle() {
		if( !FAILED_VALIDATION)
			prepareSubmissionBundle();
		return super.getSubmissionBundle();
	}

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
