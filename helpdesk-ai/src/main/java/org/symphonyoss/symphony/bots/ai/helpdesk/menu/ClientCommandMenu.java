package org.symphonyoss.symphony.bots.ai.helpdesk.menu;

import org.apache.commons.lang3.StringUtils;
import org.symphonyoss.symphony.bots.ai.model.AiCommandMenu;

/**
 * Created by nick.tarsillo on 9/28/17.
 * An AI command line menu that will contain client commands. (If any.)
 */
public class ClientCommandMenu extends AiCommandMenu {

  public ClientCommandMenu() {
    super(StringUtils.EMPTY);
  }

}
