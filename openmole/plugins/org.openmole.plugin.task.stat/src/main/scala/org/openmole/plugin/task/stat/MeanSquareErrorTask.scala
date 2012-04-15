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

package org.openmole.plugin.task.stat

import org.openmole.core.model.task.IPluginSet
import org.openmole.misc.math.Stat

object MeanSquareErrorTask {
  
  def apply(name: String)(implicit plugins: IPluginSet) = new DoubleSequenceStatTaskBuilder { builder =>
    def toTask = new MeanSquareErrorTask(name) {
      val sequences = builder.sequences()
      val inputs = builder.inputs
      val outputs = builder.outputs
      val parameters = builder.parameters
    }
  }
  
}

sealed abstract class MeanSquareErrorTask(val name: String)(implicit val plugins: IPluginSet) extends DoubleSequenceStatTask {
  
  override def stat(seq: Array[Double]) = Stat.meanSquareError(seq)

}