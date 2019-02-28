package uk.ac.ebi.ena.submit;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;
import uk.ac.ebi.ena.webin.cli.logger.ValidationMessageLogger;

public class 
SubmissionBundleHelper 
{
    final private String submission_bundle_path;

    private static final Logger log = LoggerFactory.getLogger(SubmissionBundleHelper.class);

    public
    SubmissionBundleHelper( String submission_bundle_path )
    {
        this.submission_bundle_path = submission_bundle_path;
    }
    
    
    public SubmissionBundle
    read()
    { 
        return read( null );
    }
    
    
    public SubmissionBundle
    read( String manifest_md5 )
    {
        try( ObjectInputStream os = new ObjectInputStream( new FileInputStream( submission_bundle_path ) ) )
        {
            SubmissionBundle sb = (SubmissionBundle) os.readObject();
            
            if( null != manifest_md5 && !manifest_md5.equals( sb.getManifestMd5() ) )
            {
                log.info(WebinCliMessage.Bundle.REVALIDATE_SUBMISSION.format());
                return null;
            }

            ValidationResult result = sb.validate( new ValidationResult( new DefaultOrigin(submission_bundle_path) ) );
            ValidationMessageLogger.log(result);

            if( result.count(Severity.INFO) > 0 ) 
            {
                log.info(WebinCliMessage.Bundle.REVALIDATE_SUBMISSION.format());
                return null;
            }
            
            return sb;
            
        } catch( ClassNotFoundException | IOException e )
        {
            // Submission bundle could not be read.
            log.info(WebinCliMessage.Bundle.VALIDATE_SUBMISSION.format());
            return null;
        }

    }
    
    
    public void
    write( SubmissionBundle sb )
    {
        try( ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream( submission_bundle_path ) ) )
        {
            os.writeObject( sb );
            os.flush();
        } catch( IOException e )
        {
            throw WebinCliException.systemError(WebinCliMessage.Bundle.FILE_ERROR.format(submission_bundle_path ));
        }
    }
    
}
