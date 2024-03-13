package org.greenbeing.data;

import org.greenbeing.api.Participant;
import org.greenbeing.api.SurveyResponse;
import org.jfree.chart.JFreeChart;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

// Handles survey responses at the end of a walk
public class SurveyResponseHandler {

  private static final String imagesPath = "images" + File.separator;

  private WalkDataHandler walkDataHandler;

  public SurveyResponseHandler(WalkDataHandler walkDataHandler) {
    this.walkDataHandler = walkDataHandler;
  }

  // Handle survey responses and manage incentive image creation
  public String analyseSurveyResponse(Participant participant, List<SurveyResponse> participantSurveyResponses) throws IOException {
    List<List<String>> surveyResponses = new ArrayList<>();
    List<String> imageResponses = new ArrayList<>();

    for (SurveyResponse response : participantSurveyResponses) {
      surveyResponses.add(response.getResponses());
      imageResponses.add(response.getImagePath());
    }

    // Retrieve graph
    JFreeChart chart = DataAnalysisUtils.analyseAndGraph(surveyResponses);

    String filePath = ImageProcessor.processImage(imageResponses, participant.getFileTelephoneNumber(), chart);
    String processedImageURL = S3ImageUploader.imageUploader(ImageProcessor.getCombinedPath(), filePath);

    File combinedImage = new File(imagesPath + ImageProcessor.getCombinedPath());

    // Delete images after use to save storage
    if (combinedImage.exists()) {
      combinedImage.delete();
    }

    try {
      walkDataHandler.spreadsheetEditor(participant.getSurveyResponses(), participant.getParticipantNumber());
    } catch (ParseException e) {
      throw new IOException(e);
    }

    // Delete images after use to save storage
    for (String imagePath : imageResponses) {
      File baseImageToDelete = new File(imagePath);
      if (baseImageToDelete.exists()) {
        baseImageToDelete.delete();
      }
    }

    return processedImageURL;
  }
}
