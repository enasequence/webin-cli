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
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;


public class SequenceAssemblyWebinCli extends SequenceWebinCli<SequenceAssemblyManifestReader, SequenceManifest> {

    public SequenceAssemblyWebinCli() {
        super(WebinCliContext.sequence);
    }

    @Override
    protected SequenceAssemblyManifestReader createManifestReader() {
        return SequenceAssemblyManifestReader.create( ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory( getParameters()) );
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
