package uk.ac.ebi.ena.webin.cli.spreadsheet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.IntStream;

// https://poi.apache.org/components/spreadsheet/quick-guide.html

public class SpreadsheetWriter {

    public static void main(String[] args) {
        SpreadsheetWriter spreadsheetWriter = new SpreadsheetWriter();
        spreadsheetWriter.write();
    }

    private static final String SHEET_NAME_CV = "cv";

    private static String getCvName(ManifestFieldDefinition field) {
        return "CV_" + field.getName();
    }

    public void write() {
        for (SpreadsheetContext spreadsheetContext : SpreadsheetContext.values()) {
            write(spreadsheetContext);
        }
    }

    private static void write(SpreadsheetContext spreadsheetContext) {

        XSSFWorkbook workbook = new XSSFWorkbook();

        try {
            addSheet(workbook, spreadsheetContext);
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, "Unable to create spreadsheet: " + spreadsheetContext.getFileName());
        }

        try (OutputStream fileOut = new FileOutputStream(spreadsheetContext.getFileName())) {
            workbook.write(fileOut);
        } catch (IOException ex) {
            throw WebinCliException.systemError(ex, "Unable to write spreadsheet: " + spreadsheetContext.getFileName());
        }
    }

    private static void addSheet(XSSFWorkbook workbook, SpreadsheetContext spreadsheetContext) {

        ManifestReader manifest = spreadsheetContext.getManifest();
        Set<String> ignoreFields = spreadsheetContext.getIgnoreFields();

        XSSFSheet dataSheet = workbook.createSheet(spreadsheetContext.getSheetName());
        XSSFSheet cvSheet = workbook.createSheet(SHEET_NAME_CV);

        Row dataSheetHeaderRow = dataSheet.createRow(0);
        Row cvSheetHeaderRow = cvSheet.createRow(0);

        ArrayList<ManifestFieldDefinition> fields = new ArrayList<>();
        for (ManifestFieldDefinition field : manifest.getFields()) {
            if (ignoreFields == null || !ignoreFields.contains(field.getName())) {
                fields.add(field);
            }
        }

        addHeaderText(dataSheet, dataSheetHeaderRow, fields);
        addHeaderText(cvSheet, cvSheetHeaderRow, fields);

        addHeaderComment(workbook, dataSheet, dataSheetHeaderRow, fields);
        addHeaderComment(workbook, cvSheet, cvSheetHeaderRow, fields);

        addCvValues(workbook, cvSheet, fields);

        addCvConstraints(dataSheet, fields);

        dataSheet.createFreezePane(0, 1);
    }

    private static void addHeaderText(Sheet dataSheet, Row dataSheetHeaderRow, ArrayList<ManifestFieldDefinition> fields) {
        int columnNumber = 0;
        int maxColumnWidth = 0;
        for (ManifestFieldDefinition field : fields) {
            Cell cell = dataSheetHeaderRow.createCell(columnNumber);
            cell.setCellValue(field.getName());
            dataSheet.autoSizeColumn(columnNumber);
            maxColumnWidth = Math.max(maxColumnWidth, dataSheet.getColumnWidth(columnNumber));

            columnNumber++;
        }

        for (int i = 0; i < columnNumber; ++i) {
            if (dataSheet.getColumnWidth(i) < maxColumnWidth * 7 / 10) {
                dataSheet.setColumnWidth(i, maxColumnWidth * 7 / 10);
            }
        }
    }


    private static void addHeaderComment(Workbook workbook, Sheet dataSheet, Row dataSheetHeaderRow, ArrayList<ManifestFieldDefinition> fields) {
        CreationHelper creationHelper = workbook.getCreationHelper();
        int columnNumber = 0;
        for (ManifestFieldDefinition field : fields) {
            Drawing drawing = dataSheet.createDrawingPatriarch();
            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(columnNumber);
            anchor.setCol2(columnNumber + 1);
            anchor.setRow1(0);
            anchor.setRow2(2);
            Comment comment = drawing.createCellComment(anchor);
            RichTextString str = creationHelper.createRichTextString(
                    field.getMinCount() > 0 ? "Mandatory field" : "Optional field");
            comment.setString(str);
            comment.setAuthor("Webin-CLI");
            Cell cell = dataSheetHeaderRow.getCell(columnNumber);
            cell.setCellComment(comment);

            columnNumber++;
        }
    }

    private static void addCvValues(Workbook workbook, XSSFSheet cvSheet, ArrayList<ManifestFieldDefinition> fields) {

        int maxValues = 0;
        for (ManifestFieldDefinition field : fields) {
            for (ManifestFieldProcessor processor : field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    maxValues = Math.max(maxValues, ((CVFieldProcessor) processor).getValues().size());
                }
            }
        }

        IntStream.range(1, maxValues + 1).forEach(cvSheet::createRow);

        for (int columnNumber = 0; columnNumber < fields.size(); ++columnNumber) {
            ManifestFieldDefinition field = fields.get(columnNumber);
            for (ManifestFieldProcessor processor : field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    List<String> values = ((CVFieldProcessor) processor).getValues();
                    for (int rowNumber = 1; rowNumber <= values.size(); ++rowNumber) {
                        XSSFRow row = cvSheet.getRow(rowNumber);
                        XSSFCell cell = row.createCell(columnNumber);
                        cell.setCellValue(values.get(rowNumber - 1));
                    }

                    Name name = workbook.createName();
                    name.setNameName(getCvName(field));
                    String columnLetter = CellReference.convertNumToColString(columnNumber);
                    name.setRefersToFormula(SHEET_NAME_CV
                            + "!"
                            + "$" + columnLetter + "$2"
                            + ":"
                            + "$" + columnLetter + "$" + (values.size()+1));
                }
            }
        }
    }

    private static void addCvConstraints(XSSFSheet dataSheet, ArrayList<ManifestFieldDefinition> fields) {
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(dataSheet);

        int columnNumber = 0;
        for (ManifestFieldDefinition field : fields) {
            for (ManifestFieldProcessor processor : field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    DataValidationConstraint constraint = helper.createFormulaListConstraint(getCvName(field));
                    CellRangeAddressList addressList = new CellRangeAddressList(-1, -1, columnNumber, columnNumber);
                    XSSFDataValidation validation =
                            (XSSFDataValidation) helper.createValidation(constraint, addressList);
                    validation.setShowErrorBox(true);
                    dataSheet.addValidationData(validation);

                }
            }
            columnNumber++;
        }
    }
}
