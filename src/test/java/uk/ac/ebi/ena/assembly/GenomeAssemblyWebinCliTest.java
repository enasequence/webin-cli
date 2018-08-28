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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;

public class GenomeAssemblyWebinCliTest {
	@Before public void
	before()
	{
		Locale.setDefault( Locale.UK );
		//-Duser.country=US -Duser.language=en
	}
	
	
	@Test public void 
	testAssemblyWithnoInfo() throws Exception 
	{
	    URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaOnly.txt" );
        File file = new File( url.getFile() );

        Sample sample = new Sample();
		sample.setOrganism( "Quercus robur" );
		Study study = new Study();
		GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
		validator.setInputDir( file.getParentFile() );
		validator.setValidationDir( createOutputFolder() );
		validator.setSubmitDir( createOutputFolder() );
		validator.defineFileTypes( file );
		validator.setSample( sample );
		validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}


	@Test public void 
	testAssemblywithOnlyInvalidInfo() throws Exception 
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithAssemblyinfoOnly.txt" );
        File file = new File( url.getFile() );
        Sample sample = new Sample();
		sample.setOrganism( "Quercus robur" );
		Study study = new Study();

        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}
	

	@Test public void 
	testAssemblyFastaInfo() throws Exception 
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaInfo.txt" );
        File file = new File( url.getFile() );
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}


	@Test public void 
	testAssemblyFlatFileInfo() throws Exception	
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFlatFileInfo.txt" );
        File file = new File( url.getFile() );
		List<String> locusTagsList = new ArrayList<>();
		locusTagsList.add("SPLC1");
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
		study.setLocusTagsList(locusTagsList);
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}
	

	@Test public void 
	testAssemblywithUnlocalisedList() throws Exception	
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithUnlocalisedListInfo.txt" );
        File file = new File( url.getFile() );
 		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}

	
	@Test public void 
	testAssemblywithAGP() throws Exception 
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaAGPinfo.txt" );
        File file = new File( url.getFile() );
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}
	
	
	@Test public void 
	testAssemblywithChromosomeAGP() throws Exception 
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithChromosomeFastaAGPinfo.txt" );
        File file = new File( url.getFile() );
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}
	

	@Test public void 
    testFastaNoValidEntries() throws Exception 
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setStudy( new Study() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        Assert.assertTrue( !validator.validateFastaFiles( validator.getValidationProperties(), 
                                                          Arrays.asList( new File( GenomeAssemblyWebinCliTest.class
                                                                                              .getClassLoader()
                                                                                              .getResource( "uk/ac/ebi/ena/assembly/genome/ERZ480053/PYO97_7.fa.gz" )
                                                                                              .getFile() ) ) ) );
    }


	@Test public void 
    testChromosomeListNoValidEntries() throws Exception 
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setStudy( new Study() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        Assert.assertTrue( !validator.validateChromosomeList( validator.getValidationProperties(), 
                                                              new File( GenomeAssemblyWebinCliTest.class
                                                                                                  .getClassLoader()
                                                                                                  .getResource( "uk/ac/ebi/ena/assembly/genome/ERZ496213/RUG553.fa.chromlist.gz" )
                                                                                                  .getFile() ) ) );
    }
    

    @Test public void 
    testERZ449652() throws Exception 
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/genome/ERZ449652/ERZ449652.manifest" );
        File file = new File( url.getFile() );
        Sample sample = new Sample();
        sample.setOrganism( "Quercus robur" );
        
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( new Study() );
        Assert.assertTrue( validator.validate() );
    }

    
    @Test public void 
    testERZ092580() throws Exception 
    {
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/genome/ERZ092580/ERZ092580.manifest" );
        File file = new File( url.getFile() );
        Sample sample = new Sample();
        sample.setOrganism( "Quercus robur" );
        
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( new Study() );
        Assert.assertTrue( !validator.validate() );
    }
    
    
    private File
    createOutputFolder() throws IOException
    {
        File output = File.createTempFile( "test", "test" );
        Assert.assertTrue( output.delete() );
        Assert.assertTrue( output.mkdirs() );
        return output;
    }

    
    @Test public void 
    testAssemblywithInvalidAGP() throws Exception 
    {
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFastaInvalidAGPinfo.txt" );
        File file = new File( url.getFile() );
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( !validator.validate() );
	}
	
    
	@Test public void
	testAssemblywithFlatfileandGP() throws Exception 
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithFlatfilevalidAGPinfo.txt" );
        File file = new File( url.getFile() );
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue( validator.validate() );
	}
	
	
	@Test public void 
	testAssemblywithSequencelessChromosomeAGP() throws Exception 
	{
        URL url = GenomeAssemblyWebinCliTest.class.getClassLoader().getResource( "uk/ac/ebi/ena/assembly/manifestwithSequencelessChromosomeAGPinfo.txt" );
        File file = new File( url.getFile() );
		Sample sample = new Sample();
		sample.setOrganism("Quercus robur");
		Study study = new Study();
        GenomeAssemblyWebinCli validator = new GenomeAssemblyWebinCli( true );
        validator.setInputDir( file.getParentFile() );
        validator.setValidationDir( createOutputFolder() );
        validator.setSubmitDir( createOutputFolder() );
        validator.defineFileTypes( file );
        validator.setSample( sample );
        validator.setStudy( study );
		Assert.assertTrue(!validator.validate() );
	}
}
