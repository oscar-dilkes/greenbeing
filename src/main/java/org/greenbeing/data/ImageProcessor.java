package org.greenbeing.data;

import org.jfree.chart.JFreeChart;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Authenticator;
import java.net.PasswordAuthentication;
import java.net.URL;
import java.net.URLConnection;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

// Processes images to send back to participant
public class ImageProcessor {

  // Retrieves path of combined images
  public static String getCombinedPath() {
    return combinedPath;
  }

  public static void setCombinedPath(String combinedPath) {
    ImageProcessor.combinedPath = combinedPath;
  }

  private static final String imagesPath = "images";
  private static String combinedPath;

  public static String processImage(List<String> imageList, String telephoneNumber, JFreeChart chart) throws IOException {
    String baseImagePath = (imageStitch(imageList, telephoneNumber));
    return overlayChartOnImage(baseImagePath, telephoneNumber, chart);
  }

  // Stitches provided images together
  private static String imageStitch(List<String> fileNames, String telephoneNumber) throws IOException {
    List<String> resizedPaths = new ArrayList<>();

    int totalWidth = 0;

    // Resize images to half of their original height and width to make graph more visible
    for (String fileName : fileNames) {
      BufferedImage originalImage = ImageIO.read(new File(fileName));
      int originalHeight = originalImage.getHeight();
      int originalWidth = originalImage.getWidth();

      double scaleFactor = 0.5;
      int newHeight = (int) (originalHeight * scaleFactor);
      int newWidth = (int) (originalWidth * scaleFactor);

      BufferedImage resizedImage = new BufferedImage(newWidth, newHeight, BufferedImage.TYPE_INT_ARGB);
      Graphics2D g2d = resizedImage.createGraphics();
      g2d.drawImage(originalImage.getScaledInstance(newWidth, newHeight, Image.SCALE_SMOOTH), 0, 0, null);
      g2d.dispose();

      ImageIO.write(resizedImage, "png", new File(fileName));
      resizedPaths.add(fileName);

      totalWidth += newWidth; // Update total width of image
    }

    // Get the minimum image height
    int minHeight = resizedPaths.stream().mapToInt(path -> {
      try {
        return ImageIO.read(new File(path)).getHeight();
      } catch (IOException e) {
        e.printStackTrace();
        return Integer.MAX_VALUE;
      }
    }).min().orElseThrow(IOException::new);

    // Create a blank image to stitch images into
    BufferedImage mergedImage = new BufferedImage(totalWidth, minHeight, BufferedImage.TYPE_INT_ARGB);
    Graphics g = mergedImage.getGraphics();

    // Stitch images together in order
    int x = 0;
    for (String resizedPath : resizedPaths) {
      BufferedImage image = ImageIO.read(new File(resizedPath));
      g.drawImage(image, x, 0, null);
      x += image.getWidth();
    }

    g.dispose();

    // Save the stitched image
    // Date and time used to avoid attempting to save image with same name twice, although this shouldn't happen
    LocalDateTime currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    String formattedDateTime = currentDateTime.format(formatter);
    String stitchedImagePath = imagesPath + "stitched_" + telephoneNumber + "_" + formattedDateTime + ".png";
    ImageIO.write(mergedImage, "png", new File(stitchedImagePath));
    return stitchedImagePath;
  }

  private static String overlayChartOnImage(String baseImagePath, String telephoneNumber, JFreeChart chart) throws IOException {
    // Read the base image
    BufferedImage baseImage = ImageIO.read(new File(baseImagePath));

    // Create a buffered image from the chart
    BufferedImage chartImage = new BufferedImage(baseImage.getWidth(), baseImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
    Graphics2D chartGraphics = chartImage.createGraphics();
    chart.draw(chartGraphics, new Rectangle(0, 0, baseImage.getWidth(), baseImage.getHeight()));
    chartGraphics.dispose();

    // Draw the chart image over the base image with transparency
    Graphics2D g2dBase = baseImage.createGraphics();

    // Apply transparency
    AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f); // 0.8f is the transparency level
    g2dBase.setComposite(alphaComposite);

    // Draw the chart onto the stitched image
    g2dBase.drawImage(chartImage, 0, 0, null);
    g2dBase.dispose();

    LocalDateTime currentDateTime = LocalDateTime.now();
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    String formattedDateTime = currentDateTime.format(formatter);

    // Save the combined image to a new file
    setCombinedPath("combined_" + telephoneNumber + "_" + formattedDateTime + ".png");

    String combinedImagePath = imagesPath + getCombinedPath();
    File outputFile = new File(combinedImagePath);
    ImageIO.write(baseImage, "png", outputFile);

    return outputFile.getAbsolutePath();
  }

  // Saves image from Twilio webhook to computer
  public static String saveImageFromWebhook(String mediaUrl, String participantNumber) {
    if (mediaUrl != null && !mediaUrl.isEmpty()) {
      try {
        // Set up Twilio authentication
        Authenticator.setDefault(new Authenticator() {
          protected PasswordAuthentication getPasswordAuthentication() {
            return new PasswordAuthentication("", "".toCharArray());
          }
        });

        // Open connection to webhook
        URL url = new URL(mediaUrl);
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();

        LocalDateTime currentDateTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
        String formattedDateTime = currentDateTime.format(formatter);

        String filePath = imagesPath + formattedDateTime + participantNumber + ".jpg";
        OutputStream outputStream = new FileOutputStream(filePath);

        // Read image data from input stream and save to file
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
          outputStream.write(buffer, 0, bytesRead);
        }

        // Close streams
        inputStream.close();
        outputStream.close();
        return filePath;
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
