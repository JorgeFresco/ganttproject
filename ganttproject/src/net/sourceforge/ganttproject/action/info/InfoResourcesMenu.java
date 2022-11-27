/*
GanttProject is an opensource project management tool. License: GPL3
Copyright (C) 2005-2011 GanttProject Team

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

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.Date;

import javax.swing.Action;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.ganttproject.GPLogger;
import net.sourceforge.ganttproject.IGanttProject;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.GPAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.document.Document;
import net.sourceforge.ganttproject.document.DocumentManager;
import net.sourceforge.ganttproject.document.ReadOnlyProxyDocument;
import net.sourceforge.ganttproject.gui.ProjectUIFacade;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.UIUtil;
import net.sourceforge.ganttproject.gui.about.AboutDialog2;
import net.sourceforge.ganttproject.language.GanttLanguage;

/**
 * Collection of actions from Info Resources menu.
 *
 * @author dbarashev (Dmitry Barashev)
 */
public class InfoResourcesMenu {

    private final AboutAction myAboutAction;

    public InfoResourcesMenu(IGanttProject project, UIFacade uiFacade, ProjectUIFacade projectUiFacade) {
        myAboutAction = new AboutAction(uiFacade);
    }

    public JMenu createMenu() {
        JMenu result = UIUtil.createTooltiplessJMenu(GPAction.createVoidAction("projectInfo"));
        result.add(myAboutAction);
        return result;
    }

    private static class AboutAction extends GPAction {
        private final UIFacade myUiFacade;

        AboutAction(UIFacade uifacade) {
            super("about");
            myUiFacade = uifacade;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AboutDialog2 agp = new AboutDialog2(myUiFacade);
            agp.show();
        }
    }


}
