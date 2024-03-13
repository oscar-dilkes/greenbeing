package org.greenbeing.server;

import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.greenbeing.api.Participant;

import java.net.URI;
import java.util.List;

public class MessageSender {
  private final PhoneNumber sendPhoneNumber;
  private final Participant participant;

  public MessageSender(PhoneNumber sendPhoneNumber, Participant participant) {
    this.sendPhoneNumber = sendPhoneNumber;
    this.participant = participant;
  }

  // Implemented to handle RPS rate limit exceeded if multiple participants interact with system simultaneously
  public void exponentialBackoffRetry(int maxRetries, long initialDelay, Runnable task) throws InterruptedException {
    long delay = initialDelay;
    int retries = 0;
    while (retries < maxRetries) {
      try {
        task.run();
        // Exit retry loop upon success
        break;
      } catch (TwilioException e) {
        Thread.sleep(delay);
        // Exponential backoff doubles the delay after each attempt
        delay *= 2;
        retries++;
      }
    }
  }


  public void sendImage(String imageUrl) throws InterruptedException {
    PhoneNumber recipientPhoneNumber = new PhoneNumber(participant.getTelephoneNumber());

    exponentialBackoffRetry(3, 1000, () -> {
      Message message = Message.creator(
                      recipientPhoneNumber,
                      sendPhoneNumber,
                      "")
              .setMediaUrl(
                      List.of(URI.create(imageUrl)))
              .create();
      // Print record of message sent to terminal to enable easy checking if system is functioning
      System.out.println(participant.getTelephoneNumber() + " " + message.getSid());
      try {
        Thread.sleep(500);
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    });
  }

  public void sendMessages(List<String> messages) throws InterruptedException {
    PhoneNumber recipientPhoneNumber = new PhoneNumber(participant.getTelephoneNumber());

    exponentialBackoffRetry(3, 1000, () -> {
      for (String textBody : messages) {
        Message message = Message.creator(
                        recipientPhoneNumber,
                        sendPhoneNumber,
                        textBody)
                .create();
        System.out.println(participant.getTelephoneNumber() + " " + message.getSid());
        try {
          Thread.sleep(500);
        } catch (InterruptedException e) {
          throw new RuntimeException(e);
        }
      }
    });
  }
}
