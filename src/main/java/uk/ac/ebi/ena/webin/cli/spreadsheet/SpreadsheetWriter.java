/*
 * Copyright 2018-2023 EMBL - European Bioinformatics Institute
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package uk.ac.ebi.ena.webin.cli.spreadsheet;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;

import uk.ac.ebi.ena.webin.cli.WebinCliException;
import uk.ac.ebi.ena.webin.cli.context.reads.ReadsManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldType;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;

public class SpreadsheetWriter {

    public static void main(String[] args) {
        SpreadsheetWriter.writeAll();
    }

    private final SpreadsheetContext spreadsheetContext;
    private final XSSFWorkbook workbook = new XSSFWorkbook();
    private final XSSFSheet dataSheet;
    private final XSSFSheet cvSheet;
    private final Row dataSheetHeaderRow;
    private final Row cvSheetHeaderRow;
    private final XSSFCellStyle requiredHeaderStyle;
    private final XSSFCellStyle optionalHeaderStyle;


    public SpreadsheetWriter(SpreadsheetContext spreadsheetContext) {
        this.spreadsheetContext = spreadsheetContext;
        this.dataSheet = workbook.createSheet(spreadsheetContext.getSheetName());
        this.cvSheet = workbook.createSheet(SHEET_NAME_CV);
        this.dataSheetHeaderRow = createHeaderRow(dataSheet);
        this.cvSheetHeaderRow = createHeaderRow(cvSheet);
        this.requiredHeaderStyle = getRequiredHeaderStyle();
        this.optionalHeaderStyle = getOptionalHeaderStyle();

        try {
            addSheets();
        } catch (Exception ex) {
            throw WebinCliException.systemError(ex, "Unable to create spreadsheet: " + spreadsheetContext.getFileName());
        }
    }

    private static final String SHEET_NAME_CV = "cv";

    private static String getCvName(ManifestFieldDefinition field) {
        return "CV_" + field.getName();
    }

    private void addSheets() {

        ArrayList<ManifestFieldDefinition> fields = getFields();

        addHeaderText(dataSheet, dataSheetHeaderRow, fields);
        addHeaderText(cvSheet, cvSheetHeaderRow, fields);

        addHeaderComment(dataSheet, dataSheetHeaderRow, fields);
        addHeaderComment(cvSheet, cvSheetHeaderRow, fields);

        addCvValues(fields);
        addCvConstraints(fields);

        dataSheet.createFreezePane(0, 1);
        cvSheet.createFreezePane(0, 1);
    }

    private ArrayList<ManifestFieldDefinition> getFields() {
        ManifestReader manifest = spreadsheetContext.getManifest();
        ArrayList<ManifestFieldDefinition> fields = new ArrayList<>();
        List<ManifestFieldDefinition> manifestFields = manifest.getFields();

        for (ManifestFieldDefinition field : manifestFields) {
            for (int i = 0 ; i < field.getRecommendedMaxCount() ; ++i) {
                fields.add(field);
            }
        }
        return fields;
    }

    private static XSSFRow createHeaderRow(XSSFSheet sheet) {
        return sheet.createRow(0);
    }

    private XSSFCellStyle getRequiredHeaderStyle() {
        XSSFCellStyle style = getCommonHeaderStyle();
        style.getFont().setBold(true);
        return style;
    }

    private XSSFCellStyle getOptionalHeaderStyle() {
        XSSFCellStyle style = getCommonHeaderStyle();
        style.getFont().setBold(true);
        style.getFont().setItalic(true);

        return style;
    }

    private XSSFCellStyle getCommonHeaderStyle() {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(new byte[]{(byte)29,(byte)128,(byte)134}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;

    }

    private void addHeaderText(XSSFSheet sheet, Row headerRow, ArrayList<ManifestFieldDefinition> fields) {
        int columnNumber = 0;
        int minColumnWidth = 256 * 20;

        for (ManifestFieldDefinition field : fields) {
            Cell cell = headerRow.createCell(columnNumber);
            cell.setCellValue(field.getName());
            cell.setCellStyle(field.getRecommendedMinCount() > 0 ? requiredHeaderStyle : optionalHeaderStyle);
            sheet.autoSizeColumn(columnNumber);
            columnNumber++;
        }

        int extraColumnToWidenForComments = 5;
        for (int i = 0; i < columnNumber + extraColumnToWidenForComments; ++i) {
            if (sheet.getColumnWidth(i) < minColumnWidth) {
                sheet.setColumnWidth(i, minColumnWidth);
            }
        }
    }

    private void addHeaderComment(XSSFSheet sheet, Row headerRow, ArrayList<ManifestFieldDefinition> fields) {
        CreationHelper creationHelper = workbook.getCreationHelper();
        int columnNumber = 0;
        for (ManifestFieldDefinition field : fields) {
            Drawing drawing = sheet.createDrawingPatriarch();
            ClientAnchor anchor = creationHelper.createClientAnchor();
            anchor.setCol1(columnNumber);
            anchor.setCol2(columnNumber + 3);
            anchor.setRow1(0);
            anchor.setRow2(7);
            Comment comment = drawing.createCellComment(anchor);

            RichTextString commentStr;
            if (field.getType() == ManifestFieldType.META) {
                    commentStr = creationHelper.createRichTextString(field.getDescription() +
                    (field.getRecommendedMinCount() > 0 ? " (mandatory field)" : "(optional field)"));
            }
            else {
                commentStr = creationHelper.createRichTextString(spreadsheetContext.getFileGroupText());
            }
            comment.setString(commentStr);
            comment.setAuthor("Webin-CLI");
            Cell cell = headerRow.getCell(columnNumber);
            cell.setCellComment(comment);

            columnNumber++;
        }
    }

    private void addCvValues(ArrayList<ManifestFieldDefinition> fields) {
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
                    List<String> values = getCvValues(field, (CVFieldProcessor) processor);
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

    private List<String> getCvValues(ManifestFieldDefinition field, CVFieldProcessor processor) {
        ArrayList<String> excludeValues = new ArrayList<>();
        if (spreadsheetContext == SpreadsheetContext.READ && field.getName().equals(ReadsManifestReader.Field.INSTRUMENT)) {
            excludeValues.add("unspecified");
        }
        return processor.getValues().stream().filter(value -> !excludeValues.contains(value)).sorted().collect(Collectors.toList());
    }

    private void addCvConstraints(ArrayList<ManifestFieldDefinition> fields) {
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

    public void write() {
        try (OutputStream fileOut = new FileOutputStream(spreadsheetContext.getFileName())) {
            workbook.write(fileOut);
        } catch (IOException ex) {
            throw WebinCliException.systemError(ex, "Unable to write spreadsheet: " + spreadsheetContext.getFileName());
        }
    }

    public static void writeAll() {
        new SpreadsheetWriter(SpreadsheetContext.GENOME).write();
        new SpreadsheetWriter(SpreadsheetContext.TRANSCRIPTOME).write();
        new SpreadsheetWriter(SpreadsheetContext.SEQUENCE).write();
        new SpreadsheetWriter(SpreadsheetContext.READ).write();
    }
}
