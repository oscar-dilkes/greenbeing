package org.greenbeing.server;

import com.luckycatlabs.sunrisesunset.dto.Location;
import org.greenbeing.api.Participant;
import org.greenbeing.data.ImageProcessor;
import org.greenbeing.data.SurveyResponseHandler;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

// Handle sending messages and storing data for active walk
public class SurveyConversationHandler extends ConversationHandler {

  private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

  private final Participant participant;
  private final SurveyResponseHandler surveyResponseHandler;

  // Contains reference to 5 minute wait task so it can end when user ends walk
  private ScheduledFuture<?> submittedTask;

  public SurveyConversationHandler(Participant participant, SurveyResponseHandler surveyResponseHandler) {
    super();
    this.participant = participant;
    this.surveyResponseHandler = surveyResponseHandler;
  }

  // Check for valid responsse
  @Override
  protected void validateResponse(int position, Response response) throws ResponseInvalidException {
    switch (position) {
      case 0:
      case 1:
      case 2:
      case 3:
        // Check that an integer between 1 and 5 is given
        final String errorMessage = "Please send a response in the format '1', '2', '3', '4', or '5'.";

        try {
          int integerBody = Integer.parseInt(response.getBody());

          if (integerBody < 1 || integerBody > 5) {
            throw new ResponseInvalidException(errorMessage);
          }
        } catch (NumberFormatException e) {
          throw new ResponseInvalidException(errorMessage);
        }
        break;
      case 4:
        // Check if an image is given
        if (response.getMediaUrl() == null) {
          throw new ResponseInvalidException("Didn't receive an image.");
        }
        break;
      case 5:
        // Check if a location is given
        if (response.getLongitude() == null || response.getLatitude() == null) {
          throw new ResponseInvalidException("Didn't receive your location.");
        }
        break;
    }
  }

  @Override
  public boolean handleResponse(MessageSender sender, Response response) throws ConversationHandlerException {
    String bodyTrim = response.getBody().trim();

    // Ensures that the message that was sent prior to a user asking for help is sent again to improve user experience
    if (bodyTrim.equalsIgnoreCase("HELP")) {
      if (getConversation().getPosition() >= 0) {
        try {
          sender.sendMessages(getConversation().getCurrentMessageList());
        } catch (InterruptedException e) {
          throw new ConversationHandlerException(e);
        }
      }
      return true;
    }

    // Handle end of walk
    if (bodyTrim.equalsIgnoreCase("END WALK")) {
      handleEndWalk(sender);
      return true;
    }

    // Skip processing if the conversation is in 5 minute wait
    if (getConversation().isWaiting()) {
      return true;
    }

    try {
      validateResponse(getConversation().getPosition(), response);
    } catch (ResponseInvalidException e) {
      try {
        sender.sendMessages(List.of(e.getMessage()));
      } catch (InterruptedException ex) {
        throw new ConversationHandlerException(ex);
      }
      return false;
    }

    // Position 4 indicates waiting for image, 5 indicates waiting for location, any value between 0 and 3 is a normal PANAS score response
    if (getConversation().getPosition() == 4) {
      String imagePath = ImageProcessor.saveImageFromWebhook(response.getMediaUrl(), participant.getParticipantNumber());
      participant.getCurrentSurveyResponse().setImagePath(imagePath);
    } else if (getConversation().getPosition() == 5) {
      Location location = new Location(response.getLatitude(), response.getLongitude());
      participant.getCurrentSurveyResponse().setLocation(location);
    } else if (getConversation().getPosition() >= 0) {
      participant.getCurrentSurveyResponse().addResponse(response.getBody());
    }

    // Handle the next step of the survey based on the current state of the conversation
    return handleNextSurveyStep(sender);
  }

  // Handles 5 minute wait and retrieves date and time data
  @Override
  public boolean handleEndOfSurvey(MessageSender sender) throws ConversationHandlerException {
    try {
      sender.sendMessages(List.of("Thank you for answering these questions. You will receive your next survey in 5 minute's time.", "Type 'end walk' at any point during this wait to end your walk."));
    } catch (InterruptedException e) {
      throw new ConversationHandlerException(e);
    }

    LocalDateTime currentDateTime = LocalDateTime.now();

    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/dd/MM");
    participant.getCurrentSurveyResponse().setDate(currentDateTime.format(formatter));

    formatter = DateTimeFormatter.ofPattern("HH:mm");
    participant.getCurrentSurveyResponse().setTime(currentDateTime.format(formatter));

    participant.getSurveyResponses().add(participant.getCurrentSurveyResponse());
    participant.clearCurrentSurveyResponse();

    getConversation().setWaiting(true);

    // Implements 5 minute wait
    submittedTask = executorService.schedule(() -> {
      getConversation().setPosition(0);
      try {
        sender.sendMessages(getConversation().getCurrentMessageList());
        getConversation().setWaiting(false);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    }, 5, TimeUnit.MINUTES);

    return false;
  }

  private void handleEndWalk(MessageSender sender) throws ConversationHandlerException {
    // End 5 minute wait before ending conversation
    if (getConversation().isWaiting()) {
      if (submittedTask != null) {
        submittedTask.cancel(true);
      }

      executorService.shutdownNow();

      // Partiicpant ends walk while waiting for the next survey
      try {
        sender.sendMessages(List.of("Processing images..."));

        String processedImageURL = surveyResponseHandler.analyseSurveyResponse(participant, participant.getSurveyResponses());

        sender.sendImage(processedImageURL);
        sender.sendMessages(List.of("Thank you for participating. Type 'start walk' to begin your next walk."));
      } catch (InterruptedException | IOException e) {
        throw new ConversationHandlerException(e);
      }
    } else {
      // Participant ends walk before completing the current survey
      List<String> messages = new ArrayList<>();
      messages.add("Please answer the questions before ending the walk.");
      messages.addAll(getConversation().getCurrentMessageList());
      try {
        sender.sendMessages(messages);
      } catch (InterruptedException e) {
        throw new ConversationHandlerException(e);
      }
    }
  }

  @Override
  public List<String> getInitialMessages() {
    return List.of("Welcome back, we'll begin this walk shortly.", "All the questions in the following surveys require a response on a scale of 1 to 5, other than the photograph and location portions.", """
            1 (Not at all)
            2 (A little)
            3 (Moderately)s
            4 (Quite a bit)
            5 (Extremely)
            """, "Type 'help' at any point to receive a link to the help guide.");
  }

  @Override
  public List<List<String>> getSurveyMessages() {
    return List.of(List.of("How content do you feel at this moment?"), List.of("How relaxed do you feel at this moment?"), List.of("How anxious or worried do you feel at this moment?"), List.of("How irritated or frustrated do you feel at this moment?"), List.of("Please send a picture that best shows your surrounding environment."), List.of("Please send your location."));
  }
}