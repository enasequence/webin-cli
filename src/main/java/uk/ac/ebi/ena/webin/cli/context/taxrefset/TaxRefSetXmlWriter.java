/*
 * Copyright 2018-2021 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.context.taxrefset;

import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createFileElement;
import static uk.ac.ebi.ena.webin.cli.xml.XmlWriterHelper.createTextElement;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.jdom2.Element;

import uk.ac.ebi.ena.webin.cli.context.SequenceToolsXmlWriter;
import uk.ac.ebi.ena.webin.cli.validator.api.ValidationResponse;
import uk.ac.ebi.ena.webin.cli.validator.manifest.TaxRefSetManifest;

public class TaxRefSetXmlWriter extends SequenceToolsXmlWriter<TaxRefSetManifest, ValidationResponse> {

    @Override
    protected Element createXmlAnalysisTypeElement(TaxRefSetManifest manifest) {
        Element e = new Element("TAXONOMIC_REFERENCE_SET");

        e.addContent(createTextElement("NAME", manifest.getName()));
        e.addContent(createTextElement("TAXONOMY_SYSTEM", manifest.getTaxonomySystem()));
        if (null != manifest.getTaxonomySystemVersion() && !manifest.getTaxonomySystemVersion().trim().isEmpty())
            e.addContent(createTextElement("TAXONOMY_SYSTEM_VERSION", manifest.getTaxonomySystemVersion()));
        if (null != manifest.getCustomFields() && !manifest.getCustomFields().isEmpty()) {

            Element customFieldsE = new Element("CUSTOM_FIELDS");

            manifest.getCustomFields().forEach((key, value) -> {
                        Element fileldsE = new Element("FIELD");
                        fileldsE.addContent(createTextElement("NAME", key));
                        fileldsE.addContent(createTextElement("DESCRIPTION", value));
                        customFieldsE.addContent(fileldsE);
                    }
            );

            e.addContent(customFieldsE);
        }

        return e;
    }

    @Override
    protected List<Element> createXmlFileElements(TaxRefSetManifest manifest, Path inputDir, Path uploadDir) {
        List<Element> list = new ArrayList<>();


        manifest.files(TaxRefSetManifest.FileType.FASTA).stream()
                .map(file -> file.getFile().toPath())
                .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "fasta")));
        manifest.files(TaxRefSetManifest.FileType.TAB).stream()
                .map(file -> file.getFile().toPath())
                .forEach(file -> list.add(createFileElement(inputDir, uploadDir, file, "tab")));

        return list;
    }
}
