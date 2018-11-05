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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.Optional;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.submission.Context;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.WebinCliTestUtils;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public class SequenceAssemblyValidationTest {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private final static String[] allTemplatesA = {"ERT000002-rRNA.tsv.gz",
			"ERT000003-EST-1.tsv.gz",
			"ERT000006-SCM.tsv.gz",
			"ERT000009-ITS.tsv.gz",
			"ERT000020-COI.tsv.gz",
			"ERT000024-GSS-1.tsv.gz",
			"ERT000028-SVC.tsv.gz",
			"ERT000029-SCGD.tsv.gz",
			"ERT000030-MHC1.tsv.gz",
			"ERT000031-viroid.tsv.gz",
			"ERT000032-matK.tsv.gz",
			"ERT000034-Dloop.tsv.gz",
			"ERT000035-IGS.tsv.gz",
			"ERT000036-MHC2.tsv.gz",
			"ERT000037-intron.tsv.gz",
			"ERT000038-hyloMarker.tsv.gz",
			"ERT000039-Sat.tsv.gz",
			"ERT000042-ncRNA.tsv.gz",
			"ERT000047-betasat.tsv.gz",
			"ERT000050-ISR.tsv.gz",
			"ERT000051-poly.tsv.gz",
			"ERT000052-ssRNA.tsv.gz",
			"ERT000053-ETS.tsv.gz",
			"ERT000054-prom.tsv.gz",
			"ERT000055-STS.tsv.gz",
			"ERT000056-mobele.tsv.gz",
			"ERT000057-alphasat.tsv.gz",
			"ERT000058-MLmarker.tsv.gz",
	"ERT000060-vUTR.tsv.gz"};

	//TODO Default Locale handling is incorrect
	@Before public void 
	before()
	{
		Locale.setDefault( Locale.UK );
	}


	@Test
	public void mandatoryFieldsPresent() throws IOException, ValidationEngineException  {
	
			File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-mandatory-field-missing.tsv.gz");
			SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
			thrown.expect( WebinCliException.class );
			thrown.expectMessage(getmessage("tsv",file.getName(), validator.getValidationDir().getAbsolutePath()));
			validator.validate();
			String expectedResults = new String(Files.readAllBytes(Paths.get("src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-mandatory-field-missing-expected-results.txt")));
			assertEquals(validator.getValidationDir()+File.separator+"Sequence-mandatory-field-missing.tsv.gz.report", expectedResults.replaceAll("\\s+", ""));
		
	}

	@Test
	public void testAllTemplate() throws ValidationEngineException {
		
			for (String testTsvFile: allTemplatesA) {
				File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/"+testTsvFile);
				SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
				validator.validate();
			}
	
	}

	@Test
	public void invalidAlphanumericEntrynumber() throws ValidationEngineException, IOException  {
	
			File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-invalid-alphanumeric-entrynumber-.tsv.gz");
			SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
			thrown.expect( WebinCliException.class );
			thrown.expectMessage(getmessage("tsv",file.getName(), validator.getValidationDir().getAbsolutePath()));
			validator.validate();
			String expectedResults = new String(Files.readAllBytes(Paths.get("src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-invalidAlphanumericEntrynumber-expected-results.txt")));
			assertEquals(validator.getValidationDir()+File.separator+"Sequence-invalid-alphanumeric-entrynumber-.tsv.gz.report", expectedResults.replaceAll("\\s+", ""));
		}

	/*@Test
	public void requiredHeadersMissing() throws ValidationEngineException, IOException {
		
			File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-ERT000039-missingheaders.tsv.gz");
			SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
			thrown.expect(ValidationEngineException.class);
			thrown.expectMessage("TSV file validation failed : Sequence-ERT000039-missingheaders.tsv.gz");
			assertFalse(validator.validate());
			String expectedResults = new String(Files.readAllBytes(Paths.get("src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-ERT000039-missingheaders-expected-results.txt")));
			assertEquals(validator.getValidationDir()+File.separator+"Sequence-ERT000039-missingheaders.tsv.gz.report", expectedResults.replaceAll("\\s+", ""));
		
	}*/

	@Test
	public void invalidMarker() throws ValidationEngineException, IOException {
		
			File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-invalid-marker.tsv.gz");
			SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
			thrown.expect( WebinCliException.class);
			thrown.expectMessage(getmessage("tsv",file.getName(), validator.getValidationDir().getAbsolutePath()));
			validator.validate();
//???
//			String expectedResults = new String(Files.readAllBytes(Paths.get("src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-invalidMarker-expected-results.txt")));
//			assertEquals(validator.getValidationDir()+File.separator+"Sequence-invalid-marker.tsv.gz.report", expectedResults.replaceAll("\\s+", ""));
		}

	
	@Test public void 
	invalidSediment() throws ValidationEngineException, IOException 
	{
	
		File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-invalid-sediment.tsv.gz" );
		SequenceAssemblyWebinCli validator = getValidator( file, FileType.TSV );
		try
		{
		    validator.validate();
		    fail();
		    
		} catch( WebinCliException wce )
		{
			assertEquals( getmessage( "tsv",file.getName(), validator.getValidationDir().getAbsolutePath() ),wce.getMessage());
		    String expectedResults = new String( Files.readAllBytes( Paths.get( "src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-invalidSediment-expected-results.txt" ) ) );
		    assertEquals( expectedResults.replaceAll( "\\s+", "" ), new String( Files.readAllBytes( validator.getValidationDir().toPath().resolve( "Sequence-invalid-sediment.tsv.gz.report" ) ), StandardCharsets.UTF_8 ).replaceAll( "\\s+", "" ));
		}
	}

	
	@Test public void 
	invalidEntryNumberStart() throws ValidationEngineException, IOException 
	{
	
		File file=WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-invalid-entrynumber-start-.tsv.gz");
		SequenceAssemblyWebinCli validator = getValidator(file, FileType.TSV);
		try
		{
		    validator.validate();
		    fail();
		} catch( WebinCliException wce )
		{
			assertEquals( getmessage( "tsv",file.getName(), validator.getValidationDir().getAbsolutePath() ),wce.getMessage());
		    String expectedResults = new String(Files.readAllBytes(Paths.get("src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-invalidEntrynumberStart-expected-results.txt")));
		    assertEquals( new String( Files.readAllBytes( validator.getValidationDir().toPath().resolve( "Sequence-invalid-entrynumber-start-.tsv.gz.report" ) ), StandardCharsets.UTF_8 ).replaceAll("\\s+", ""),
		                  expectedResults.replaceAll("\\s+", ""));
		}
	}


	@Test public void 
	nonAsciiCharacters() throws ValidationEngineException, IOException 
	{
	
		File file = WebinCliTestUtils.getFile( "uk/ac/ebi/ena/template/tsvfile/Sequence-non-ascii-characters.gz" );
		SequenceAssemblyWebinCli validator = getValidator( file, FileType.TSV );
		try
		{
		    validator.validate();
		    fail();
		} catch( WebinCliException wce )
		{
			assertEquals( getmessage( "tsv",file.getName(), validator.getValidationDir().getAbsolutePath() ),wce.getMessage());
		    String expectedResults = new String( Files.readAllBytes( Paths.get("src/test/resources/uk/ac/ebi/ena/template/tsvfile/Sequence-nonAsciiCharacters-expected-results.txt" ) ) );
		    assertEquals( new String( Files.readAllBytes( validator.getValidationDir().toPath().resolve( "Sequence-non-ascii-characters.gz.report" ) ), StandardCharsets.UTF_8 ).replaceAll("\\s+", ""),
		                  expectedResults.replaceAll( "\\s+", "" ).trim() );
		}

	}


	private SequenceAssemblyWebinCli 
	getValidator( File file, FileType fileType )

	{
		SubmissionOptions options = new SubmissionOptions();
		if(file!=null)
		{
			SubmissionFiles files = new SubmissionFiles();
			SubmissionFile SubmissionFile= new SubmissionFile(fileType,file);
			files.addFile(SubmissionFile);
			options.submissionFiles = Optional.of(files);
		}
		options.assemblyInfoEntry = Optional.of(new AssemblyInfoEntry());
		options.context =Optional.of(Context.sequence);
		options.isFixMode =true;
		options.isRemote =true;
		SequenceAssemblyWebinCli validator = new SequenceAssemblyWebinCli();
		validator.setTestMode( true );
		validator.setStudy( new Study() );
		validator.setSubmitDir(WebinCliTestUtils.createTempDir());
		validator.setSubmissionOptions(options);
		validator.setValidationDir( WebinCliTestUtils.createTempDir() );
		return validator;
	}
	
	private String getmessage(String fileType,String fileName,String reportDir)
	{
		return fileType+" file validation failed : "+fileName+", Please see the error report: "+ reportDir+File.separator+fileName+".report";
	}
}
