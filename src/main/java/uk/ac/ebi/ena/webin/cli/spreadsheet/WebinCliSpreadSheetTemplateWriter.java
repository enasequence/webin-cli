package uk.ac.ebi.ena.webin.cli.spreadsheet;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.*;
import uk.ac.ebi.ena.webin.cli.assembly.GenomeAssemblyManifest;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldDefinition;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestFieldProcessor;
import uk.ac.ebi.ena.webin.cli.manifest.ManifestReader;
import uk.ac.ebi.ena.webin.cli.manifest.processor.CVFieldProcessor;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;

// https://poi.apache.org/components/spreadsheet/quick-guide.html

public class WebinCliSpreadSheetTemplateWriter {

    public static void main(String[] args) throws IOException  {


        XSSFWorkbook wb = new XSSFWorkbook();

        //Workbook wb = new HSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Webin-CLI");

        GenomeAssemblyManifest genomeManifest = new GenomeAssemblyManifest( null, null, null);
        Set<String> genomeManifestIgnore = new HashSet<>(Arrays.asList(GenomeAssemblyManifest.Fields.ASSEMBLYNAME));


        addHeader(wb, sheet, genomeManifest, genomeManifestIgnore);


        try (OutputStream fileOut = new FileOutputStream("workbook.xlsx")) {
            wb.write(fileOut);
        }
    }

    private static void addHeader(XSSFWorkbook wb, XSSFSheet sheet, ManifestReader manifest, Set<String> ignoreFields) {

        Row row = sheet.createRow(0);

        ArrayList<ManifestFieldDefinition> fields = new ArrayList<>();

        for (ManifestFieldDefinition field : manifest.getFields()) {
            if (ignoreFields == null || !ignoreFields.contains(field.getName())) {
                fields.add(field);
            }
        }



        addHeaderText(sheet, row, fields, ignoreFields);



        addHeaderComment(wb, sheet, row, fields, ignoreFields);


        // CVs

        XSSFDataValidationHelper dvHelper = new XSSFDataValidationHelper(sheet);

        int column = 0;
        for (ManifestFieldDefinition field : fields) {
            for(ManifestFieldProcessor processor :  field.getFieldProcessors()) {
                if (processor instanceof CVFieldProcessor) {
                    CVFieldProcessor cvProcessor = (CVFieldProcessor)processor;
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


        sheet.createFreezePane(0, 1);
    }

    private static void addHeaderText(Sheet sheet, Row row, ArrayList<ManifestFieldDefinition> fields, Set<String> ignoreFields) {
        int column = 0;
        int maxColumnWidth = 0;
        for (ManifestFieldDefinition field : fields) {
            String fieldName = field.getName();
            if (ignoreFields == null || !ignoreFields.contains(fieldName)) {
                Cell cell = row.createCell(column);
                cell.setCellValue(fieldName);
                sheet.autoSizeColumn(column);
                maxColumnWidth = Math.max(maxColumnWidth, sheet.getColumnWidth(column));

                column++;
            }
        }

        for (int i = 0 ; i < column ; ++i) {
            if (sheet.getColumnWidth(i) < maxColumnWidth * 7/10) {
                sheet.setColumnWidth(i, maxColumnWidth * 7/10);
            }
        }
    }


    private static void addHeaderComment(Workbook wb, Sheet sheet, Row row, ArrayList<ManifestFieldDefinition> fields, Set<String> ignoreFields) {
        int column;
        CreationHelper creationHelper = wb.getCreationHelper();

        column = 0;
        for (ManifestFieldDefinition field : fields) {
            String fieldName = field.getName();
            if (ignoreFields == null || !ignoreFields.contains(fieldName)) {

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
    }
}
