package org.symphonyoss.symphony.bots.ai.model;

import org.symphonyoss.symphony.clients.model.SymAttachmentInfo;
import org.symphonyoss.symphony.clients.model.SymMessage;
import org.symphonyoss.symphony.clients.model.SymStream;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * This class represents a message sent or received by the Ai.
 * <p>
 * Created by nick.tarsillo on 10/2/17.
 */
public class AiMessage {

  private String aiMessage;

  private String entityData;

  private String messageData;

  private Long fromUserId;

  private String messageId;

  private String streamId;

  private String timestamp;

  private File attachment;

  private List<SymAttachmentInfo> attachments;

  public AiMessage(String message) {
    this.aiMessage = message;
    this.entityData = "";
    this.attachments = new ArrayList<>();
  }

  public AiMessage(SymMessage symMessage) {
    this.aiMessage = symMessage.getMessageText();
    this.messageData = symMessage.getMessage();
    this.entityData = symMessage.getEntityData();
    this.attachments = symMessage.getAttachments();
    this.messageId = symMessage.getId();
    this.timestamp = symMessage.getTimestamp();
    this.streamId = symMessage.getStreamId();
    this.attachment = symMessage.getAttachment();
    this.fromUserId = symMessage.getFromUserId();
  }

  public String getAiMessage() {
    return aiMessage;
  }

  public void setAiMessage(String aiMessage) {
    this.aiMessage = aiMessage;
  }

  public File getAttachment() {
    return attachment;
  }

  public void setAttachment(File attachment) {
    this.attachment = attachment;
  }

  public String getEntityData() {
    return entityData;
  }

  public void setEntityData(String entityData) {
    this.entityData = entityData;
  }

  public List<SymAttachmentInfo> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<SymAttachmentInfo> attachments) {
    this.attachments = attachments;
  }

  public Long getFromUserId() {
    return fromUserId;
  }

  public void setFromUserId(Long fromUserId) {
    this.fromUserId = fromUserId;
  }

  public String getMessageId() {
    return messageId;
  }

  public void setMessageId(String messageId) {
    this.messageId = messageId;
  }

  public String getStreamId() {
    return streamId;
  }

  public void setStreamId(String streamId) {
    this.streamId = streamId;
  }

  public String getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(String timestamp) {
    this.timestamp = timestamp;
  }

  public String getMessageData() {
    return messageData;
  }


  public void setMessageData(String messageData) {
    this.messageData = messageData;
  }

  public SymMessage toSymMessage() {
    SymMessage symMessage = new SymMessage();
    symMessage.setEntityData(entityData);
    symMessage.setMessage(getAiMessage());
    symMessage.setMessage(messageData);

    SymStream stream = new SymStream();
    stream.setStreamId(streamId);
    symMessage.setStream(stream);
    symMessage.setStreamId(streamId);

    symMessage.setFromUserId(fromUserId);
    symMessage.setId(messageId);
    symMessage.setTimestamp(timestamp);
    symMessage.setAttachments(attachments);
    symMessage.setAttachment(attachment);

    return symMessage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) { return true; }
    if (o == null || getClass() != o.getClass()) { return false; }

    AiMessage that = (AiMessage) o;

    if (entityData != null ? !entityData.equals(that.entityData) : that.entityData != null) {
      return false;
    }
    if (messageData != null ? !messageData.equals(that.messageData) : that.messageData != null) {
      return false;
    }
    if (fromUserId != null ? !fromUserId.equals(that.fromUserId) : that.fromUserId != null) {
      return false;
    }
    if (messageId != null ? !messageId.equals(that.messageId) : that.messageId != null) {
      return false;
    }
    if (streamId != null ? !streamId.equals(that.streamId) : that.streamId != null) {
      return false;
    }
    if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) {
      return false;
    }
    if (attachment != null ? !attachment.equals(that.attachment) : that.attachment != null) {
      return false;
    }
    return attachments != null ? attachments.equals(that.attachments) : that.attachments == null;

  }

  @Override
  public int hashCode() {
    int result = entityData != null ? entityData.hashCode() : 0;
    result = 31 * result + (messageData != null ? messageData.hashCode() : 0);
    result = 31 * result + (fromUserId != null ? fromUserId.hashCode() : 0);
    result = 31 * result + (messageId != null ? messageId.hashCode() : 0);
    result = 31 * result + (streamId != null ? streamId.hashCode() : 0);
    result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
    result = 31 * result + (attachments != null ? attachments.hashCode() : 0);
    result = 31 * result + (attachment != null ? attachment.hashCode() : 0);

    return result;
  }
}
