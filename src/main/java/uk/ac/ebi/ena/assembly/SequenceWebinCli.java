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

import static java.util.stream.Collectors.toCollection;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import uk.ac.ebi.embl.api.entry.feature.SourceFeature;
import uk.ac.ebi.embl.api.entry.genomeassembly.AssemblyInfoEntry;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFile.FileType;
import uk.ac.ebi.embl.api.validation.submission.SubmissionFiles;
import uk.ac.ebi.embl.api.validation.submission.SubmissionOptions;
import uk.ac.ebi.ena.manifest.ManifestReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFile;
import uk.ac.ebi.ena.submit.SubmissionBundle.SubmissionXMLFileType;
import uk.ac.ebi.ena.utils.FileUtils;
import uk.ac.ebi.ena.webin.cli.AbstractWebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCli;
import uk.ac.ebi.ena.webin.cli.WebinCliException;

public abstract class 
SequenceWebinCli<T extends ManifestReader> extends AbstractWebinCli<T>
{
	private static final String DIGEST_NAME = "MD5";
	protected final static String ANALYSIS_XML = "analysis.xml";

	SubmissionOptions submissionOptions;
	private Study  study;
	private Sample sample;
	private SourceFeature source;
	protected AssemblyInfoEntry assembly_info;

	protected abstract boolean validateInternal() throws ValidationEngineException;



	public void
	setStudy( Study study )
	{ 
		this.study = study;
	}

	public void
	setSample( Sample sample )
	{
		this.sample = sample;
	}


	public Study
	getStudy()
	{ 
		return this.study;
	}


	public Sample
	getSample()
	{
		return this.sample;
	}

	public SourceFeature getSource() {
		return source;
	}

	public void setSource(SourceFeature source) {
		this.source = source;
	}


	public void 
	setInputDir( File inputDir )
	{
		getParameters().setInputDir( inputDir );
	}


	public File
	getInputDir()
	{
		return getParameters().getInputDir();
	}


	public AssemblyInfoEntry
	getAssemblyInfo()
	{
		return assembly_info;
	}


	public void
	setAssemblyInfo( AssemblyInfoEntry assembly_info )
	{
		this.assembly_info = assembly_info;
	}

	protected Element
	createTextElement( String name, String text )
	{
		Element e = new Element( name );
		e.setText( text );
		return e;
	}
	
	public SubmissionOptions getSubmissionOptions() {
		return submissionOptions;
	}

	public void setSubmissionOptions(SubmissionOptions submissionOptions) {
		this.submissionOptions = submissionOptions;
	}


	abstract Element makeAnalysisType( AssemblyInfoEntry entry );


	String
	createAnalysisXml( List<Element> fileList, AssemblyInfoEntry entry, String centerName  ) 
	{
		try 
		{
			String full_name = getContext().getTitle( getName() );
			//String type      = getContext().getType(); 

			Element analysisSetE = new Element( "ANALYSIS_SET" );
			Element analysisE = new Element( "ANALYSIS" );
			analysisSetE.addContent( analysisE );

			Document doc = new Document( analysisSetE );
			analysisE.setAttribute( "alias", getAlias() );

			if( null != centerName && !centerName.isEmpty() )
				analysisE.setAttribute( "center_name", centerName );

			analysisE.addContent( new Element( "TITLE" ).setText( full_name ) );
			Element studyRefE = new Element( "STUDY_REF" );
			analysisE.addContent( studyRefE );
			studyRefE.setAttribute( "accession", entry.getStudyId() );
			if( entry.getBiosampleId() != null && !entry.getBiosampleId().isEmpty() )
			{
				Element sampleRefE = new Element( "SAMPLE_REF" );
				analysisE.addContent( sampleRefE );
				sampleRefE.setAttribute( "accession", entry.getBiosampleId() );
			}
			Element analysisTypeE = new Element( "ANALYSIS_TYPE" );
			analysisE.addContent(analysisTypeE);
			Element typeE = makeAnalysisType( entry );
			analysisTypeE.addContent( typeE );

			Element filesE = new Element( "FILES" );
			analysisE.addContent( filesE );

			for( Element e: fileList )
				filesE.addContent( e );

			XMLOutputter xmlOutput = new XMLOutputter();
			xmlOutput.setFormat( Format.getPrettyFormat() );
			StringWriter stringWriter = new StringWriter();
			xmlOutput.output( doc, stringWriter );
			return stringWriter.toString();

		} catch( IOException e ) 
		{
			throw WebinCliException.createSystemError( e.getMessage() );
		}
	}


	protected Element
	createfileElement( Path uploadDir, File file, String file_type )
	{
		try
		{
			return createfileElement( String.valueOf( uploadDir.resolve( extractSubpath( getParameters().getInputDir(), file ) ) ).replaceAll( "\\\\+", "/" ), 
					String.valueOf( file_type ), 
					DIGEST_NAME, 
					FileUtils.calculateDigest( DIGEST_NAME, file ) );
		}catch( IOException | NoSuchAlgorithmException e )
		{
			throw new RuntimeException( e );
		}
	}


	private Element
	createfileElement( String file_path, String file_type, String digest, String checksum )
	{
		Element fileE = new Element( "FILE" );
		fileE.setAttribute( "filename", file_path );
		fileE.setAttribute( "filetype", String.valueOf( file_type ) );
		fileE.setAttribute( "checksum_method", digest );
		fileE.setAttribute( "checksum", checksum );
		return fileE;
	}


	protected String
	extractSubpath( File inputDir, File file ) throws IOException
	{
		return file.toPath().startsWith( inputDir.toPath() ) ? inputDir.toPath().relativize( file.toPath() ).toString() : file.getName();
	}

	@Override public boolean
	validate() throws ValidationEngineException
	{
		if( !FileUtils.emptyDirectory( getValidationDir() ) )
			throw WebinCliException.createSystemError( "Unable to empty directory " + getValidationDir() );

		if( !FileUtils.emptyDirectory( getSubmitDir() ) )
			throw WebinCliException.createSystemError( "Unable to empty directory " + getSubmitDir() );

		return validateInternal();
	}


	protected List<File>
	getUploadFiles() throws IOException
	{
		List<File> uploadFiles = new ArrayList<File>();
		uploadFiles.addAll(getFilesToUpload(FileType.CHROMOSOME_LIST));
		uploadFiles.addAll(getFilesToUpload(FileType.UNLOCALISED_LIST));
		uploadFiles.addAll(getFilesToUpload(FileType.FASTA));
		uploadFiles.addAll(getFilesToUpload(FileType.AGP));
		uploadFiles.addAll(getFilesToUpload(FileType.FLATFILE));
		uploadFiles.addAll(getFilesToUpload(FileType.TSV));

		return uploadFiles;
	/*	return new ArrayList<File>()
		{
			{
				addAll(getFilesToUpload(FileType.CHROMOSOME_LIST));
				addAll(getFilesToUpload(FileType.UNLOCALISED_LIST));
				addAll(getFilesToUpload(FileType.FASTA));
				addAll(getFilesToUpload(FileType.AGP));
				addAll(getFilesToUpload(FileType.FLATFILE));
				addAll(getFilesToUpload(FileType.TSV));
			}

		};*/

	}


	protected List<Element>
	getXMLFiles( Path uploadDir ) throws IOException
	{
		
		 List<Element> eList = new ArrayList<>();

            getFilesToUpload(FileType.CHROMOSOME_LIST).forEach( file -> eList.add( createfileElement( uploadDir, file, "chromosome_list" ) ) );
	       getFilesToUpload(FileType.UNLOCALISED_LIST).forEach( file -> eList.add( createfileElement( uploadDir, file, "unlocalised_list" ) ) );
	        getFilesToUpload(FileType.FASTA).forEach( file -> eList.add( createfileElement( uploadDir, file, "fasta" ) ) );
	        getFilesToUpload(FileType.FLATFILE).forEach( file -> eList.add( createfileElement( uploadDir, file, "flatfile" ) ) );
	        getFilesToUpload(FileType.AGP).forEach( file -> eList.add( createfileElement( uploadDir, file, "agp" ) ) );
	        getFilesToUpload(FileType.AGP).forEach( file -> eList.add( createfileElement( uploadDir, file, "tab" ) ) );

	        return eList;
	}


	@Override public void
	prepareSubmissionBundle() throws WebinCliException
	{
		try
		{
			Path uploadDir = Paths.get( String.valueOf( getContext() ), WebinCli.getSafeOutputDir(getName()) );

			List<File> uploadFileList = getUploadFiles();
			List<Element> eList = getXMLFiles( uploadDir );

			String xml = createAnalysisXml( eList, getAssemblyInfo(), getParameters().getCenterName() );

			Path analysisFile = getSubmitDir().toPath().resolve( ANALYSIS_XML );

			Files.write( analysisFile, xml.getBytes( StandardCharsets.UTF_8 ), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC );

			setSubmissionBundle( new SubmissionBundle( getSubmitDir(), 
					uploadDir.toString(), 
					uploadFileList, 
					Arrays.asList( new SubmissionXMLFile( SubmissionXMLFileType.ANALYSIS, analysisFile.toFile(), FileUtils.calculateDigest( "MD5", analysisFile.toFile() ) ) ), 
					getParameters().getCenterName() ) );   
		} catch( IOException | NoSuchAlgorithmException e )
		{
			throw WebinCliException.createSystemError( e.getMessage() );
		}        
	}

	private List<File> getFilesToUpload(FileType fileType)
	{
		List<File> files=new ArrayList<File>();
		if(getSubmissionOptions()!=null &&getSubmissionOptions().submissionFiles.isPresent())
		{
			for(SubmissionFile submissionFile : getSubmissionOptions().submissionFiles.get().getFiles())
			{
				if(fileType!=null && fileType.equals(submissionFile.getFileType()))
					files.add(submissionFile.getFile());
			}

		}
		return files;
	}
}
