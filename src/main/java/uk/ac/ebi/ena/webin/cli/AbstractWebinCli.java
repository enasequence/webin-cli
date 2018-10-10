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

import org.apache.commons.lang.StringUtils;
import uk.ac.ebi.embl.api.validation.DefaultOrigin;
import uk.ac.ebi.embl.api.validation.Severity;
import uk.ac.ebi.embl.api.validation.ValidationEngineException;
import uk.ac.ebi.embl.api.validation.ValidationResult;
import uk.ac.ebi.ena.manifest.ManifestReader;
import uk.ac.ebi.ena.submit.ContextE;
import uk.ac.ebi.ena.submit.SubmissionBundle;

public abstract class 
AbstractWebinCli<T extends ManifestReader>
{
    protected static final String VALIDATE_DIR = "validate";
    protected static final String SUBMIT_DIR   = "submit";
    protected static final String REPORT_FILE_SUFFIX = ".report";
    private static final String SUBMISSION_BUNDLE = ".data";

    private String name; 
    private WebinCliParameters parameters = new WebinCliParameters();
    private boolean test_mode;

    private T manifestReader;

    private File validationDir;
    private File submitDir;

    private boolean fetchSample = true;
    private boolean fetchStudy = true;

    protected abstract T createManifestReader();

    public final void
    init( WebinCliParameters parameters )
    {
        this.parameters = parameters;
        this.manifestReader = createManifestReader();

        this.validationDir = WebinCli.createOutputDir(parameters, ".");

        File manifestFile = getParameters().getManifestFile();
        File reportFile = getReportFile(manifestFile.getName() );

        reportFile.delete();

        try
        {
            readManifest( getParameters().getInputDir().toPath(), manifestFile );

            if (!StringUtils.isBlank(manifestReader.getName())) {
                setName();
                this.validationDir = WebinCli.createOutputDir( parameters, String.valueOf( getContext() ), getName(), VALIDATE_DIR );
                this.submitDir = WebinCli.createOutputDir( parameters, String.valueOf( getContext() ), getName(), SUBMIT_DIR );
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
                WebinCliReporter.writeToConsole( manifestReader.getValidationResult() );
                WebinCliReporter.writeToFile( reportFile, manifestReader.getValidationResult() );
            }
        }

        if (manifestReader == null || !manifestReader.getValidationResult().isValid()) {
            throw WebinCliException.createUserError( "Invalid manifest file. Please see the error report: " + reportFile.getPath() );
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
    getReportFile( String filename )
    {
        return WebinCli.getReportFile( getValidationDir(), filename, REPORT_FILE_SUFFIX );
    }

    public SubmissionBundle
    getSubmissionBundle()
    {
        try( ObjectInputStream os = new ObjectInputStream( new FileInputStream(SUBMISSION_BUNDLE) ) )
        {
            SubmissionBundle sb = (SubmissionBundle) os.readObject();
            ValidationResult result = sb.validate( new ValidationResult( new DefaultOrigin( String.valueOf(SUBMISSION_BUNDLE) ) ) );

            WebinCliReporter.writeToFile( WebinCliReporter.getDefaultReport(), result );

            if( result.count(Severity.INFO) > 0 ) {
                // Submission bundle was invalid.
                WebinCliReporter.writeToFile( WebinCliReporter.getDefaultReport(),
                        Severity.INFO, "Submission requires re-validation." );
                return null;
            }
            
            return sb;
        } catch( ClassNotFoundException | IOException e )
        {
            // Submission bundle could not be read.
            WebinCliReporter.writeToFile( WebinCliReporter.getDefaultReport(),
                    Severity.INFO, "Submission has not been validated previously." );
            return null;
        }
    }

    
    protected void
    setSubmissionBundle( SubmissionBundle submissionBundle )
    {
        try( ObjectOutputStream os = new ObjectOutputStream( new FileOutputStream(SUBMISSION_BUNDLE) ) )
        {
            os.writeObject( submissionBundle );
            os.flush();
        } catch( IOException e )
        {
            throw WebinCliException.createSystemError( "Unable to write file: " + SUBMISSION_BUNDLE);
        }
    }


    public WebinCliParameters
    getParameters()
    {
        return this.parameters;
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
