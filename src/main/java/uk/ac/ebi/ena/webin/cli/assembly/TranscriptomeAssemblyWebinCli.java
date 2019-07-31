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

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.helper.MasterSourceFeatureUtils;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelperImpl;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;
import uk.ac.ebi.ena.webin.cli.validator.reference.Attribute;
import uk.ac.ebi.ena.webin.cli.validator.reference.Sample;

public class TranscriptomeAssemblyWebinCli extends SequenceWebinCli<TranscriptomeAssemblyManifest> {

	@Override
	public WebinCliContext getContext() {
		return WebinCliContext.transcriptome;
	}

	@Override protected
	TranscriptomeAssemblyManifest createManifestReader() 
	{
		// Create manifest parser which will also set the sample and study fields.
		return new TranscriptomeAssemblyManifest(
				isMetadataServiceActive(MetadataService.SAMPLE) ? new SampleProcessor(getParameters(), this::setSample) : null,
				isMetadataServiceActive(MetadataService.STUDY)  ? new StudyProcessor(getParameters(), this::setStudy) : null,
				isMetadataServiceActive(MetadataService.SAMPLE_XML) ? new SampleXmlProcessor(getParameters(), this::setSample ):null,
				isMetadataServiceActive(MetadataService.RUN)    ? new RunProcessor( getParameters(), this::setRunRef ) : null,
				isMetadataServiceActive(MetadataService.ANALYSIS) ? new AnalysisProcessor( getParameters(), this::setAnalysisRef ) : null );
	}

	@Override
	protected void readManifest(Path inputDir, File manifestFile) {
		getManifestReader().readManifest(inputDir, manifestFile);
		setSubmissionOptions(getManifestReader().getSubmissionOptions());
	    setDescription( getManifestReader().getDescription() );
		if(getSubmissionOptions().assemblyInfoEntry.isPresent())
		{
		if (getStudy() != null)
			getSubmissionOptions().assemblyInfoEntry.get().setStudyId(getStudy().getBioProjectId());
		if (getSample() != null)
			getSubmissionOptions().assemblyInfoEntry.get().setBiosampleId(getSample().getBioSampleId());
			this.setAssemblyInfo(getSubmissionOptions().assemblyInfoEntry.get());
		}
		if(getStudy()!=null&&getStudy().getLocusTags()!=null)
 			getSubmissionOptions().locusTagPrefixes = Optional.of( getStudy().getLocusTags());

		if( getSample() != null ) {
			Sample sample = getSample();
			SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
			MasterSourceFeatureUtils sourceUtils = new MasterSourceFeatureUtils();
			if (sample.getTaxId() != null) {
				sourceFeature.addQualifier(Qualifier.DB_XREF_QUALIFIER_NAME, String.valueOf(sample.getTaxId()));
			}
			sourceFeature.setScientificName(sample.getOrganism());
			for (Attribute attribute: sample.getAttributes()) {
				sourceUtils.addSourceQualifier(attribute.getName(), attribute.getValue(), sourceFeature);
			}
			sourceUtils.addExtraSourceQualifiers(sourceFeature, new TaxonHelperImpl(), sample.getName());
			getSubmissionOptions().source = Optional.of(sourceFeature);
		}
	}

	@Override	
	Element makeAnalysisType( AssemblyInfoEntry entry ) {
		Element typeE = new Element( WebinCliContext.transcriptome.getXmlElement() );
		typeE.addContent( createTextElement( "NAME", entry.getName() ) );
		typeE.addContent( createTextElement( "PROGRAM",  entry.getProgram() ) );
		typeE.addContent( createTextElement( "PLATFORM", entry.getPlatform() ) );
		if ( entry.isTpa())
			typeE.addContent( createTextElement( "TPA", String.valueOf( entry.isTpa() ) ) );
		if (null != entry.getAuthors() && null != entry.getAddress()) {
			typeE.addContent(createTextElement("AUTHORS", entry.getAuthors()));
			typeE.addContent(createTextElement("ADDRESS", entry.getAddress()));
		}
		return typeE;
	}
}
