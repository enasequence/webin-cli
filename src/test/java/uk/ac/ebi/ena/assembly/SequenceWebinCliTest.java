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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.embl.api.entry.feature.FeatureFactory;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliParameters;

public class
SequenceWebinCliTest
{
    @Before public void
    before()
    {
        Locale.setDefault( Locale.UK );
    }

    // Test manifest parsing of file fields
    //

	private static SourceFeature getDefaultSourceFeature()
	{
		SourceFeature source= new FeatureFactory().createSourceFeature();
		source.setScientificName("Micrococcus sp. 5");
		return source;
	}
    @Test( expected = WebinCliException.class )
    public void testGenomeFastaFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FASTA", "test.fasta.gz" );
        }
        finally {
            Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
            Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size());
            Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
            Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size());
            Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.UNLOCALISED_LIST).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_FlatFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FLATFILE", "test.dat.gz" );
        }
        finally {
        	  Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
              Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size());
              Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
              Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size());
              Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.UNLOCALISED_LIST).size());
                }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_AgpFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "AGP", "test.agp.gz" );
        }
        finally {
        	 Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
             Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.UNLOCALISED_LIST).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_ChromosomeListFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "CHROMOSOME_LIST", "test.txt.gz" );
        }
        finally {
        	 Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
             Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.UNLOCALISED_LIST).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testGenome_ManifestParsing_UnlocalisedListFileField() throws Exception
    {
        GenomeAssemblyWebinCli webinCli = new GenomeAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "UNLOCALISED_LIST", "test.txt.gz" );
        }
        finally {
        	 Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.AGP).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
             Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.CHROMOSOME_LIST).size());
             Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.UNLOCALISED_LIST).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testTranscriptome_ManifestParsing_FastaFileField() throws Exception
    {
        TranscriptomeAssemblyWebinCli webinCli = new TranscriptomeAssemblyWebinCli();
        webinCli.setSource(getDefaultSourceFeature());
        try {
            testManifestParsingFileField(webinCli, "FASTA", "test.fasta.gz" );
        }
        finally {
        	Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
            Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testSequence_ManifestParsing_FlatFileField() throws Exception
    {
        SequenceAssemblyWebinCli webinCli = new SequenceAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "FLATFILE", "test.dat.gz" );
        }
        finally {
        	Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.TSV).size());
            Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testSequence_ManifestParsing_TabFileField() throws Exception
    {
        SequenceAssemblyWebinCli webinCli = new SequenceAssemblyWebinCli();
        try {
            testManifestParsingFileField(webinCli, "TAB", "test.dat.gz" );
        }
        finally {
        	Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.TSV).size());
            Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
        }
    }

    @Test( expected = WebinCliException.class )
    public void testTranscriptome_ManifestParsing_FlatFileField() throws Exception
    {
        TranscriptomeAssemblyWebinCli webinCli = new TranscriptomeAssemblyWebinCli();
        webinCli.setSource(getDefaultSourceFeature());

        try {
            testManifestParsingFileField(webinCli, "FLATFILE", "test.dat.gz" );
        }
        finally {
        	Assert.assertEquals(0, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FASTA).size());
            Assert.assertEquals(1, webinCli.getManifestReader().getSubmissionOptions().submissionFiles.get().getFiles(FileType.FLATFILE).size());
        }
    }



    private void testManifestParsingFileField(AbstractWebinCli webinCli, String fieldName, String fileName) throws Exception
    {
        Path inputDir = WebinCliTestUtils.createTempDir().toPath();
        Path filePath = WebinCliTestUtils.createDefaultTempFile( fileName, inputDir, true );

        Path manifestFilePath = Files.write( Files.createTempFile( "TEMP", "MANIFEST" ),
                ("NAME\ttest\n"+fieldName + " " + inputDir.relativize( filePath ).toString()+"\n").getBytes(),
                StandardOpenOption.SYNC, StandardOpenOption.CREATE );

        WebinCliParameters parameters = new WebinCliParameters();

        parameters.setManifestFile( manifestFilePath.toFile() );
        parameters.setInputDir( inputDir.toFile() );
        parameters.setOutputDir( WebinCliTestUtils.createTempDir() );
        try {
            webinCli.init(parameters);
        }
        finally {
            Assert.assertFalse(webinCli.getManifestReader().getValidationResult().isValid());
        }
    }
}
