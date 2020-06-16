package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.ena.webin.cli.validator.message.source.MessageFormatSource;

public enum WebinCliMessage implements MessageFormatSource {

    CLI_CURRENT_VERSION("Your application version is {0}"),
    CLI_NEW_VERSION("A new application version is available. Please download the latest version {0} from https://github.com/enasequence/webin-cli/releases"),
    CLI_UNSUPPORTED_VERSION("Your application version is no longer supported. The minimum supported version is {0}. Please download the latest version {1} from https://github.com/enasequence/webin-cli/releases"),
    CLI_EXPIRYING_VERSION("Your application version will not be supported after {0}. The minimum supported version will be {1}. Please download the latest version {2} from https://github.com/enasequence/webin-cli/releases"),
    CLI_VALIDATE_SUCCESS("The submission has been validated successfully."),
    CLI_UPLOAD_SUCCESS("Files have been uploaded to webin2.ebi.ac.uk. "),
    CLI_INPUT_PATH_NOT_DIR("Input dir path does not represent folder: \"{0}\"."),
    CLI_OUTPUT_PATH_NOT_DIR("Output dir path does represent folder: \"{0}\"."),
    CLI_VALIDATE_USER_ERROR("Submission validation failed because of a user error: {0}. Please check validation reports for further information: {1}"),
    CLI_VALIDATE_SYSTEM_ERROR("Submission validation failed because of a system error: {0}. Please check validation reports for further information: {1}"),
    CLI_VALIDATE_USER_ERROR_EX("Submission validation failed because of a user error. Please check validation reports for further information: {0}"),
    CLI_INVALID_REPORT_DIR_ERROR("invalid report directory: {0}"),
    CLI_MISSING_OUTPUT_DIR_ERROR("Missing output directory."),
    CLI_CREATE_DIR_ERROR("Unable to create directory: {0}"),
    CLI_UPLOAD_ERROR("Failed to upload files to webin.ebi.ac.uk because of a {0}. "),
    CLI_SUBMIT_ERROR("The submission has failed because of a {0}. "),
    CLI_AUTHENTICATION_ERROR("Invalid submission account user name or password. Please try enclosing your password in single quotes."),

    EXECUTOR_INIT_ERROR("Failed to initialise validator. {0}"),
    EXECUTOR_EMPTY_DIRECTORY_ERROR("Unable to empty directory {0}"),

    SUBMISSION_BUNDLE_REVALIDATE_SUBMISSION("Submission requires re-validation."),
    SUBMISSION_BUNDLE_FILE_ERROR("Unable to create submission bundle file: {0}"),
    SUBMISSION_BUNDLE_VALIDATE_SUBMISSION("Submission has not been validated previously."),

    FTP_UPLOAD_DIR_ERROR("Missing upload directory. Failed to upload files to webin.ebi.ac.uk using FTP."),
    FTP_CONNECT_ERROR("Failed to connect to webin.ebi.ac.uk using FTP."),
    FTP_CREATE_DIR_ERROR("Failed to create directory \"{0}\" in webin.ebi.ac.uk using FTP."),
    FTP_CHANGE_DIR_ERROR("Failed to access directory \"{0}\" in webin.ebi.ac.uk using FTP."),
    FTP_UPLOAD_ERROR("Failed to upload \"{0}\" file to webin.ebi.ac.uk using FTP."),
    FTP_SERVER_ERROR("Failed to upload files to webin.ebi.ac.uk using FTP."),

    ASCP_UPLOAD_ERROR("Failed to upload files to webin.ebi.ac.uk using Aspera."),

    SUBMIT_SERVICE_SUCCESS("The submission has been completed successfully. The following {0} accession was assigned to the submission: {1}"),
    SUBMIT_SERVICE_SUCCESS_NOACC("The submission has been completed successfully. No accession was assigned to the {0} submission. Please contact the helpdesk."),
    SUBMIT_SERVICE_SUCCESS_TEST("The TEST submission has been completed successfully. This was a TEST submission and no data was submitted. The following {0} accession was assigned to the submission: {1}"),
    SUBMIT_SERVICE_SUCCESS_TEST_NOACC("The TEST submission has been completed successfully. This was a TEST submission and no data was submitted. No accession was assigned to the {0} submission."),
    SUBMIT_SERVICE_SYSTEM_ERROR("A server error occurred when attempting to submit."),
    IGNORE_ERRORS_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving ignore error information."),
    VERSION_SERVICE_SYSTEM_ERROR("A server error occurred when checking application version."),
    SAMPLE_SERVICE_VALIDATION_ERROR("Unknown sample {0} or the sample cannot be referenced by your submission account. Samples must be submitted before they can be referenced in the submission."),
    SAMPLE_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving sample {0} information."),
    STUDY_SERVICE_VALIDATION_ERROR("Unknown study {0} or the study cannot be referenced by your submission account. Studies must be submitted before they can be referenced in the submission."),
    STUDY_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving study {0} information."),
    RUN_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving analysis {0} information."),
    RUN_SERVICE_VALIDATION_ERROR("Unknown run {0} or the run cannot be referenced by your submission account. Runs must be submitted before they can be referenced in the submission."),
    ANALYSIS_SERVICE_SYSTEM_ERROR("A server error occurred when retrieving analysis {0} information."),
    ANALYSIS_SERVICE_VALIDATION_ERROR("Unknown analysis {0} or the analysis cannot be referenced by your submission account. Analyses must be submitted before they can be referenced in the submission."),

    MANIFEST_READER_MANIFEST_FILE_READ_ERROR("Could not read manifest file: \"{0}\"."),
    MANIFEST_READER_INFO_FILE_READ_ERROR("Could not read info file: \"{0}\"."),
    MANIFEST_READER_INVALID_MANIFEST_FILE_ERROR("Invalid manifest file. Please see the error report file \"{0}\"."),
    MANIFEST_READER_UNKNOWN_FIELD_ERROR("Unknown field: {0}."),
    MANIFEST_READER_INVALID_FILE_FIELD_ERROR("Could not read data file: \"{0}\"."),
    MANIFEST_READER_INVALID_POSITIVE_INTEGER_ERROR("Invalid field value. Non-negative integer expected."),
    MANIFEST_READER_INVALID_POSITIVE_FLOAT_ERROR("Invalid field value. Non-negative float expected."),
    MANIFEST_READER_MISSING_MANDATORY_FIELD_ERROR("Missing mandatory field {0}."),
    MANIFEST_READER_TOO_MANY_FIELDS_ERROR("Field {0} should not appear more than {1} times."),
    MANIFEST_READER_NO_DATA_FILES_ERROR("No data files have been specified. Expected data files are: {0}."),
    MANIFEST_READER_INVALID_FILE_GROUP_ERROR("An invalid set of files has been specified{1}. Expected data files are: {0}."),
    MANIFEST_READER_INVALID_FILE_COMPRESSION_ERROR("Failed to uncompress file: \"{0}\". The file must be compressed with {1}."),
    MANIFEST_READER_MISSING_ADDRESS_OR_AUTHOR_ERROR("Please provide both address and author details or neither."),

    FILE_SUFFIX_PROCESSOR_ERROR("Invalid {0} file suffix: \"{1}\". Valid file suffixes are: {2}."),
    CV_FIELD_PROCESSOR_ERROR("Invalid {0} field value: \"{1}\". Valid values are: {2}."),
    CV_FIELD_PROCESSOR_FIELD_VALUE_CORRECTED("Field \"{0}\" value \"{1}\" was corrected to \"{2}\"."),
    ASCII_FILE_NAME_PROCESSOR_ERROR("Invalid {0} file name: \"{1}\". File name should conform following regular expression: {2}."),
    STUDY_PROCESSOR_LOOKUP_ERROR("Could not find study \"{0}\". The study must be owned by the submission account used for this submission or it must be private or temporarily suppressed and referenced by accession. Note that only a single study can be referenced. {1}"),
    SAMPLE_PROCESSOR_LOOKUP_ERROR("Could not find sample \"{0}\". The sample must be owned by the submission account used for this submission or it must be private or temporarily suppressed and referenced by accession. Note that only a single sample can be referenced. {1}"),
    RUN_PROCESSOR_LOOKUP_ERROR("Failed to lookup run \"{0}\". {1}"),
    ANALYSIS_PROCESSOR_LOOKUP_ERROR("Failed to lookup analysis \"{0}\". {1}"),
    READS_MANIFEST_READER_MISSING_PLATFORM_AND_INSTRUMENT_ERROR("Platform and/or instrument should be defined. Valid platforms: {0}. Valid instruments: {1}."),
    READS_MANIFEST_READER_INVALID_PLATFORM_FOR_INSTRUMENT_ERROR("Platform {0} for instrument {1}. Valid platforms are: {2}."),
    READS_MANIFEST_READER_MISSING_PLATFORM_FOR_INSTRUMENT_ERROR("Missing platform for instrument: {0}."),
    READS_MANIFEST_READER_INVALID_QUALITY_SCORE_ERROR("Invalid quality score: {0}"),
    CUSTOM_FIELD_PROCESSOR_INCORRECT_FIELD_VALUE("Field \"{0}\" value \"{1}\" format is incorrect \"{2}\". Expected format: \"CUSTOM_FIELD name:value\" ");

    private final String text;

    WebinCliMessage(String text) {
        this.text = text;
    }

    public String text() {
        return text;
    }
}
