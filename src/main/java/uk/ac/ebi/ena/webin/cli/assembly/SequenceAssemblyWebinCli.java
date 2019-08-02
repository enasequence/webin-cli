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
import uk.ac.ebi.ena.webin.cli.manifest.processor.AnalysisProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.RunProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.processor.StudyProcessor;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;
import uk.ac.ebi.ena.webin.cli.validator.reference.Attribute;


public class SequenceAssemblyWebinCli extends SequenceWebinCli<SequenceAssemblyManifestReader, SequenceManifest> {

    @Override
    public WebinCliContext getContext() {
        return WebinCliContext.sequence;
    }

    @Override
    protected SequenceAssemblyManifestReader createManifestReader() {
        return new SequenceAssemblyManifestReader(
                isMetadataServiceActive(MetadataService.STUDY)    ? new StudyProcessor( getParameters() ) : null,
                isMetadataServiceActive(MetadataService.RUN)      ? new RunProcessor( getParameters() ) : null,
                isMetadataServiceActive(MetadataService.ANALYSIS) ? new AnalysisProcessor( getParameters() ) : null );
    }

    @Override
    protected void validate(File reportDir, File processDir) throws WebinCliException, ValidationEngineException {
        SequenceManifest manifest = getManifestReader().getManifest();
        manifest.setReportDir(getValidationDir());
        manifest.setProcessDir(getProcessDir());

        SubmissionOptions submissionOptions = new SubmissionOptions();
        submissionOptions.context = Optional.of( Context.sequence );
        AssemblyInfoEntry assemblyInfo = new AssemblyInfoEntry();
        MasterSourceFeatureUtils sourceUtils = new MasterSourceFeatureUtils();
        SubmissionFiles submissionFiles = new SubmissionFiles();

        assemblyInfo.setName( manifest.getName() );
        assemblyInfo.setAuthors(manifest.getAuthors());
        assemblyInfo.setAddress(manifest.getAddress());

        if (manifest.getStudy() != null) {
            assemblyInfo.setStudyId(manifest.getStudy().getBioProjectId());
            if (manifest.getStudy().getLocusTags()!= null) {
                submissionOptions.locusTagPrefixes = Optional.of(manifest.getStudy().getLocusTags());
            }
        }
        if (manifest.getSample() != null) {
            assemblyInfo.setBiosampleId(manifest.getSample().getBioSampleId());

            SourceFeature sourceFeature = new FeatureFactory().createSourceFeature();
            sourceFeature.addQualifier(Qualifier.DB_XREF_QUALIFIER_NAME, String.valueOf(manifest.getSample().getTaxId()));
            sourceFeature.setScientificName(manifest.getSample().getOrganism());
            for (Attribute attribute: manifest.getSample().getAttributes()) {
                sourceUtils.addSourceQualifier(attribute.getName(), attribute.getValue(), sourceFeature);
            }
            sourceUtils.addExtraSourceQualifiers(sourceFeature, new TaxonHelperImpl(), manifest.getName());
        }

        manifest.files().get().forEach(file -> submissionFiles.addFile( new SubmissionFile( SubmissionFile.FileType.FASTA, file.getFile() )));
        manifest.files().get().forEach(file -> submissionFiles.addFile( new SubmissionFile( SubmissionFile.FileType.AGP,file.getFile() )));
        manifest.files().get().forEach(file -> submissionFiles.addFile( new SubmissionFile( SubmissionFile.FileType.FLATFILE, file.getFile() )));
        manifest.files().get().forEach(file -> submissionFiles.addFile( new SubmissionFile( SubmissionFile.FileType.CHROMOSOME_LIST, file.getFile() )));
        manifest.files().get().forEach(file -> submissionFiles.addFile( new SubmissionFile( SubmissionFile.FileType.UNLOCALISED_LIST, file.getFile() )));

        submissionOptions.assemblyInfoEntry = Optional.of( assemblyInfo );
        submissionOptions.isRemote = true;
        submissionOptions.ignoreErrors = manifest.isIgnoreErrors();
        submissionOptions.reportDir = Optional.of(manifest.getReportDir().getAbsolutePath());
        submissionOptions.processDir = Optional.of( manifest.getProcessDir().getAbsolutePath());
        submissionOptions.submissionFiles = Optional.of( submissionFiles );

        new SubmissionValidator(submissionOptions).validate();
    }

    @Override Element
    createXmlAnalysisTypeElement()
    {
        SequenceManifest manifest = getManifestReader().getManifest();

        if (null != manifest.getAuthors() && null != manifest.getAddress()) {
            Element typeE = new Element(WebinCliContext.sequence.getXmlElement());
            typeE.addContent(createXmlTextElement("AUTHORS", manifest.getAuthors()));
            typeE.addContent(createXmlTextElement("ADDRESS", manifest.getAddress()));
            return typeE;
        }
        return new Element(WebinCliContext.sequence.getXmlElement());
    }

    @Override
    protected List<Element> createXmlFileElements(Path uploadDir) {
        List<Element> fileElements = new ArrayList<>();

        SequenceManifest manifest = getManifestReader().getManifest();
        manifest.files( SequenceManifest.FileType.FLATFILE ).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "flatfile" ) ) );
        manifest.files( SequenceManifest.FileType.TAB).forEach( file -> fileElements.add( createXmlFileElement( uploadDir, file.getFile(), "tab" ) ) );

        return fileElements;
    }
}
