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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;

import net.sourceforge.ganttproject.GPVersion;
import net.sourceforge.ganttproject.gui.AbstractPagesDialog;
import net.sourceforge.ganttproject.gui.NotificationManager;
import net.sourceforge.ganttproject.gui.UIFacade;
import net.sourceforge.ganttproject.gui.options.TopPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResourceManager;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.task.ResourceAssignment;
import net.sourceforge.ganttproject.task.TaskManager;
import net.sourceforge.ganttproject.task.Task;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class InfoDialog2 extends AbstractPagesDialog {

  private static final Color HTML_BACKGROUND = new JPanel().getBackground();

  //private HumanResourceManager myRm;

  public InfoDialog2(UIFacade uiFacade, HumanResourceManager rm, TaskManager tm) {

    super("infoTitle", uiFacade, createPages(rm,tm));
  }


  private static List<ListItem> createPages(HumanResourceManager rm, TaskManager tm) {

    GanttLanguage language = GanttLanguage.getInstance();

    List<ListItem> result = new ArrayList<AbstractPagesDialog.ListItem>();

    //--Total tasks values--\\
    SimpleDateFormat formatter = new SimpleDateFormat(language.getText("dateFormat"));
    Date date = new Date();
    String startDate = formatter.format(tm.getProjectStart()).toString();
    String endDate = formatter.format(tm.getProjectEnd()).toString();
    System.out.println(formatter.format(date));

    Task[] tasks = tm.getTasks();
    int nOfCompletedTasks = 0;
    for(int i=0; i< tasks.length; i++) {
      if(tasks[i].getCompletionPercentage() == 100){
        nOfCompletedTasks ++;
      }
    }

    String totalTasksInfo = language.formatText("totalTasksInfo", startDate, endDate,nOfCompletedTasks,tasks.length);

    result.add(createHtmlPage(" ", language.getText("totalProjectInfo"), totalTasksInfo));


    //--Resources values--\\
    List<HumanResource> resources = rm.getResources();
    Iterator<HumanResource> it = resources.iterator();
    HumanResource current;
    while(it.hasNext()) {
      current = it.next();
      ResourceAssignment[] resourceAssignments = current.getAssignments();
      int tasksDone = 0;
      int workDone = 0;
      float a;
      Task task;
      for (ResourceAssignment resourceAssignment : resourceAssignments) {
        task = resourceAssignment.getTask();
        workDone += task.getCompletionPercentage();
        if(task.getCompletionPercentage() == 100){
          tasksDone++;
        }

      }

      if(resourceAssignments.length>0) {
        workDone /= resourceAssignments.length;
      }
      String info =  language.formatText("resourceInfo", current.getName(), tasksDone, resourceAssignments.length, workDone);
      String resourceInfo = current.getName() + " ("+current.getId()+")";
      result.add(createHtmlPage(Integer.toString(current.getId()), resourceInfo, info));
    }

    return result;
  }

  private static ListItem createHtmlPage(String key, String title, String body) {
    JPanel result = new JPanel(new BorderLayout());
    JComponent topPanel = TopPanel.create(title, null);
    topPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
    result.add(topPanel, BorderLayout.NORTH);

    JPanel planePageWrapper = new JPanel(new BorderLayout());
    planePageWrapper.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
    JComponent planePage = createHtml(body);
    planePage.setAlignmentX(Component.LEFT_ALIGNMENT);
    planePageWrapper.add(planePage, BorderLayout.NORTH);
    result.add(planePageWrapper, BorderLayout.CENTER);

    return new ListItem(false, key, title, result);
  }

  private static JEditorPane createHtml(String html) {
    JEditorPane htmlPane = new JEditorPane("text/html", html);
    htmlPane.setEditable(false);
    htmlPane.setBackground(HTML_BACKGROUND);
    htmlPane.addHyperlinkListener(NotificationManager.DEFAULT_HYPERLINK_LISTENER);
    Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
    htmlPane.setSize(new Dimension(screenSize.width / 3, Integer.MAX_VALUE));
    return htmlPane;
  }

  @Override
  protected void onOk() {
  }
}
