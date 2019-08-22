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

import uk.ac.ebi.ena.webin.cli.WebinCliContext;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.MetadataProcessorFactory;
import uk.ac.ebi.ena.webin.cli.validator.manifest.SequenceManifest;
import uk.ac.ebi.ena.webin.cli.xml.XmlWriter;


public class SequenceAssemblyWebinCli extends SequenceWebinCli<SequenceManifest> {

    public SequenceAssemblyWebinCli(WebinCliParameters parameters) {
        this(parameters, SequenceAssemblyManifestReader.create(ManifestReader.DEFAULT_PARAMETERS, new MetadataProcessorFactory( parameters )), new SequenceAssemblyXmlWriter());
    }

    public SequenceAssemblyWebinCli(WebinCliParameters parameters, ManifestReader<SequenceManifest> manifestReader, XmlWriter<SequenceManifest> xmlWriter) {
        super(WebinCliContext.sequence, parameters, manifestReader, xmlWriter);
    }
}
