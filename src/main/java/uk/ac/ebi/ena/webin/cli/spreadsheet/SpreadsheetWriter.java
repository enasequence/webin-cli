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
import uk.ac.ebi.ena.webin.cli.rawreads.RawReadsManifest;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;
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
            addSheets(workbook, spreadsheetContext);
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, "Unable to create spreadsheet: " + spreadsheetContext.getFileName());
        }

        try (OutputStream fileOut = new FileOutputStream(spreadsheetContext.getFileName())) {
            workbook.write(fileOut);
        } catch (IOException ex) {
            throw WebinCliException.systemError(ex, "Unable to write spreadsheet: " + spreadsheetContext.getFileName());
        }
    }

    private static void addSheets(XSSFWorkbook workbook, SpreadsheetContext spreadsheetContext) {

        ArrayList<ManifestFieldDefinition> fields = getFields(spreadsheetContext);

        XSSFSheet dataSheet = workbook.createSheet(spreadsheetContext.getSheetName());
        XSSFSheet cvSheet = workbook.createSheet(SHEET_NAME_CV);

        Row dataSheetHeaderRow = createHeaderRow(dataSheet);
        Row cvSheetHeaderRow = createHeaderRow(cvSheet);

        XSSFCellStyle requiredHeaderStyle = getRequiredHeaderStyle(workbook);
        XSSFCellStyle optionalHeaderStyle = getOptionalHeaderStyle(workbook);

        addHeaderText(dataSheet, dataSheetHeaderRow, fields, requiredHeaderStyle, optionalHeaderStyle);
        addHeaderText(cvSheet, cvSheetHeaderRow, fields, requiredHeaderStyle, optionalHeaderStyle);

        addHeaderComment(workbook, dataSheet, dataSheetHeaderRow, fields);
        addHeaderComment(workbook, cvSheet, cvSheetHeaderRow, fields);

        addCvValues(spreadsheetContext, workbook, cvSheet, fields);
        addCvConstraints(workbook, dataSheet, fields);

        dataSheet.createFreezePane(0, 1);
        cvSheet.createFreezePane(0, 1);
    }

    private static ArrayList<ManifestFieldDefinition> getFields(SpreadsheetContext spreadsheetContext) {
        ManifestReader manifest = spreadsheetContext.getManifest();
        ArrayList<ManifestFieldDefinition> fields = new ArrayList<>();
        for (ManifestFieldDefinition field : manifest.getFields()) {
            for (int i = 0 ; i < field.getSpreadsheetMaxCount() ; ++i) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static XSSFRow createHeaderRow(XSSFSheet sheet) {
        return sheet.createRow(0);
    }

    private static XSSFCellStyle getRequiredHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = getCommonHeaderStyle(workbook);
        style.getFont().setBold(true);
        return style;
    }

    private static XSSFCellStyle getOptionalHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = getCommonHeaderStyle(workbook);
        style.getFont().setBold(true);
        style.getFont().setItalic(true);

        return style;
    }

    private static XSSFCellStyle getCommonHeaderStyle(XSSFWorkbook workbook) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)29,(byte)128,(byte)134}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;

    }

        private static void addHeaderText(Sheet dataSheet, Row dataSheetHeaderRow, ArrayList<ManifestFieldDefinition> fields,
                                          XSSFCellStyle requiredHeaderStyle, XSSFCellStyle optionalHeaderStyle) {
        int columnNumber = 0;
        int minColumnWidth = 256 * 20;

        for (ManifestFieldDefinition field : fields) {
            Cell cell = dataSheetHeaderRow.createCell(columnNumber);
            cell.setCellValue(field.getName());
            cell.setCellStyle(field.getSpreadsheetMinCount() > 0 ? requiredHeaderStyle : optionalHeaderStyle);
            dataSheet.autoSizeColumn(columnNumber);
            columnNumber++;
        }

        for (int i = 0; i < columnNumber; ++i) {
            if (dataSheet.getColumnWidth(i) < minColumnWidth) {
                dataSheet.setColumnWidth(i, minColumnWidth);
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
            anchor.setCol2(columnNumber + 2);
            anchor.setRow1(0);
            anchor.setRow2(2);
            Comment comment = drawing.createCellComment(anchor);
            RichTextString str = creationHelper.createRichTextString(field.getDescription() +
                    (field.getSpreadsheetMinCount() > 0 ? " (mandatory field)" : "")); // " (optional field)"));
            comment.setString(str);
            comment.setAuthor("Webin-CLI");
            Cell cell = dataSheetHeaderRow.getCell(columnNumber);
            cell.setCellComment(comment);

            columnNumber++;
        }
    }

    private static void addCvValues(SpreadsheetContext spreadsheetContext, Workbook workbook, XSSFSheet cvSheet, ArrayList<ManifestFieldDefinition> fields) {
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
                    List<String> values = getCvValues(spreadsheetContext, field, (CVFieldProcessor) processor);
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

    private static List<String> getCvValues(SpreadsheetContext spreadsheetContext, ManifestFieldDefinition field, CVFieldProcessor processor) {
        ArrayList<String> excludeValues = new ArrayList<>();
        if (spreadsheetContext == SpreadsheetContext.READ && field.getName().equals(RawReadsManifest.Field.INSTRUMENT)) {
            excludeValues.add("unspecified");
        }
        return processor.getValues().stream().filter(value -> !excludeValues.contains(value)).sorted().collect(Collectors.toList());
    }

    private static void addCvConstraints(XSSFWorkbook workbook, XSSFSheet dataSheet, ArrayList<ManifestFieldDefinition> fields) {
        XSSFDataValidationHelper helper = new XSSFDataValidationHelper(dataSheet);
        int lastRowNumber = workbook.getSpreadsheetVersion().getLastRowIndex();
        int columnNumber = 0;
        for (ManifestFieldDefinition field : fields) {
            for (ManifestFieldProcessor processor : field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    DataValidationConstraint constraint = helper.createFormulaListConstraint(getCvName(field));
                    CellRangeAddressList addressList = new CellRangeAddressList(1, lastRowNumber, columnNumber, columnNumber);
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
