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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TranscriptomeManifest;

public class TranscriptomeAssemblyWebinCli extends SequenceWebinCli<TranscriptomeAssemblyManifestReader, TranscriptomeManifest> {

	public TranscriptomeAssemblyWebinCli() {
		super(WebinCliContext.transcriptome);
	}

	@Override protected TranscriptomeAssemblyManifestReader createManifestReader() {
		return TranscriptomeAssemblyManifestReader.create( ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory( getParameters()) );
	}

	@Override
	protected String getTitle() {
		return "Transcriptome assembly: " + getName();
	}

	@Override Element
	createXmlAnalysisTypeElement()
	{
		TranscriptomeManifest manifest = getManifestReader().getManifest();

		Element element = new Element( "TRANSCRIPTOME_ASSEMBLY" );
		element.addContent( createXmlTextElement( "NAME", manifest.getName() ) );
		element.addContent( createXmlTextElement( "PROGRAM",  manifest.getProgram() ) );
		element.addContent( createXmlTextElement( "PLATFORM", manifest.getPlatform() ) );
		if ( manifest.isTpa())
			element.addContent( createXmlTextElement( "TPA", String.valueOf( manifest.isTpa() ) ) );
		if (null != manifest.getAuthors() && null != manifest.getAddress()) {
			element.addContent(createXmlTextElement("AUTHORS", manifest.getAuthors()));
			element.addContent(createXmlTextElement("ADDRESS", manifest.getAddress()));
		}
		return element;
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
