package org.greenbeing.server;

import com.twilio.Twilio;
import com.twilio.type.PhoneNumber;
import org.greenbeing.api.Participant;
import org.greenbeing.data.ParticipantDataHandler;
import org.greenbeing.data.SurveyResponseHandler;
import org.greenbeing.data.WalkDataHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.port;
import static spark.Spark.post;

// Main class that receives participant messages from Twilio
public class Main {
  private static final String ACCOUNT_SID = "";
  private static final String AUTH_TOKEN = "";
  private static final String TWILIO_PHONE_NUMBER = "";

  private static final Map<String, Participant> participantInformationMap = new HashMap<>();
  private static final Map<Participant, ConversationHandler> conversationMap = new HashMap<>();

  private static final WalkDataHandler walkDataHandler = new WalkDataHandler();
  private static final ParticipantDataHandler participantDataHandler = new ParticipantDataHandler();

  private static final SurveyResponseHandler responseHandler = new SurveyResponseHandler(walkDataHandler);

  public static void main(String[] args) {
    // Connect to Twilio
    Twilio.init(ACCOUNT_SID, AUTH_TOKEN);
    // Initialise number used by Twilio to send messages
    new PhoneNumber("whatsapp:" + TWILIO_PHONE_NUMBER);

    receiveAndSend();
  }

  // Receives messages, and directs to survey sending logic
  // Initial point where new users are identified
  public static void receiveAndSend() {

    port(80); // Set HTTP port

    // Set up a custom handler for incoming WhatsApp messages
    post("/whatsapp", (request, response) -> {
      String from = request.queryParams("From");
      String body = request.queryParams("Body");
      // Image webhook URL
      String mediaUrl = request.queryParams("MediaUrl0");
      String latitude = request.queryParams("Latitude");
      String longitude = request.queryParams("Longitude");

      if (from == null || body == null) {
        return false;
      }

      Participant participant;

      // Acknowledge valid message received by setting response code
      response.status(200);
      // Faster way of identifying if participant has signed up if they interact before system restarts
      if (!participantInformationMap.containsKey(from)) {
        participant = new Participant(from);

        // Backstop
        if (!participantDataHandler.doesPhoneNumberExist(from)) {
          participant.setNewUser(true);
        }

        participant.setTelephoneNumber(from);
        participant.setParticipantNumber(participantDataHandler.retrieveParticipantNumber(participant.getFileTelephoneNumber()));

        // Place empty participant data in map for handling later
        participantInformationMap.put(from, participant);
      } else {
        participant = participantInformationMap.get(from);
      }

      MessageSender sender = new MessageSender(new PhoneNumber("whatsapp:" + TWILIO_PHONE_NUMBER), participant);

      // Send URL of help sheet hosted on S3 bucket
      if (body.trim().equalsIgnoreCase("HELP")) {
        sender.sendMessages(List.of("s3bucketlink"));
      }

      // Start conversation if necessary
      if (!conversationMap.containsKey(participant)) {
        ConversationHandler conversationHandler;

        if (participant.isNewUser()) {
          conversationHandler = new NewUserConversationHandler(participant, participantDataHandler);
          conversationHandler.startSurvey(sender);
        } else if (body.trim().equalsIgnoreCase("START WALK")) {
          conversationHandler = new SurveyConversationHandler(participant, responseHandler);
          conversationHandler.startSurvey(sender);
        } else {
          return true;
        }

        // Acknowledge active conversation by placing in conversation map
        conversationMap.put(participant, conversationHandler);
      }

      // Get conversation
      ConversationHandler conversationHandler = conversationMap.get(participant);
      try {
        if (conversationHandler.handleResponse(sender, new Response(body, mediaUrl, latitude, longitude))) {
          // End conversation
          conversationMap.remove(participant);
        }
      } catch (ConversationHandlerException e) {
        e.printStackTrace();
        return false;
      }

      return true;
    });
  }
}
