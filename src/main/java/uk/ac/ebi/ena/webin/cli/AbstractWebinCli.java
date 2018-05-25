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
    protected static final String VALIDATE_SUCCESS = "The submission has been validated successfully. ";
    protected static final String VALIDATE_SYSTEM_ERROR = "Submission validation failed because of a system error. ";
    protected static final String VALIDATE_DIR = "validate";
    protected static final String SUBMIT_DIR   = "submit";
    
    
    private String name; 
    private ValidationResult   validationResult;
    private WebinCliParameters parameters = new WebinCliParameters();

    
    public void 
    init( WebinCliParameters parameters ) throws ValidationEngineException
    {
        this.parameters = parameters;
    }
    
    
    public abstract boolean validate() throws ValidationEngineException;
    public abstract void prepareSubmissionBundle() throws IOException;
    public abstract File getSubmissionBundleFileName();

    
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
}
