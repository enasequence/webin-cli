/*
 * Copyright 2018-2019 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.submit;

import java.io.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationMessage;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationOrigin;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class 
SubmissionBundleHelper 
{
    final private File submissionBundleFile;

    private static final Logger log = LoggerFactory.getLogger(SubmissionBundleHelper.class);

    public
    SubmissionBundleHelper( String submissionBundleFile)
    {
        this.submissionBundleFile = new File(submissionBundleFile);
    }

    public
    SubmissionBundleHelper( File submissionBundleFile)
    {
        this.submissionBundleFile = submissionBundleFile;
    }

    public SubmissionBundle
    read()
    { 
        return read( null );
    }
    
    
    public SubmissionBundle
    read( String manifestMd5 )
    {
        try( ObjectInputStream os = new ObjectInputStream( new FileInputStream(submissionBundleFile) ) )
        {
            SubmissionBundle sb = (SubmissionBundle) os.readObject();
            
            if( null != manifestMd5 && !manifestMd5.equals( sb.getManifestMd5() ) )
            {
                log.info(WebinCliMessage.SUBMISSION_BUNDLE_REVALIDATE_SUBMISSION.text());
                return null;
            }

            ValidationResult result = new ValidationResult(
                    new ValidationOrigin("submission bundle", submissionBundleFile.getAbsolutePath()));
            sb.validate(result);

            if(result.count(ValidationMessage.Severity.INFO) > 0) // TODO: potentially dangerous comparison
            {
                log.info(WebinCliMessage.SUBMISSION_BUNDLE_REVALIDATE_SUBMISSION.text());
                return null;
            }
            
            return sb;
            
        } catch( ClassNotFoundException | IOException e )
        {
            // Submission bundle could not be read.
            log.info(WebinCliMessage.SUBMISSION_BUNDLE_VALIDATE_SUBMISSION.text());
            return null;
        }

    }
    
    
    public void
    write( SubmissionBundle sb )
    {
        try( ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream(submissionBundleFile) ) )
        {
            os.writeObject( sb );
            os.flush();
        } catch( IOException ex )
        {
            throw WebinCliException.systemError(ex, WebinCliMessage.SUBMISSION_BUNDLE_FILE_ERROR.format(submissionBundleFile));
        }
    }
    
}
