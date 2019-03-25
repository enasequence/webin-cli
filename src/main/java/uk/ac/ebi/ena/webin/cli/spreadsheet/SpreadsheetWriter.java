package uk.ac.ebi.ena.webin.cli.spreadsheet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
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

// https://poi.apache.org/components/spreadsheet/quick-guide.html

public class SpreadsheetWriter {

    public static void main(String[] args) {
        SpreadsheetWriter spreadsheetWriter = new SpreadsheetWriter();
        spreadsheetWriter.write();
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

        XSSFSheet sheet = workbook.createSheet(spreadsheetContext.getSheetName());
        Row headerRow = sheet.createRow(0);

        ArrayList<ManifestFieldDefinition> fields = new ArrayList<>();
        for (ManifestFieldDefinition field : manifest.getFields()) {
            if (ignoreFields == null || !ignoreFields.contains(field.getName())) {
                fields.add(field);
            }
        }

        addHeaderText(sheet, headerRow, fields);
        addHeaderComment(workbook, sheet, headerRow, fields);
        addColumnCV(sheet, fields);

        sheet.createFreezePane(0, 1);
    }

    private static void addHeaderText(Sheet sheet, Row row, ArrayList<ManifestFieldDefinition> fields) {
        int column = 0;
        int maxColumnWidth = 0;
        for (ManifestFieldDefinition field : fields) {
            Cell cell = row.createCell(column);
            cell.setCellValue(field.getName());
            sheet.autoSizeColumn(column);
            maxColumnWidth = Math.max(maxColumnWidth, sheet.getColumnWidth(column));

            column++;
        }

        for (int i = 0; i < column; ++i) {
            if (sheet.getColumnWidth(i) < maxColumnWidth * 7 / 10) {
                sheet.setColumnWidth(i, maxColumnWidth * 7 / 10);
            }
        }
    }


    private static void addHeaderComment(Workbook workbook, Sheet sheet, Row row, ArrayList<ManifestFieldDefinition> fields) {
        CreationHelper creationHelper = workbook.getCreationHelper();
        int column = 0;
        for (ManifestFieldDefinition field : fields) {
            Drawing drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(column);
            anchor.setCol2(column + 1);
            anchor.setRow1(0);
            anchor.setRow2(2);
            Comment comment = drawing.createCellComment(anchor);
            RichTextString str = creationHelper.createRichTextString(
                    field.getMinCount() > 0 ? "Mandatory field" : "Optional field");
            comment.setString(str);
            comment.setAuthor("Webin-CLI");
            Cell cell = row.getCell(column);
            cell.setCellComment(comment);

            column++;
        }
    }

    private static void addColumnCV(XSSFSheet sheet, ArrayList<ManifestFieldDefinition> fields) {
        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);

        int column = 0;
        for (ManifestFieldDefinition field : fields) {
            for (ManifestFieldProcessor processor : field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    CVFieldProcessor cvProcessor = (CVFieldProcessor) processor;
                    String[] values = new String[cvProcessor.getValues().size()];
                    values = cvProcessor.getValues().toArray(values);

                    XSSFDataValidationConstraint dvConstraint = (XSSFDataValidationConstraint)
                            dvHelper.createExplicitListConstraint(values);
                    CellRangeAddressList addressList = new CellRangeAddressList(-1, -1, column, column);
                    XSSFDataValidation validation = (XSSFDataValidation) dvHelper.createValidation(
                            dvConstraint, addressList);
                    validation.setShowErrorBox(true);
                    sheet.addValidationData(validation);

                }
            }

            column++;
        }
    }
}
