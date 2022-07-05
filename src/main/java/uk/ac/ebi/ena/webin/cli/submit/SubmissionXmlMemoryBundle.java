package uk.ac.ebi.ena.webin.cli.submit;

import uk.ac.ebi.ena.webin.cli.validator.message.ValidationResult;

import java.io.File;
import java.util.List;

public class SubmissionXmlMemoryBundle extends SubmissionBundle {

    public SubmissionXmlMemoryBundle(File submitDir, String uploadDir, List<File> uploadFileList,
                                     List<SubmissionXMLFile> xmlFileList, String manifestMd5) {
        super(submitDir, uploadDir, uploadFileList, xmlFileList, manifestMd5);
    }

    @Override
    public void validate(ValidationResult result) { }
}
