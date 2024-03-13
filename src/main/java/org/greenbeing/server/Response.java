package org.greenbeing.server;

// Response object contains response information per participant for each message
public class Response {

  private final String body;
  private final String mediaUrl;
  private final String latitude;
  private final String longitude;

  public Response(String body, String mediaUrl, String latitude, String longitude) {
    this.body = body;
    this.mediaUrl = mediaUrl;
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public String getBody() {
    return body;
  }

  public String getMediaUrl() {
    return mediaUrl;
  }

  public String getLatitude() {
    return latitude;
  }

  public String getLongitude() {
    return longitude;
  }
}
