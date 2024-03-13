package org.greenbeing.api;

import com.luckycatlabs.sunrisesunset.dto.Location;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Stores information relating to each participant, including onboarding data
public class Participant {
  private String telephoneNumber;
  private int gender;
  private Location workplaceLocation;
  private boolean isNewUser;
  private String participantNumber;
  private SurveyResponse currentSurveyResponse = new SurveyResponse(); // Store responses for the current survey

  public SurveyResponse getCurrentSurveyResponse() {
    return currentSurveyResponse;
  }

  public void clearCurrentSurveyResponse() {
    this.currentSurveyResponse = new SurveyResponse();
  }

  public List<SurveyResponse> getSurveyResponses() {
    return surveyResponses;
  }

  private List<SurveyResponse> surveyResponses = new ArrayList<>();

  public String getParticipantNumber() {
    return participantNumber;
  }

  public void setParticipantNumber(String participantNumber) {
    this.participantNumber = participantNumber;
  }

  public boolean isNewUser() {
    return isNewUser;
  }

  public void setNewUser(boolean isNewUser) {
    this.isNewUser = isNewUser;
  }

  public Participant(String telephoneNumber) {
    this.telephoneNumber = telephoneNumber;
  }

  public String getTelephoneNumber() {
    return telephoneNumber;
  }

  public String getFileTelephoneNumber() {
    return telephoneNumber.replace(":+", "_");
  }

  public int getGender() {
    return gender;
  }

  public Location getWorkplaceLocation() {
    return workplaceLocation;
  }

  public void setGender(int gender) {
    this.gender = gender;
  }

  public void setWorkplaceLocation(Location workplaceLocation) {
    this.workplaceLocation = workplaceLocation;
  }

  public void setTelephoneNumber(String telephoneNumber) {
    this.telephoneNumber = telephoneNumber;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Participant that = (Participant) o;
    return Objects.equals(telephoneNumber, that.telephoneNumber);
  }

  @Override
  public int hashCode() {
    return Objects.hash(telephoneNumber);
  }
}
