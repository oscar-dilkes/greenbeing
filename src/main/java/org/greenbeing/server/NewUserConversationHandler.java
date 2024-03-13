package org.greenbeing.server;

import com.luckycatlabs.sunrisesunset.dto.Location;
import org.greenbeing.api.Participant;
import org.greenbeing.data.ParticipantDataHandler;

import java.util.List;

// Handle sending messages and storing data for new user
public class NewUserConversationHandler extends ConversationHandler {

  private final Participant participant;
  private final ParticipantDataHandler participantDataHandler;

  public NewUserConversationHandler(Participant participant, ParticipantDataHandler participantDataHandler) {
    super();
    this.participant = participant;
    this.participantDataHandler = participantDataHandler;
  }

  // Check if valid response to given question is received
  @Override
  protected void validateResponse(int position, Response response) throws ResponseInvalidException {
    switch (position) {
      case 0:
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
      case 1:
        // Check if a location is given
        if (response.getLongitude() == null || response.getLatitude() == null) {
          throw new ResponseInvalidException("Please send the location of your workplace.");
        }
        break;
    }
  }

  @Override
  public boolean handleResponse(MessageSender sender, Response response) throws ConversationHandlerException {
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

    // Set participant information based on position in onboarding process
    if (getConversation().getPosition() == 0) {
      participant.setGender(Integer.parseInt(response.getBody()));
    } else if (getConversation().getPosition() == 1) {
      Location workplaceLocation = new Location(response.getLatitude(), response.getLongitude());
      participant.setWorkplaceLocation(workplaceLocation);
      participantDataHandler.spreadsheetEditor(participant);
    }

    // Handle the next step of the survey based on the current state of the conversation
    return handleNextSurveyStep(sender);
  }

  // End of onboarding process
  @Override
  public boolean handleEndOfSurvey(MessageSender sender) throws ConversationHandlerException {
    participant.setNewUser(false);

    try {
      sender.sendMessages(List.of("Thank you! Send 'start walk' at any point to begin your first walk."));
    } catch (InterruptedException e) {
      throw new ConversationHandlerException(e);
    }

    return true;
  }

  @Override
  public List<String> getInitialMessages() {
    return List.of("Welcome! Since this is your first time participating, we need a couple of details from you.");
  }

  @Override
  public List<List<String>> getSurveyMessages() {
    return List.of(List.of("Which of the following genders best describes you?", """
            1 (Woman)
            2 (Man)
            3 (Transgender)
            4 (Non-binary/non-conforming)
            5 (Prefer not to respond)
            """), List.of("Please send the location of your workplace."));
  }
}
