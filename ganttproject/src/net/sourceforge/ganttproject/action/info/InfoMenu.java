/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2011 Dmitry Barashev, GanttProject Team

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

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.view.GPViewManager;
import net.sourceforge.ganttproject.search.SearchUi;
import net.sourceforge.ganttproject.undo.GPUndoManager;
import net.sourceforge.ganttproject.resource.HumanResourceManager;


public class InfoMenu extends JMenu {

  public InfoMenu(IGanttProject project, UIFacade uiFacade, GPViewManager viewManager, SearchUi searchUi, HumanResourceManager rm, String key) {
    super(GPAction.createVoidAction(key));

    add(new InfoResourcesDialogAction(project, uiFacade, rm));
    setToolTipText(null);
  }

  @Override
  public JMenuItem add(Action a) {
    a.putValue(Action.SHORT_DESCRIPTION, null);
    return super.add(a);
  }


}