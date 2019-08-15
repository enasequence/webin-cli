package uk.ac.ebi.ena.webin.cli.manifest;

import uk.ac.ebi.ena.webin.cli.ManifestBuilder;
import uk.ac.ebi.ena.webin.cli.WebinCliMessage;

public class ManifestReaderFileSuffixTester {
    public static <FileType extends Enum<FileType>, T extends ManifestReader>
    void invalid(Class<T> manifestReaderClass, FileType fileType, String fileName) {
        ManifestReaderTester tester =
                new ManifestReaderTester(manifestReaderClass)
                        .manifestValidateMandatory(false)
                        .manifestValidateFileExist(false)
                        .manifestValidateFileCount(false);
        ManifestBuilder manifestBuilder = new ManifestBuilder().file(fileType, fileName);
        tester.testError(manifestBuilder, WebinCliMessage.Manifest.INVALID_FILE_SUFFIX_ERROR);
    }

    public static <FileType extends Enum<FileType>, T extends ManifestReader>
    void valid(Class<T> manifestReaderClass, FileType fileType, String fileName) {
        ManifestReaderTester tester =
                new ManifestReaderTester(manifestReaderClass)
                        .manifestValidateMandatory(false)
                        .manifestValidateFileExist(false)
                        .manifestValidateFileCount(false);
        ManifestBuilder manifestBuilder = new ManifestBuilder().file(fileType, fileName);
        tester.test(manifestBuilder);
    }
}
