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

package uk.ac.ebi.ena.transcriptome;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.assembly.GenomeAssemblyWebinCliTest;
import uk.ac.ebi.ena.assembly.TranscriptomeAssemblyWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

import java.util.Locale;

public class TranscriptomeAssemblyWebinCliTest {

    @Before
    public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    @Test
    public void
    testTranscriptomeFileValidation_ValidFasta() throws Exception
    {
        WebinCliParameters parameters = WebinCliTestUtils.createWebinCliParameters(
                WebinCliTestUtils.getFile("uk/ac/ebi/ena/transcriptome/simple_fasta/transcriptome.manifest"));
        TranscriptomeAssemblyWebinCli validator = new TranscriptomeAssemblyWebinCli();
        validator.setFetchSample(false);
        validator.setFetchStudy(false);
        validator.setSample(GenomeAssemblyWebinCliTest.getDefaultSample());
        validator.setStudy(GenomeAssemblyWebinCliTest.getDefaultStudy());
        validator.init(parameters);
        Assert.assertTrue( validator.validate() );
    }
}
