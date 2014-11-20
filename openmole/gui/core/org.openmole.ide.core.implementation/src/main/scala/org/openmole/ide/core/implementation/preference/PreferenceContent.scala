/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.preference

import java.awt.Dimension
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.TabbedPane
import scala.swing.event.Key._

class PreferenceContent extends PluginPanel("wrap", "[right]", "") {
  preferredSize = new Dimension(650, 300)
  StatusBar.clear
  val authentification = new AuthenticationPanel
  val servers = new ServerListPanel

  contents += new TabbedPane {
    pages.append(new TabbedPane.Page("Authentication", authentification))
    pages.append(new TabbedPane.Page("Servers", servers))
    pages.append(new TabbedPane.Page("Other", new EnvironmentSettingPanel))
  }
  def save = {
    authentification.save
    servers.save
  }
}