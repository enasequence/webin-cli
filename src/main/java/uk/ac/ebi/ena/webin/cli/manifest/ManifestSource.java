package uk.ac.ebi.ena.webin.cli.manifest;

import org.apache.poi.ss.usermodel.Sheet;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.utils.FileUtils;

import java.io.File;

public class ManifestSource {
    private final File manifestFile;
    private final boolean spreadsheet;
    private final Sheet dataSheet;
    private final Integer dataSheetRowNumber;

    public ManifestSource(File manifestFile) {
        this.manifestFile = manifestFile;
        this.spreadsheet = false;
        this.dataSheet = null;
        this.dataSheetRowNumber = null;
    }

    public ManifestSource(File manifestFile, Sheet dataSheet, Integer dataSheetRowNumber) {
        this.manifestFile = manifestFile;
        this.spreadsheet = true;
        this.dataSheet = dataSheet;
        this.dataSheetRowNumber = dataSheetRowNumber;
    }

    public File getManifestFile() {
        return manifestFile;
    }

    public boolean isSpreadsheet() {
        return spreadsheet;
    }

    public Sheet getDataSheet() {
        return dataSheet;
    }

    public Integer getDataSheetRowNumber() {
        return dataSheetRowNumber;
    }

    public String getManifestMd5() {
        try {
            if (isSpreadsheet()) {
                // TODO: think about better ways to calculate checksum for spreadsheet manifest
                return FileUtils.calculateDigest("MD5", manifestFile);
            } else {
                return FileUtils.calculateDigest("MD5", manifestFile);
            }
        }
        catch (Exception ex) {
            throw WebinCliException.systemError("Unable to calculate manifest checksum");
        }
    }
}
