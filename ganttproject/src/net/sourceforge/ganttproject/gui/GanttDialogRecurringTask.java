/*
GanttProject is an opensource project management tool.
Copyright (C) 2003-2011 GanttProject Team

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
package net.sourceforge.ganttproject.gui;

import biz.ganttproject.core.calendar.GanttDaysOff;
import biz.ganttproject.core.option.DefaultEnumerationOption;
import biz.ganttproject.core.option.DefaultMoneyOption;
import biz.ganttproject.core.option.DefaultStringOption;
import biz.ganttproject.core.option.DefaultIntegerOption;
import biz.ganttproject.core.option.EnumerationOption;
import biz.ganttproject.core.option.GPOption;
import biz.ganttproject.core.option.GPOptionGroup;
import biz.ganttproject.core.option.MoneyOption;
import biz.ganttproject.core.option.StringOption;
import biz.ganttproject.core.option.IntegerOption;
import net.sourceforge.ganttproject.CustomPropertyManager;
import net.sourceforge.ganttproject.action.CancelAction;
import net.sourceforge.ganttproject.action.OkAction;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DateInterval;
import net.sourceforge.ganttproject.gui.DateIntervalListEditor.DefaultDateIntervalModel;
import net.sourceforge.ganttproject.gui.options.OptionsPageBuilder;
import net.sourceforge.ganttproject.gui.taskproperties.CustomColumnsPanel;
import net.sourceforge.ganttproject.language.GanttLanguage;
import net.sourceforge.ganttproject.resource.HumanResource;
import net.sourceforge.ganttproject.roles.Role;
import net.sourceforge.ganttproject.roles.RoleManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

public class GanttDialogRecurringTask {
  private boolean change;
  private static final GanttLanguage language = GanttLanguage.getInstance();
  private JTabbedPane tabbedPane;
  private final IntegerOption myRepetitionsField = new DefaultIntegerOption("repetitions");
  private final IntegerOption myIntervalField = new DefaultIntegerOption("interval");
  private final GPOptionGroup myGroup;
  private final UIFacade myUIFacade;

  private int nRepetitions;

  private int interval;


  public GanttDialogRecurringTask(UIFacade uiFacade, int nRepetitions, int interval) {
    myUIFacade = uiFacade;
    this.nRepetitions = nRepetitions;
    this.interval = interval;
    Role[] enabledRoles = RoleManager.Access.getInstance().getEnabledRoles();
    String[] roleFieldValues = new String[enabledRoles.length];
    for (int i = 0; i < enabledRoles.length; i++) {
      roleFieldValues[i] = enabledRoles[i].getName();
    }
    myGroup = new GPOptionGroup("", new GPOption[] {myRepetitionsField, myIntervalField });
    myGroup.setTitled(false);
  }

  public boolean result() {
    return change;
  }

  public void setVisible(boolean isVisible) {
    if (isVisible) {
      Component contentPane = getComponent();
      OkAction okAction = new OkAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myGroup.commit();
          okButtonActionPerformed();
        }
      };
      CancelAction cancelAction = new CancelAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
          myGroup.rollback();
          change = false;
        }
      };
      myUIFacade.createDialog(contentPane, new Action[] { okAction, cancelAction }, language.getCorrectedLabel("recurringTask")).show();
    }
  }

  private Component getComponent() {
    OptionsPageBuilder builder = new OptionsPageBuilder();
    OptionsPageBuilder.I18N i18n = new OptionsPageBuilder.I18N() {
      @Override
      public String getOptionLabel(GPOptionGroup group, GPOption<?> option) {
        return getValue(option.getID());
      }
    };
    builder.setI18N(i18n);
    final JComponent mainPage = builder.buildPlanePage(new GPOptionGroup[] { myGroup });
    mainPage.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
    tabbedPane = new JTabbedPane();
    tabbedPane.addTab(language.getText("general"), new ImageIcon(getClass().getResource("/icons/properties_16.gif")), mainPage);
    myUIFacade.getResourceTree().getVisibleFields();

    tabbedPane.addFocusListener(new FocusAdapter() {
      boolean isFirstTime = true;

      @Override
      public void focusGained(FocusEvent e) {
        if (isFirstTime) {
          mainPage.requestFocus();
          isFirstTime = false;
        }
        super.focusGained(e);
      }

    });
    return tabbedPane;
  }

  private void okButtonActionPerformed() {

    nRepetitions = myRepetitionsField.getValue();
    interval = myIntervalField.getValue();
    change = true;
  }



}
