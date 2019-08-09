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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.jdom2.Element;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.entry.qualifier.Qualifier;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.helper.MasterSourceFeatureUtils;
import uk.ac.ebi.embl.api.validation.helper.taxon.TaxonHelperImpl;
import uk.ac.ebi.embl.api.validation.submission.*;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Attribute;


public class TranscriptomeAssemblyWebinCli extends SequenceWebinCli<TranscriptomeAssemblyManifestReader, TranscriptomeManifest> {

	@Override
	public WebinCliContext getContext() {
		return WebinCliContext.transcriptome;
	}

	@Override protected TranscriptomeAssemblyManifestReader createManifestReader(MetadataProcessorFactory metadataProcessorFactory) {
		return new TranscriptomeAssemblyManifestReader(
				metadataProcessorFactory.createSampleProcessor(getParameters()),
				metadataProcessorFactory.createStudyProcessor(getParameters()),
				metadataProcessorFactory.createSampleXmlProcessor(getParameters()),
				metadataProcessorFactory.createRunProcessor( getParameters() ),
				metadataProcessorFactory.createAnalysisProcessor( getParameters() ));
	}

	@Override Element
	createXmlAnalysisTypeElement()
	{
		TranscriptomeManifest manifest = getManifestReader().getManifest();

		Element typeE = new Element( WebinCliContext.transcriptome.getXmlElement() );
		typeE.addContent( createXmlTextElement( "NAME", manifest.getName() ) );
		typeE.addContent( createXmlTextElement( "PROGRAM",  manifest.getProgram() ) );
		typeE.addContent( createXmlTextElement( "PLATFORM", manifest.getPlatform() ) );
		if ( manifest.isTpa())
			typeE.addContent( createXmlTextElement( "TPA", String.valueOf( manifest.isTpa() ) ) );
		if (null != manifest.getAuthors() && null != manifest.getAddress()) {
			typeE.addContent(createXmlTextElement("AUTHORS", manifest.getAuthors()));
			typeE.addContent(createXmlTextElement("ADDRESS", manifest.getAddress()));
		}
		return typeE;
	}

	@Override
	protected List<Element> createXmlFileElements(Path uploadDir) {
		List<Element> fileElements = new ArrayList<>();

		TranscriptomeManifest manifest = getManifestReader().getManifest();
		manifest.files( TranscriptomeManifest.FileType.FASTA ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "fasta" ) ) );
		manifest.files( TranscriptomeManifest.FileType.FLATFILE ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "flatfile" ) ) );

		return fileElements;
	}
}
