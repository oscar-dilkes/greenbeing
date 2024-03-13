package org.greenbeing.data;

import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;
import com.luckycatlabs.sunrisesunset.SunriseSunsetCalculator;
import com.luckycatlabs.sunrisesunset.dto.Location;
import org.greenbeing.api.SurveyResponse;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class DataAnalysisUtils {

  // Defines greenspace labels
  private static final Set<String> greenSpaceLabels = Set.of(
          "dandelion", "garden", "grass", "grassland", "landscape", "lawn",
          "meadow", "park", "pasture", "tree", "woodland", "nature reserve",
          "vine", "flower", "bush", "forest", "plant", "greenhouse",
          "orchard", "flowerpot", "vegetation", "garden roses", "wildflower",
          "plantation", "shrub", "flowering plant", "agricultural land",
          "agricultural area", "rural area", "botanical garden");

  // Generates the image greenness score by comparing greenspace labels to total labels
  public static float analyseImage(String imagePath) throws IOException {
    float aggregateAnnotationScores = 0;
    int greenSpaceAnnotations = 0;
    int nonGreenSpaceAnnotations = 0;

    ByteString imgBytes = getImgBytes(imagePath);

    // Builds the image annotation request
    List<AnnotateImageRequest> requests = new ArrayList<>();
    Image img = Image.newBuilder().setContent(imgBytes).build();
    Feature feat = Feature.newBuilder().setType(Feature.Type.LABEL_DETECTION).build();
    AnnotateImageRequest request =
            AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
    requests.add(request);

    // Perform the request
    try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
      List<AnnotateImageResponse> responses = client.batchAnnotateImages(requests).getResponsesList();
      for (AnnotateImageResponse res : responses) {
        if (res.hasError()) {
          System.out.printf("Error: %s\n", res.getError().getMessage());
          return 0;
        }

        // Check for labels and classify them
        for (EntityAnnotation annotation : res.getLabelAnnotationsList()) {
          String label = annotation.getDescription().toLowerCase();
          if (greenSpaceLabels.contains(label)) {
            aggregateAnnotationScores += annotation.getScore();
            greenSpaceAnnotations++;
          } else {
            nonGreenSpaceAnnotations++;
          }
        }
      }
    }

    // Calculate final score considering both types of annotations
    if (greenSpaceAnnotations == 0) {
      return 0;
    } else {
      float greenSpaceScore = aggregateAnnotationScores / greenSpaceAnnotations;
      float deduction = (float) nonGreenSpaceAnnotations / (greenSpaceAnnotations + nonGreenSpaceAnnotations);
      return Math.max(greenSpaceScore - deduction, 0);
    }
  }

  // Reads the image file into memory
  private static ByteString getImgBytes(String imagePath) throws IOException {
    Path path = Paths.get(imagePath);
    byte[] data = Files.readAllBytes(path);
    return ByteString.copyFrom(data);
  }

  // Uses sunrise calendar API to assess whether it is day or night based on message information provided by Twilio API
  public static boolean isDaytime(SurveyResponse survey) throws ParseException {
    Location location = new Location(String.valueOf(survey.getLocation().getLatitude()), String.valueOf(survey.getLocation().getLongitude()));

    // Set your time zone
    String timeZoneId = "Europe/London";
    TimeZone timeZone = TimeZone.getTimeZone(timeZoneId);

    // Create a calculator instance
    SunriseSunsetCalculator calculator = new SunriseSunsetCalculator(location, timeZoneId);

    // Get today's date
    Calendar currentDate = Calendar.getInstance(timeZone);

    // Calculate sunrise and sunset times
    Calendar sunrise = calculator.getOfficialSunriseCalendarForDate(currentDate);
    Calendar sunset = calculator.getOfficialSunsetCalendarForDate(currentDate);

    String timeToCheck = survey.getTime();

    // Parse the time to check into a Calendar object
    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
    Date dateToCheck = sdf.parse(timeToCheck);
    Calendar checkTime = Calendar.getInstance();
    checkTime.setTime(dateToCheck);

    // Set the year, month, and day of  checkTime to match the sunrise and sunset dates
    checkTime.set(Calendar.YEAR, sunrise.get(Calendar.YEAR));
    checkTime.set(Calendar.MONTH, sunrise.get(Calendar.MONTH));
    checkTime.set(Calendar.DAY_OF_MONTH, sunrise.get(Calendar.DAY_OF_MONTH));

    // Return result
    return checkTime.after(sunrise) && checkTime.before(sunset);

  }


  public static JFreeChart analyseAndGraph(List<List<String>> walkResponses) {
    XYSeriesCollection dataset = new XYSeriesCollection();

    XYSeries lineSeries = new XYSeries("PANAS Score Line");
    XYSeries scatterSeries = new XYSeries("PANAS Score Points");

    for (int i = 0; i < walkResponses.size(); i++) {
      double score = analyseResponses(walkResponses.get(i));
      lineSeries.add(i + 1, score);
      scatterSeries.add(i + 1, score);
    }

    dataset.addSeries(lineSeries);
    dataset.addSeries(scatterSeries);

    // Create a chart with an XYPlot
    JFreeChart chart = ChartFactory.createXYLineChart(
            null, // Chart title
            "Survey number", // X-axis Label
            "Average PANAS Score", // Y-axis Label
            dataset, // Dataset
            PlotOrientation.VERTICAL,
            false, // Include legend
            true,
            false
    );

    XYPlot plot = chart.getXYPlot();

    // Customise x and y axes to show only integers
    NumberAxis xAxis = (NumberAxis) plot.getDomainAxis();
    xAxis.setStandardTickUnits(NumberAxis.createIntegerTickUnits());
    NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
    yAxis.setTickUnit(new NumberTickUnit(0.5));

    XYLineAndShapeRenderer lineRenderer = new XYLineAndShapeRenderer();
    lineRenderer.setSeriesShapesVisible(0, true);
    plot.setRenderer(0, lineRenderer);

    return chart;
  }

  // Calculate PANAS score by taking the mean of negative affect scores from mean of positive affect scores
  private static double analyseResponses(List<String> surveyResponses) {
    return (Double.parseDouble(surveyResponses.get(0)) + Double.parseDouble(surveyResponses.get(1))) / 2
            - (Double.parseDouble(surveyResponses.get(2)) + Double.parseDouble(surveyResponses.get(3))) / 2;
  }

}
