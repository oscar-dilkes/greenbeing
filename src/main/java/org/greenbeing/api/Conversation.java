package org.greenbeing.api;

import java.util.List;

// Handles individual participant conversation information and handles minor conversation advancement logic
public class Conversation {
  private final List<List<String>> messages;
  private int position;

  private boolean waiting;

  public Conversation(List<List<String>> messages) {
    this.messages = messages;
    reset();
  }

  public boolean hasNextMessageList() {
    return position < messages.size();
  }

  public List<String> getCurrentMessageList() {
    return messages.get(position);
  }

  public void advancePosition() {
    position += 1;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public boolean isWaiting() {
    return waiting;
  }

  public void setWaiting(boolean waiting) {
    this.waiting = waiting;
  }

  public void reset() {
    this.position = -1;
    this.waiting = false;
  }
}
