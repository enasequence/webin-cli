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

import org.jdom2.Element;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.submission.SubmissionValidator;
import uk.ac.ebi.ena.model.manifest.GenomeManifest;
import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.manifest.processor.*;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class 
GenomeAssemblyWebinCli extends SequenceWebinCli<GenomeAssemblyManifestReader, GenomeManifest>
{
	@Override public WebinCliContext
	getContext() 
	{
		return WebinCliContext.genome;
	}

	@Override protected GenomeAssemblyManifestReader
	createManifestReader()
	{
		return new GenomeAssemblyManifestReader(
				isMetadataServiceActive(MetadataService.SAMPLE) ? new SampleProcessor( getParameters() ) : null,
				isMetadataServiceActive(MetadataService.STUDY) ? new StudyProcessor( getParameters() ) : null,
				isMetadataServiceActive(MetadataService.RUN) ? new RunProcessor( getParameters() ) : null,
				isMetadataServiceActive(MetadataService.ANALYSIS) ? new AnalysisProcessor( getParameters() ) : null,
				isMetadataServiceActive(MetadataService.SAMPLE_XML) ? new SampleXmlProcessor( getParameters() ) : null );
	}

	@Override
	protected void validate(File reportDir1, File processDir1) throws WebinCliException, ValidationEngineException {
		GenomeManifest manifest = getManifestReader().getManifest();
		manifest.setReportDir(getValidationDir());
		manifest.setProcessDir(getProcessDir());

		new SubmissionValidator().validate(manifest);
	}

	@Override Element
	createXmlAnalysisTypeElement()
	{
		GenomeManifest manifest = getManifestReader().getManifest();

		Element typeE = new Element( WebinCliContext.genome.getXmlElement() );

		typeE.addContent( createXmlTextElement( "NAME", manifest.getName() ) );
		if( null != manifest.getAssemblyType() && !manifest.getAssemblyType().isEmpty() )
			typeE.addContent( createXmlTextElement( "TYPE", manifest.getAssemblyType()));
		typeE.addContent( createXmlTextElement( "PARTIAL", String.valueOf( Boolean.FALSE ) ) ); //as per SraAnalysisParser.setAssemblyInfo
		typeE.addContent( createXmlTextElement( "COVERAGE", manifest.getCoverage() ) );
		typeE.addContent( createXmlTextElement( "PROGRAM",  manifest.getProgram() ) );
		typeE.addContent( createXmlTextElement( "PLATFORM", manifest.getPlatform() ) );

		if( null != manifest.getMinGapLength() )
			typeE.addContent( createXmlTextElement( "MIN_GAP_LENGTH", String.valueOf( manifest.getMinGapLength() ) ) );

		if( null != manifest.getMoleculeType() && !manifest.getMoleculeType().isEmpty() )
			typeE.addContent( createXmlTextElement( "MOL_TYPE", manifest.getMoleculeType() ) );

		if( manifest.isTpa() )
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

		GenomeManifest manifest = getManifestReader().getManifest();
		manifest.files( GenomeManifest.FileType.CHROMOSOME_LIST ).forEach(file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "chromosome_list" ) ) );
		manifest.files( GenomeManifest.FileType.UNLOCALISED_LIST ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "unlocalised_list" ) ) );
		manifest.files( GenomeManifest.FileType.FASTA ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "fasta" ) ) );
		manifest.files( GenomeManifest.FileType.FLATFILE ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "flatfile" ) ) );
		manifest.files( GenomeManifest.FileType.AGP ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "agp" ) ) );

		return fileElements;
	}
}
