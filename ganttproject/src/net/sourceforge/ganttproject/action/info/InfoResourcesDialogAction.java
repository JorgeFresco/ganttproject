/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev

This program is free software; you can redistribute it and/or
modify it under the terms of the GNU General Public License
as published by the Free Software Foundation; either version 3
of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package net.sourceforge.ganttproject.action.info;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.InfoResourcesDialog2;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.HumanResource;

import java.awt.event.ActionEvent;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Action to show the options dialog for the application. It will seach and show
 * all available OptionPageProvider classes
 */
public class InfoResourcesDialogAction extends GPAction {
  private final UIFacade myUiFacade;
  private final IGanttProject myProject;

  private HumanResourceManager myRm;

  public InfoResourcesDialogAction(IGanttProject project, UIFacade uiFacade, HumanResourceManager rm) {
    super("resourcesInfo.app");
    myUiFacade = uiFacade;
    myProject = project;
    myRm = rm;
  }

  public String getResourcesIDs() {
    String IDs = "";
    int i = 0;
    List<HumanResource> resources = myRm.getResources();
    Iterator<HumanResource> it = resources.iterator();
    HumanResource current;

    while(it.hasNext()) {
      current = it.next();
      if(i == 0){
        IDs += Integer.toString(current.getId());
      }
      else {
        IDs += "," + Integer.toString(current.getId());
      }
      i++;
    }

    return IDs;
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    InfoResourcesDialog2 dialog = new InfoResourcesDialog2(myProject, myUiFacade, "settings.app.pageOrder");
    dialog.show();
  }
}
