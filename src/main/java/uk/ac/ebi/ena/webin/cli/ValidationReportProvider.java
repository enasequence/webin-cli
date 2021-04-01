package uk.ac.ebi.ena.webin.cli;

import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldValue;
import uk.ac.ebi.ena.webin.cli.validator.message.ValidationReport;

import java.io.File;

public interface ValidationReportProvider {

  ValidationReport getSubmissionReport();

  ValidationReport getSubmissionReportOfBundleOrigin();

  ValidationReport getSubmissionReportOfManifestOrigin();

  ValidationReport getSubmissionFileReport(File file);
}
