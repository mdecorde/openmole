package org.openmole.gui.plugin.task.groovy.client

import org.openmole.gui.ext.dataui.{TaskDataUI}
import org.openmole.gui.ext.factoryui.{FactoryUI}

import scala.scalajs.js.annotation.JSExport
import rx._

/*
 * Copyright (C) 01/10/14 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

@JSExport("org.openmole.gui.plugin.task.groovy.client.GroovyTaskFactoryUI")
class GroovyTaskFactoryUI extends FactoryUI {
  type DATAUI = TaskDataUI

  def dataUI = new GroovyTaskDataUI

  val name = Var("Groovy Task")
}