/*
GanttProject is an opensource project management tool.
Copyright (C) 2011 GanttProject team

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
package net.sourceforge.ganttproject.action.task;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.task.Task;
import net.sourceforge.ganttproject.task.TaskManager;



import net.sourceforge.ganttproject.gui.AbstractPagesDialog;
import net.sourceforge.ganttproject.gui.about.AboutDialog2;

import java.awt.event.ActionEvent;
import java.util.List;

public class TaskNewRecurringAction extends GPAction {
  private final IGanttProject myProject;
  private final UIFacade myUiFacade;


  public TaskNewRecurringAction(IGanttProject project, UIFacade uiFacade) {
    this(project, uiFacade, IconSize.MENU);
  }

  private TaskNewRecurringAction(IGanttProject project, UIFacade uiFacade, IconSize size) {
    super("task.newRecurring", size.asString());
    myProject = project;
    myUiFacade = uiFacade;
  }

  @Override
  public GPAction withIcon(IconSize size) {
    return new TaskNewRecurringAction(myProject, myUiFacade, size);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    if (calledFromAppleScreenMenu(e)) {
      return;
    }
    TaskRecurringDialog agp = new TaskRecurringDialog(myUiFacade);
    agp.show();
  }



  protected TaskManager getTaskManager() {
    return myProject.getTaskManager();
  }

  protected UIFacade getUIFacade() {
    return myUiFacade;
  }

  @Override
  public void updateAction() {
    super.updateAction();
  }

  @Override
  public TaskNewRecurringAction asToolbarAction() {
    TaskNewRecurringAction result = new TaskNewRecurringAction(myProject, myUiFacade);
    result.setFontAwesomeLabel(UIUtil.getFontawesomeLabel(result));
    return result;
  }
}