package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationResult;

public abstract class 
AbstractWebinCli
{
    protected static final String VALIDATE_SUCCESS = "The submission has been validated successfully. ";
    protected static final String VALIDATE_SYSTEM_ERROR = "Submission validation failed because of a system error. ";
    protected static final String VALIDATE_DIR = "validate";
    protected static final String SUBMIT_DIR   = "submit";
    
    
    private SubmissionBundle   submissionBundle;
    private ValidationResult   validationResult;
    private WebinCliParameters parameters = new WebinCliParameters();

    
    public void 
    init( WebinCliParameters parameters )
    {
        this.parameters = parameters;
    }
    
    
    public abstract boolean validate() throws ValidationEngineException;


    public SubmissionBundle
    getSubmissionBundle()
    {
        return submissionBundle;
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
}
