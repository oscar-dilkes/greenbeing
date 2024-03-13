package org.greenbeing.data;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.greenbeing.api.SurveyResponse;

import java.io.*;
import java.text.ParseException;
import java.util.List;

// Write walk data to excel spreadsheet
public class WalkDataHandler {

  private static final String walkDataDirectory = "participantinformation" + File.separator + "walkdata" + File.separator;

  public void spreadsheetEditor(List<SurveyResponse> surveys, String userCode) throws ParseException, IOException {
    String fileName = walkDataDirectory + userCode + ".xlsx";
    File file = new File(fileName);
    Workbook workbook;
    Sheet sheet;

    int walkNumber;

    // Load spreadsheet if exists
    if (file.exists()) {
      try (InputStream is = new FileInputStream(file)) {
        workbook = WorkbookFactory.create(is);
        sheet = workbook.getSheetAt(0); // Assuming all data is in the first sheet
      } catch (IOException e) {
        throw new RuntimeException("Error reading existing Excel file", e);
      } catch (InvalidFormatException e) {
        throw new RuntimeException(e);
      }
      walkNumber = (int) ((sheet.getRow(sheet.getLastRowNum()).getCell(0).getNumericCellValue()) + 1);
    }
    // Create new spreadsheet if participants first walk
    else {
      workbook = new XSSFWorkbook();
      sheet = workbook.createSheet("Survey Data");
      createHeaderRow(sheet);
      walkNumber = 1;
    }

    // Append data to spreadsheet
    for (int i = 0; i < surveys.size(); i++) {
      appendSurveyData(sheet, surveys.get(i), walkNumber, i);
    }

    // Write to file
    try (OutputStream os = new FileOutputStream(fileName)) {
      workbook.write(os);
    } catch (IOException e) {
      throw new RuntimeException("Error writing to Excel file", e);
    }
  }

  private void createHeaderRow(Sheet sheet) {
    Row header = sheet.createRow(0);
    header.createCell(0).setCellValue("Walk_Number");
    header.createCell(1).setCellValue("Survey_Number");
    header.createCell(2).setCellValue("Date");
    header.createCell(3).setCellValue("Time");
    header.createCell(4).setCellValue("Content");
    header.createCell(5).setCellValue("Relaxed");
    header.createCell(6).setCellValue("Anxious");
    header.createCell(7).setCellValue("Irritated");
    header.createCell(8).setCellValue("Image_Greenness_Score");
    header.createCell(9).setCellValue("Latitude");
    header.createCell(10).setCellValue("Longitude");
    header.createCell(11).setCellValue("Day_Night");
  }

  private void appendSurveyData(Sheet sheet, SurveyResponse survey, int walkNumber, int surveyNumber) throws ParseException, IOException {
    int lastRowNum = sheet.getLastRowNum();
    Row row = sheet.createRow(lastRowNum + 1);

    row.createCell(0).setCellValue(walkNumber);
    row.createCell(1).setCellValue(surveyNumber + 1);
    row.createCell(2).setCellValue(survey.getDate());
    row.createCell(3).setCellValue(survey.getTime());

    List<String> responses = survey.getResponses();
    for (int i = 0; i < 4; i++) {
      row.createCell(4 + i).setCellValue(responses.size() > i ? responses.get(i) : "");
    }

    row.createCell(8).setCellValue(DataAnalysisUtils.analyseImage(survey.getImagePath()));
    row.createCell(9).setCellValue(String.valueOf(survey.getLocation().getLatitude()));
    row.createCell(10).setCellValue(String.valueOf(survey.getLocation().getLongitude()));

    if (DataAnalysisUtils.isDaytime(survey)) {
      row.createCell(11).setCellValue("Day");
    } else {
      row.createCell(11).setCellValue("Night");
    }
  }


}
