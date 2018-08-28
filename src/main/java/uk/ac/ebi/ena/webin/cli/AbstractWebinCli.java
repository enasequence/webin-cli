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

import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.submit.SubmissionBundle;
import uk.ac.ebi.ena.utils.FileUtils;

public abstract class 
AbstractWebinCli
{
    protected static final String VALIDATE_DIR = "validate";
    protected static final String SUBMIT_DIR   = "submit";
    protected static String REPORT_FILE_SUFFIX = ".report";
    
    private String name; 
    private WebinCliParameters parameters = new WebinCliParameters();
    private boolean test_mode;

    
    public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        setParameters( parameters );
    }
    
    
    public abstract boolean validate() throws ValidationEngineException;
    public abstract void prepareSubmissionBundle() throws IOException;
    public abstract File getSubmissionBundleFileName();
    public abstract File getValidationDir();
    
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
    
   
    protected void
    setParameters( WebinCliParameters parameters )
    {
        this.parameters = parameters;
    }
    
    
    protected File 
    createOutputSubdir( String...more ) throws WebinCliException
    {
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
