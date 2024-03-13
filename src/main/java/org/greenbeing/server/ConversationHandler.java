package org.greenbeing.server;

import org.greenbeing.api.Conversation;

import java.util.List;

// Main processor of conversation logic, inherited by SurveyConversationHandler and NewUserConversationHandler
public abstract class ConversationHandler {

    private final Conversation conversation;

    public ConversationHandler() {
        this.conversation = new Conversation(getSurveyMessages());
    }

    public Conversation getConversation() {
        return conversation;
    }

    protected abstract void validateResponse(int position, Response response) throws ResponseInvalidException;

    public abstract boolean handleResponse(MessageSender sender, Response response) throws ConversationHandlerException;

    // Start the next survey
    public void startSurvey(MessageSender sender) throws ConversationHandlerException {
        try {
            sender.sendMessages(getInitialMessages());
        } catch (InterruptedException e) {
            throw new ConversationHandlerException(e);
        }
    }

    // Determines whether to end a survey
    public boolean handleNextSurveyStep(MessageSender sender) throws ConversationHandlerException {
        conversation.advancePosition();

        // If there is another message left in the message list, send next messages
        if (conversation.hasNextMessageList()) {
            try {
                sender.sendMessages(conversation.getCurrentMessageList());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return false;
        } else {
            return handleEndOfSurvey(sender);
        }
    }

    public abstract boolean handleEndOfSurvey(MessageSender sender) throws ConversationHandlerException;
    public abstract List<String> getInitialMessages();
    public abstract List<List<String>> getSurveyMessages();
}
