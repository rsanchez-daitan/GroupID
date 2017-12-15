package org.symphonyoss.symphony.bots.helpdesk.bot.api;

import static org.symphonyoss.symphony.bots.helpdesk.service.membership.client.MembershipClient
    .MembershipType.AGENT;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.RestController;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.exceptions.SymException;
import org.symphonyoss.symphony.bots.ai.AiResponseIdentifier;
import org.symphonyoss.symphony.bots.ai.HelpDeskAi;
import org.symphonyoss.symphony.bots.ai.impl.AiResponseIdentifierImpl;
import org.symphonyoss.symphony.bots.ai.impl.SymphonyAiMessage;
import org.symphonyoss.symphony.bots.ai.model.AiSessionKey;
import org.symphonyoss.symphony.bots.helpdesk.bot.config.HelpDeskBotConfig;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.HealthcheckResponse;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.MakerCheckerMessageDetail;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.MakerCheckerResponse;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.TicketResponse;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.User;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.health.HealthCheckFailedException;
import org.symphonyoss.symphony.bots.helpdesk.bot.model.health.HealthcheckHelper;
import org.symphonyoss.symphony.bots.helpdesk.makerchecker.MakerCheckerService;
import org.symphonyoss.symphony.bots.helpdesk.makerchecker.model.AttachmentMakerCheckerMessage;
import org.symphonyoss.symphony.bots.helpdesk.service.makerchecker.client.MakercheckerClient;
import org.symphonyoss.symphony.bots.helpdesk.service.membership.client.MembershipClient;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Makerchecker;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Membership;
import org.symphonyoss.symphony.bots.helpdesk.service.model.Ticket;
import org.symphonyoss.symphony.bots.helpdesk.service.model.UserInfo;
import org.symphonyoss.symphony.bots.helpdesk.service.ticket.client.TicketClient;
import org.symphonyoss.symphony.bots.utility.validation.SymphonyValidationUtil;
import org.symphonyoss.symphony.clients.model.SymMessage;
import org.symphonyoss.symphony.clients.model.SymUser;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;

/**
 * Created by nick.tarsillo on 9/25/17.
 */
@RestController
public class V1HelpDeskController extends V1ApiController {
  private static final Logger LOG = LoggerFactory.getLogger(V1HelpDeskController.class);
  private static final String MAKER_CHECKER_SUCCESS_RESPONSE = "Maker checker message accepted.";
  private static final String MAKER_CHECKER_DENY_RESPONSE = "Maker checker message denied.";
  private static final String TICKET_SUCCESS_RESPONSE = "Ticket accepted.";
  private static final String TICKET_NOT_FOUND = "Ticket not found.";
  private static final String TICKET_WAS_CLAIMED = "Ticket was claimed.";
  private static final String HELPDESKBOT_NOT_FOUND = "Help desk bot not found.";
  private static final String MAKER_CHECKER_NOT_FOUND = "Makerchecker not found.";

  @Autowired
  private TicketClient ticketClient;

  @Autowired
  private SymphonyValidationUtil symphonyValidationUtil;

  @Autowired
  private MakercheckerClient makercheckerClient;

  @Autowired
  private MembershipClient membershipClient;

  @Autowired
  private HelpDeskBotConfig helpDeskBotConfig;

  @Qualifier("agentMakerCheckerService")
  @Autowired
  private MakerCheckerService agentMakerCheckerService;

  @Autowired
  private HelpDeskAi helpDeskAi;

  @Autowired
  private SymphonyClient symphonyClient;

  /**
   * Accepts a ticket.
   * Sends a message to the agent denoting that the ticket has successfully been accepted.
   * Sends a message to the client, notifying them that they are now being serviced by a agent,
   *    if the client was not being serviced prior.
   * Add agent to service room.
   * Change ticket state.
   *
   * @param ticketId the ticket id to accept
   * @param agentId the user id of the agent accepting the ticket
   * @return the ticket responses
   */
  @Override
  public TicketResponse acceptTicket(String ticketId, Long agentId) {
    Ticket ticket = ticketClient.getTicket(ticketId);

    if (ticket == null) {
      throw new BadRequestException(TICKET_NOT_FOUND);
    }

    if (TicketClient.TicketStateType.UNSERVICED.getState().equals(ticket.getState())) {
      symphonyValidationUtil.validateStream(ticket.getServiceStreamId());
      symphonyValidationUtil.validateStream(ticket.getClientStreamId());

      SymUser agentUser = symphonyValidationUtil.validateUserId(agentId);

      try {
        AiSessionKey sessionKey = helpDeskAi.getSessionKey(agentId, ticket.getServiceStreamId());

        symphonyClient.getRoomMembershipClient().addMemberToRoom(ticket.getServiceStreamId(), agentUser.getId());

        Membership membership = membershipClient.getMembership(agentId);

        if (membership == null) {
          membershipClient.newMembership(agentId, AGENT);
          LOG.info("Created new agent membership for userid: " + agentId);
        } else if (!AGENT.getType().equals(membership.getType())) {
          membership.setType(AGENT.getType());
          membershipClient.updateMembership(membership);
        }

        SymphonyAiMessage symphonyAiMessage =
            new SymphonyAiMessage(helpDeskBotConfig.getAcceptTicketClientSuccessResponse());

        Set<AiResponseIdentifier> responseIdentifierSet = new HashSet<>();
        responseIdentifierSet.add(new AiResponseIdentifierImpl(ticket.getClientStreamId()));
        if(ticket.getState().equals(TicketClient.TicketStateType.UNSERVICED.getState())) {
          helpDeskAi.sendMessage(symphonyAiMessage, responseIdentifierSet, sessionKey);
        }

        // Update ticket status and its agent
        UserInfo agent = new UserInfo();
        agent.setUserId(agentId);
        agent.setDisplayName(agentUser.getDisplayName());
        ticket.setAgent(agent);

        ticket.setState(TicketClient.TicketStateType.UNRESOLVED.getState());
        ticketClient.updateTicket(ticket);

        TicketResponse ticketResponse = new TicketResponse();
        ticketResponse.setMessage(TICKET_SUCCESS_RESPONSE);
        ticketResponse.setState(ticket.getState());
        ticketResponse.setTicketId(ticket.getId());

        User user = new User();
        user.setDisplayName(agentUser.getDisplayName());
        user.setUserId(agentId);

        ticketResponse.setUser(user);

        return ticketResponse;
      } catch (SymException e) {
        LOG.error("Could not accept ticket: ", e);
        throw new InternalServerErrorException();
      }
    } else {
      throw new BadRequestException(TICKET_WAS_CLAIMED);
    }
  }

  /**
   * Check pod connectivity.
   * Check agent connectivity.
   * @param groupId the group id of the bot to perform the health check on.
   * @return the health check response
   */
  @Override
  public HealthcheckResponse healthcheck(String groupId) {
    String agentUrl = helpDeskBotConfig.getAgentUrl();
    String podUrl = helpDeskBotConfig.getPodUrl();
    HealthcheckHelper healthcheckHelper = new HealthcheckHelper(podUrl, agentUrl);

    HealthcheckResponse response = new HealthcheckResponse();
    try {
      healthcheckHelper.checkPodConnectivity();
      response.setPodConnectivityCheck(true);
    } catch (HealthCheckFailedException e) {
      response.setPodConnectivityCheck(false);
      response.setPodConnectivityError(e.getMessage());
    }

    try {
      healthcheckHelper.checkAgentConnectivity();
      response.setAgentConnectivityCheck(true);
    } catch (HealthCheckFailedException e) {
      response.setAgentConnectivityCheck(false);
      response.setAgentConnectivityError(e.getMessage());
    }

    return response;
  }

  /**
   * Accept a maker checker message.
   * @param detail the maker checker message detail
   * @return a maker checker message response
   */
  @Override
  public MakerCheckerResponse acceptMakerCheckerMessage(MakerCheckerMessageDetail detail) {
    validateRequiredParameter("streamId", detail.getStreamId(), "body");
    validateRequiredParameter("groupId", detail.getGroupId(), "body");
    validateRequiredParameter("attachmentId", detail.getAttachmentId(), "body");
    validateRequiredParameter("timestamp", detail.getTimeStamp(), "body");
    validateRequiredParameter("messageId", detail.getMessageId(), "body");
    validateRequiredParameter("userId", detail.getUserId(), "body");

    Makerchecker makerchecker = makercheckerClient.getMakerchecker(detail.getAttachmentId());
    if (makerchecker == null) {
      throw new BadRequestException(MAKER_CHECKER_NOT_FOUND);
    }

    SymUser agentUser = symphonyValidationUtil.validateUserId(detail.getUserId());
    sendAcceptMarkerChekerMessages(detail);

    makerchecker.setCheckerId(detail.getUserId());
    makerchecker.setState(MakercheckerClient.AttachmentStateType.APPROVED.getState());
    makercheckerClient.updateMakerchecker(makerchecker);

    return buildMakerCheckerResponse(agentUser, detail);
  }

  private MakerCheckerResponse buildMakerCheckerResponse(SymUser agentUser,
      MakerCheckerMessageDetail detail) {
    MakerCheckerResponse makerCheckerResponse = new MakerCheckerResponse();
    makerCheckerResponse.setMessage(MAKER_CHECKER_SUCCESS_RESPONSE);
    makerCheckerResponse.setMakerCheckerMessageDetail(detail);

    User user = getUser(detail, agentUser);

    makerCheckerResponse.setUser(user);
    makerCheckerResponse.setState(MakercheckerClient.AttachmentStateType.APPROVED.getState());

    return makerCheckerResponse;
  }

  private void sendAcceptMarkerChekerMessages(MakerCheckerMessageDetail detail) {
    AttachmentMakerCheckerMessage checkerMessage = new AttachmentMakerCheckerMessage();
    checkerMessage.setAttachmentId(detail.getAttachmentId());
    checkerMessage.setGroupId(detail.getGroupId());
    checkerMessage.setMessageId(detail.getMessageId());
    checkerMessage.setStreamId(detail.getStreamId());
    checkerMessage.setProxyToStreamIds(detail.getProxyToStreamIds());
    checkerMessage.setTimeStamp(detail.getTimeStamp());
    checkerMessage.setType(detail.getType());

    Set<SymMessage> symMessages = agentMakerCheckerService.getAcceptMessages(checkerMessage);

    AiSessionKey aiSessionKey = helpDeskAi.getSessionKey(detail.getUserId(), detail.getStreamId());

    for (SymMessage symMessage : symMessages) {
      SymphonyAiMessage symphonyAiMessage = new SymphonyAiMessage(symMessage);
      Set<AiResponseIdentifier> identifiers = new HashSet<>();
      identifiers.add(new AiResponseIdentifierImpl(symMessage.getStreamId()));
      helpDeskAi.sendMessage(symphonyAiMessage, identifiers, aiSessionKey);
    }
  }

  /**
   * Deny a maker checker message.
   * @param detail the maker checker message detail
   * @return a maker checker message response
   */
  @Override
  public MakerCheckerResponse denyMakerCheckerMessage(MakerCheckerMessageDetail detail) {
    Makerchecker makerchecker = makercheckerClient.getMakerchecker(detail.getAttachmentId());
    if (makerchecker == null) {
      throw new BadRequestException(MAKER_CHECKER_NOT_FOUND);
    }

    SymUser agentUser = symphonyValidationUtil.validateUserId(detail.getUserId());

    makerchecker.setCheckerId(detail.getUserId());
    makerchecker.setState(MakercheckerClient.AttachmentStateType.DENIED.getState());
    makercheckerClient.updateMakerchecker(makerchecker);

    MakerCheckerResponse makerCheckerResponse = new MakerCheckerResponse();
    makerCheckerResponse.setMessage(MAKER_CHECKER_DENY_RESPONSE);
    makerCheckerResponse.setMakerCheckerMessageDetail(detail);

    User user = getUser(detail, agentUser);

    makerCheckerResponse.setUser(user);
    makerCheckerResponse.setState(MakercheckerClient.AttachmentStateType.DENIED.getState());

    return makerCheckerResponse;
  }

  private User getUser(MakerCheckerMessageDetail detail, SymUser agentUser) {
    User user = new User();
    user.setDisplayName(agentUser.getDisplayName());
    user.setUserId(detail.getUserId());
    return user;
  }

}
