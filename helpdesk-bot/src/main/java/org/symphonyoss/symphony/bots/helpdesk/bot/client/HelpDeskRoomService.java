package org.symphonyoss.symphony.bots.helpdesk.bot.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.symphonyoss.client.SymphonyClient;
import org.symphonyoss.client.SymphonyClientConfig;
import org.symphonyoss.client.exceptions.RoomException;
import org.symphonyoss.client.exceptions.StreamsException;
import org.symphonyoss.client.exceptions.SymException;
import org.symphonyoss.client.model.Room;
import org.symphonyoss.client.model.SymAuth;
import org.symphonyoss.client.services.RoomService;
import org.symphonyoss.symphony.clients.model.SymRoomAttributes;
import org.symphonyoss.symphony.clients.model.SymRoomDetail;

/**
 * Created by rsanchez on 05/12/17.
 */
public class HelpDeskRoomService extends RoomService {

  private static final Logger LOGGER = LoggerFactory.getLogger(HelpDeskRoomService.class);

  private final SymphonyClient symClient;

  private final HelpDeskStreamsClient streamsClient;

  public HelpDeskRoomService(SymphonyClient symClient, SymAuth symAuth, SymphonyClientConfig config) {
    super(symClient);
    this.symClient = symClient;
    this.streamsClient = new HelpDeskStreamsClient(symAuth, config);
  }

  public Room createRoom(SymRoomAttributes symRoomAttributes) throws RoomException {
    if(symRoomAttributes == null) {
      throw new NullPointerException("Room attributes were not provided..");
    } else {
      try {
        SymRoomDetail chatRoom = this.streamsClient.createChatRoom(symRoomAttributes, true);
        Room room = new Room();
        room.setId(chatRoom.getRoomSystemInfo().getId());
        room.setRoomDetail(chatRoom);
        room.setStreamId(room.getId());
        room.setMembershipList(this.symClient.getRoomMembershipClient().getRoomMembership(room.getId()));
        return room;
      } catch (StreamsException e) {
        LOGGER.error("Failed to obtain stream for room...", e);
        throw new RoomException("Could not create/join chat room: " + symRoomAttributes.getName(), e);
      } catch (SymException e) {
        LOGGER.error("Failed to retrieve room membership...", e);
        throw new RoomException("Could not retrieve room membership for room: " + symRoomAttributes.getName());
      }
    }
  }

}