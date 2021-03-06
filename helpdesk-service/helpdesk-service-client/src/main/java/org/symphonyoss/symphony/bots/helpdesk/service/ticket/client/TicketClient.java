package org.symphonyoss.symphony.bots.helpdesk.service.ticket.client;

import org.symphonyoss.symphony.bots.helpdesk.service.BaseClient;
import org.symphonyoss.symphony.bots.helpdesk.service.HelpDeskApiException;
import org.symphonyoss.symphony.bots.helpdesk.service.api.TicketApi;
import org.symphonyoss.symphony.bots.helpdesk.service.client.ApiClient;
import org.symphonyoss.symphony.bots.helpdesk.service.client.ApiException;
import org.symphonyoss.symphony.bots.helpdesk.service.client.Configuration;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Ticket;
import org.symphonyoss.symphony.bots.helpdesk.service.model.UserInfo;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Created by nick.tarsillo on 9/26/17.
 * The ticket service manages and creates help desk tickets.
 */
public class TicketClient extends BaseClient {

  public enum TicketStateType {
    UNSERVICED("UNSERVICED"),
    UNRESOLVED("UNRESOLVED"),
    RESOLVED("RESOLVED");

    private String state;

    TicketStateType(String state) {
      this.state = state;
    }

    public String getState() {
      return state;
    }
  }

  private TicketApi ticketApi;

  private String groupId;

  public TicketClient(String groupId, String ticketServiceUrl) {
    ApiClient apiClient = Configuration.getDefaultApiClient();
    apiClient.setBasePath(ticketServiceUrl);
    ticketApi = new TicketApi(apiClient);
    this.groupId = groupId;
  }

  /**
   * Gets a ticket by ID.
   *
   * @param jwt User JWT
   * @param ticketId the id of the ticket to get
   * @return the ticket
   */
  public Ticket getTicket(String jwt, String ticketId) {
    String authorization = getAuthorizationHeader(jwt);

    try {
      return ticketApi.getTicket(ticketId, authorization);
    } catch (ApiException e) {
      throw new HelpDeskApiException("Get ticket failed: " + ticketId, e);
    }
  }

  /**
   * Create a help desk ticket.
   *
   * @param jwt User JWT
   * @param ticketId the ticket Id to use to create the ticket
   * @param clientStreamId the stream id of the client room
   * @param serviceStreamId the stream id of the ticket room
   * @param timestamp Ticket timestamp
   * @param client Client user information
   * @param showHistory Flag to show history conversation
   * @param conversationId Conversation ID of the first message
   * @return the ticket object
   */
  public Ticket createTicket(String jwt, String ticketId, String clientStreamId, String serviceStreamId,
      Long timestamp, UserInfo client, Boolean showHistory, String conversationId) {
    String authorization = getAuthorizationHeader(jwt);

    Ticket ticket = new Ticket();
    ticket.setId(ticketId);
    ticket.setGroupId(groupId);
    ticket.setClientStreamId(clientStreamId);
    ticket.setServiceStreamId(serviceStreamId);
    ticket.setState(TicketStateType.UNSERVICED.getState());
    ticket.setClient(client);
    ticket.setQuestionTimestamp(timestamp);
    ticket.setShowHistory(showHistory);
    ticket.setConversationID(conversationId);

    try {
      return ticketApi.createTicket(ticket, authorization);
    } catch (ApiException e) {
      throw new HelpDeskApiException("Creating ticket failed: " + ticketId, e);
    }
  }

  /**
   * Gets a ticket by it's service stream Id
   *
   * @param jwt User JWT
   * @param serviceStreamId the stream of the client service room
   * @return the ticket
   */
  public Ticket getTicketByServiceStreamId(String jwt, String serviceStreamId) {
    String authorization = getAuthorizationHeader(jwt);

    try {
      List<Ticket> ticketList = ticketApi.searchTicket(authorization, groupId, serviceStreamId, null);

      if ((ticketList != null) && (!ticketList.isEmpty())) {
        return ticketList.get(0);
      }

      return null;
    } catch (ApiException e) {
      throw new HelpDeskApiException("Failed to search for room: " + serviceStreamId, e);
    }
  }

  /**
   * Gets a unresolved ticket by it's client stream Id
   *
   * @param jwt User JWT
   * @param clientStreamId the stream of the client stream id
   * @return the ticket
   */
  public Ticket getUnresolvedTicketByClientStreamId(String jwt, String clientStreamId) {
    String authorization = getAuthorizationHeader(jwt);

    try {
      List<Ticket> ticketList = ticketApi.searchTicket(authorization, groupId, null, clientStreamId);

      if ((ticketList != null) && (!ticketList.isEmpty())) {
        return ticketList.stream()
            .filter((ticket) -> !ticket.getState().equals(TicketStateType.RESOLVED.getState()))
            .findFirst()
            .orElse(null);
      }

      return null;
    } catch (ApiException e) {
      throw new HelpDeskApiException("Failed to search for room: " + clientStreamId, e);
    }
  }

  /**
   * Gets an unresolved ticket list
   *
   * @param jwt User JWT
   * @return ticket list or an empty list
   */
  public List<Ticket> getUnresolvedTickets(String jwt) {
    String authorization = getAuthorizationHeader(jwt);

    try {
      List<Ticket> ticketList = ticketApi.searchTicket(authorization, groupId, null, null);

      if ((ticketList != null) && (!ticketList.isEmpty())) {
        return ticketList.stream()
            .filter((ticket) -> !ticket.getState().equals(TicketStateType.RESOLVED.getState()))
            .collect(Collectors.toList());
      }
      return Collections.emptyList();
    } catch (ApiException e) {
      throw new HelpDeskApiException("Failed to search for tickets", e);
    }
  }

  /**
   * Updates a ticket.
   *
   * @param jwt User JWT
   * @param ticket the ticket to update.
   * @return the new ticket.
   */
  public Ticket updateTicket(String jwt, Ticket ticket) {
    String authorization = getAuthorizationHeader(jwt);

    try {
      return ticketApi.updateTicket(ticket.getId(), ticket, authorization);
    } catch (ApiException e) {
      throw new HelpDeskApiException("Updating ticket failed: " + ticket.getId(), e);
    }
  }

}
