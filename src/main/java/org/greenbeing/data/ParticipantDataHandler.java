package org.greenbeing.data;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.greenbeing.api.Participant;

import java.io.*;

// Appends onboarding data to excel spreadsheet
public class ParticipantDataHandler {

  private static final String fileName = "participantinformation.xlsx";

  public void spreadsheetEditor(Participant participant) {
    File file = new File(fileName);
    Workbook workbook;
    Sheet sheet;

    try (InputStream is = new FileInputStream(file)) {
      workbook = WorkbookFactory.create(is);
      // All data in first sheet
      sheet = workbook.getSheetAt(0);
    } catch (IOException e) {
      throw new RuntimeException("Error reading Excel file", e);
    } catch (InvalidFormatException e) {
      throw new RuntimeException(e);
    }

    appendParticipantData(sheet, participant);

    // Write to file
    try (OutputStream os = new FileOutputStream(fileName)) {
      workbook.write(os);
    } catch (IOException e) {
      throw new RuntimeException("Error writing Excel file", e);
    }
  }

  // Retrieves the participant number corresponding to their telephone number
  // Participant number acts as a key
  public String retrieveParticipantNumber(String telephoneNumber) {
    File file = new File(fileName);
    Workbook workbook;
    Sheet sheet;
    try (InputStream is = new FileInputStream(file)) {
      workbook = WorkbookFactory.create(is);
      sheet = workbook.getSheetAt(0);

      // Iterate through each row in the sheet
      for (Row row : sheet) {
        if (row.getCell(0) != null) {
          String cellValue = row.getCell(0).getStringCellValue();
          // If telephone number matches, return participant number
          if (telephoneNumber.equals(cellValue)) {
            return row.getCell(1) != null ? row.getCell(1).getStringCellValue() : null;
          }
        }
      }

    } catch (IOException | InvalidFormatException e) {
      throw new RuntimeException("Error reading Excel file", e);
    }
    // Return null if no match found
    return null;
  }

  // Checks if participant has completed onboarding process previously
  public boolean doesPhoneNumberExist(String telephoneNumber) {
    File file = new File(fileName);
    System.out.println(fileName);
    Workbook workbook;
    Sheet sheet;
    try (FileInputStream fis = new FileInputStream(file);
         BufferedInputStream is = new BufferedInputStream(fis)) {
      workbook = WorkbookFactory.create(is);
      sheet = workbook.getSheetAt(0);

      for (Row row : sheet) {
        if (row.getCell(0) != null) {
          String cellValue = row.getCell(0).getStringCellValue();
          if (telephoneNumber.equals(cellValue)) {
            return true;
          }
        }
      }
    } catch (IOException | InvalidFormatException e) {
      throw new RuntimeException("Error reading Excel file", e);
    }
    return false;
  }

  // Appends new participant data following onboarding process
  private void appendParticipantData(Sheet sheet, Participant participant) {
    int lastRowNum = sheet.getLastRowNum();
    Row row = sheet.createRow(lastRowNum + 1);

    int participantNumber = Integer.parseInt((sheet.getRow(lastRowNum).getCell(1).getStringCellValue().replaceAll("[^0-9]", ""))) + 1;

    row.createCell(0).setCellValue(participant.getTelephoneNumber());
    row.createCell(1).setCellValue("participant" + participantNumber);
    row.createCell(2).setCellValue(participant.getGender());
    row.createCell(3).setCellValue(String.valueOf(participant.getWorkplaceLocation().getLatitude()));
    row.createCell(4).setCellValue(String.valueOf(participant.getWorkplaceLocation().getLongitude()));
  }
}
