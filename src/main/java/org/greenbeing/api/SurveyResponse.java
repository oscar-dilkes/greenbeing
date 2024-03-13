package org.greenbeing.api;

import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.ArrayList;
import java.util.List;

// Stores data relevant to survey responses, that are later placed into excel spreadsheets
public class SurveyResponse {

  private final List<String> responses = new ArrayList<>();
  private Location location;
  private String date;
  private String time;
  private String imagePath;

  public List<String> getResponses() {
    return responses;
  }

  public Location getLocation() {
    return location;
  }

  public String getDate() {
    return date;
  }

  public String getTime() {
    return time;
  }

  public void addResponse(String response) {
    responses.add(response);
  }

  public void setLocation(Location location) {
    this.location = location;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public void setTime(String time) {
    this.time = time;
  }

  public String getImagePath() {
    return imagePath;
  }

  public void setImagePath(String imagePath) {
    this.imagePath = imagePath;
  }

}
