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

package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.manifest.ManifestReader;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.utils.FileUtils;

public abstract class 
AbstractWebinCli<T extends ManifestReader>
{
    protected static final String VALIDATE_DIR = "validate";
    protected static final String SUBMIT_DIR   = "submit";
    protected static String REPORT_FILE_SUFFIX = ".report";

    private String name; 
    private WebinCliParameters parameters = new WebinCliParameters();
    private boolean test_mode;

    private final T manifestReader;

    private File validationDir;
    private File submitDir;

    private boolean fetchSample = true;
    private boolean fetchStudy = true;

    public AbstractWebinCli(T manifestReader) {
        this.manifestReader = manifestReader;
    }

    public final void
    init( WebinCliParameters parameters )
    {
        this.parameters = parameters;
        this.validationDir = createOutputSubdir(".");

        File manifestFile = getParameters().getManifestFile();
        File reportFile = getReportFile( "", manifestFile.getName() );

        reportFile.delete();

        try
        {
            readManifest( getParameters().getInputDir().toPath(), manifestFile );

            if (!StringUtils.isBlank(manifestReader.getName())) {
                setName();
                this.validationDir = createOutputSubdir( String.valueOf( getContext() ), getName(), VALIDATE_DIR );
                this.submitDir = createOutputSubdir( String.valueOf( getContext() ), getName(), SUBMIT_DIR );
            }
            else {
                throw WebinCliException.createSystemError( "Missing submission name" );
            }
        }
        catch( WebinCliException e )
        {
            throw e;
        }
        catch( Throwable t )
        {
            throw WebinCliException.createSystemError( "Failed to initialise validator" );
        }
        finally {
            setName();
            if (manifestReader != null && !manifestReader.getValidationResult().isValid()) {
                FileUtils.writeReport( reportFile, manifestReader.getValidationResult() );
            }
        }

        if (manifestReader == null || !manifestReader.getValidationResult().isValid()) {
            throw WebinCliException.createUserError( "Invalid manifest file: " + reportFile.getPath() );
        }
    }

    private void setName() {
        if (manifestReader.getName() != null) {
            this.name = manifestReader.getName().trim().replaceAll("\\s+", "_");
        }
    }

    public abstract ContextE getContext();

    /**
     * Reads the manifest file and returns the submission name.
     */
    public abstract void readManifest(Path inputDir, File manifestFile);

    public T getManifestReader() {
        return manifestReader;
    }

    public File getValidationDir() {
        return validationDir;
    }

    public void setValidationDir(File validationDir) {
        this.validationDir = validationDir;
    }

    public File getSubmitDir() {
        return submitDir;
    }

    public void setSubmitDir(File submitDir) {
        this.submitDir = submitDir;
    }

    public abstract boolean validate() throws ValidationEngineException;
    public abstract void prepareSubmissionBundle() throws IOException;

    private File
    getSubmissionBundleFileName()
    {
        return new File( getSubmitDir(), "validate.receipt" );
    }

    public boolean
    isFetchStudy()
    {
        return fetchStudy;
    }


    public boolean
    isFetchSample()
    {
        return fetchSample;
    }


    public void
    setFetchSample(boolean fetchSample)
    {
        this.fetchSample = fetchSample;
    }


    public void
    setFetchStudy(boolean fetchStudy)
    {
        this.fetchStudy = fetchStudy;
    }

    protected File
    getReportFile( String prefix, String filename )
    {
    	try
    	{
    		return getReportFile( getValidationDir(), prefix, filename );
    	} catch( RuntimeException re )
    	{
    		return getReportFile( getParameters().getOutputDir(), prefix, filename );
    	}
    }
    
    
    private File
    getReportFile( File output_location, String prefix, String filename )
    {
        if( null == getValidationDir() )
        	throw new RuntimeException( "Validation dir cannot be null" );
        
        if( getValidationDir().isFile() )
            throw new RuntimeException( "Validation dir cannot be file" );
        
        return new File( getValidationDir(), /*filetype + "-" +*/ Paths.get( filename ).getFileName().toString() + REPORT_FILE_SUFFIX ); 
    }
    
    
    //TODO consider relative paths in file names
    public SubmissionBundle
    getSubmissionBundle()
    {
        try( ObjectInputStream os = new ObjectInputStream( new FileInputStream( getSubmissionBundleFileName() ) ) )
        {
            SubmissionBundle sb = (SubmissionBundle) os.readObject();
            ValidationResult result = sb.validate( new ValidationResult( new DefaultOrigin( String.valueOf( getSubmissionBundleFileName() ) ) ) );
            FileUtils.writeReport( getParameters().getSystemLogFile(), result );
            
            if( !result.isValid() ) 
                throw WebinCliException.createSystemError( "Unable to validate " + getSubmissionBundleFileName() );
            
            return sb;
        } catch( ClassNotFoundException | IOException e )
        {
            FileUtils.writeReport( getParameters().getSystemLogFile(), Severity.ERROR, "Unable to read " + getSubmissionBundleFileName() + " " + e.getMessage() );
            throw WebinCliException.createSystemError( "Unable to read " + getSubmissionBundleFileName() );
        }
    }

    
    protected void
    setSubmissionBundle( SubmissionBundle submissionBundle )
    {
        try( ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream( getSubmissionBundleFileName() ) ) )
        {
            os.writeObject( submissionBundle );
            os.flush();
        } catch( IOException e )
        {
            throw WebinCliException.createSystemError( "Unable to write " + getSubmissionBundleFileName() );
        }
    }

    
    public WebinCliParameters
    getParameters()
    {
        return this.parameters;
    }




    
    private File
    createOutputSubdir( String...more ) throws WebinCliException
    {
        if (getParameters().getOutputDir() == null) {
            throw WebinCliException.createSystemError( "Missing output directory" );
        }
        Path p = Paths.get( getParameters().getOutputDir().getPath(), more );
        File reportDirectory = p.toFile();
        
        if( !reportDirectory.exists() && !reportDirectory.mkdirs() ) 
        {
            throw WebinCliException.createSystemError( "Unable to create directory: " + reportDirectory.getPath() );
        }
        
        return reportDirectory;
    }

    
    
    protected Study 
    fetchStudy( String study_id, boolean test_mode ) 
    {
        return Study.getStudy( study_id, getParameters().getUsername(), getParameters().getPassword(), test_mode );
    }

    
    protected Sample 
    fetchSample( String sample_id, boolean test_mode ) 
    {
        return Sample.getSample( sample_id, getParameters().getUsername(), getParameters().getPassword(), test_mode );
    }


    public String
    getName()
    {
        return name;
    }


    public void
    setName( String name )
    {
        this.name = name;
    }


    public void 
    setTestMode( boolean test_mode )
    {
        this.test_mode = test_mode;
    }
    
    
    public boolean 
    getTestMode()
    {
        return this.test_mode;
        
    }
}
