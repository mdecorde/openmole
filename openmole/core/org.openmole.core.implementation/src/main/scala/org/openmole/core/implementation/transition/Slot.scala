/*
 * Copyright (C) 2010 reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.implementation.transition

import org.openmole.core.model.data.IDataChannel
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.ITransition
import org.openmole.core.model.transition.ISlot
import scala.collection.mutable.HashSet
import org.openmole.misc.tools.io.StringUtil._
import scala.collection.mutable.ListBuffer

class Slot(val capsule: ICapsule) extends ISlot {
  
  capsule.addInputSlot(this)
  
  private val _transitions = new ListBuffer[ITransition]
  private val _inputDataChannels = new ListBuffer[IDataChannel]
  
  override def +=(transition: ITransition) = {
    _transitions += transition
    this
  }


  override def addInputDataChannel(dataChannel: IDataChannel): this.type = {
    _inputDataChannels += dataChannel
    this
  }
  
  
  override def inputDataChannels: Iterable[IDataChannel] = _inputDataChannels

  override def transitions: Iterable[ITransition] =  _transitions
    
  override def contains(transition: ITransition) = _transitions.contains(transition)
  
  override def toString = "slot of capsule " + capsule + " containing transitions from (" + transitions.map{_.start}.mkString(",") + ")"

}
