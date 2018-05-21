package uk.ac.ebi.ena.webin.cli;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.sample.Sample;
import uk.ac.ebi.ena.study.Study;
import uk.ac.ebi.ena.utils.FileUtils;

public abstract class 
AbstractWebinCli
{
    protected static final String VALIDATE_SUCCESS = "The submission has been validated successfully. ";
    protected static final String VALIDATE_SYSTEM_ERROR = "Submission validation failed because of a system error. ";
    protected static final String VALIDATE_DIR = "validate";
    protected static final String SUBMIT_DIR   = "submit";
    
    private String name; 
    private SubmissionBundle   submissionBundle;
    private ValidationResult   validationResult;
    private WebinCliParameters parameters = new WebinCliParameters();

    
    public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        this.parameters = parameters;
    }
    
    
    public abstract boolean validate() throws ValidationEngineException;
    public abstract void prepareSubmissionBundle() throws IOException;

    public SubmissionBundle
    getSubmissionBundle()
    {
        return submissionBundle;
    }

    
    protected void
    setSubmissionBundle( SubmissionBundle submissionBundle )
    {
        this.submissionBundle = submissionBundle;
    }
    

    public ValidationResult
    getValidationResult()
    {
        return validationResult;
    }


    public WebinCliParameters
    getParameters()
    {
        return this.parameters;
    }
    
    
    protected File 
    createOutputSubdir( String...more ) throws Exception 
    {
        Path p = Paths.get( getParameters().getOutputDir().getPath(), more );
        File reportDirectory = p.toFile();
        
        if( reportDirectory.exists() )
        {
            FileUtils.emptyDirectory( reportDirectory );
        } else if( !reportDirectory.mkdirs() ) 
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
}
