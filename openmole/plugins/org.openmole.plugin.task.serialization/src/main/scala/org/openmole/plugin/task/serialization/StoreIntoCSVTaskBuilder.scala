/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.task.serialization

import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.task.IPluginSet

abstract class StoreIntoCSVTaskBuilder(implicit plugins: IPluginSet) extends TaskBuilder {

  var _columns: List[(IPrototype[Array[_]], String)] = List.empty
  
  def columns = new {
    def +=(prototype: IPrototype[Array[_]], columnName: String): StoreIntoCSVTaskBuilder.this.type = {
      inputs += prototype
      _columns ::= (prototype -> columnName)
      StoreIntoCSVTaskBuilder.this
    }
    
    def +=(prototype: IPrototype[Array[_]]): StoreIntoCSVTaskBuilder.this.type = this.+=(prototype, prototype.name)
    
    def apply() = _columns.reverse
  }
  
}
