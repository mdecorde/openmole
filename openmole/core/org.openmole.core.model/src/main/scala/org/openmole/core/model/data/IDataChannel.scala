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

package org.openmole.core.model.data


/**
 *
 * A data channel allow to transmit data between remotes task capsules within a mole.
 * Two capsules could be linked with a {@link IDataChannel} if:
 *      - they belong to the same mole,
 *      - there is no capsule with more than one input slot in a path between 
 *        the two capsules.
 *        
 */
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.core.model.task.ITask
import org.openmole.core.model.transition.ISlot

trait IDataChannel {

  /**
   *
   * Get the capsule from which the data channel starts.
   *
   * @return the capsule from whitch the data channel starts
   */
  def start: ICapsule


  /**
   *
   * Get the capsule to which the data channel ends.
   *
   * @return the slot to which the data channel ends.
   */
  def end:  ISlot


  /**
   *
   * Get the names of the variable transported by this data channel.
   *
   * @return the name of the variable not transported by this data channel.
   */
  def filtered: Iterable[String]

   
  /**
   *
   * Get the set of data of that will actually be transmitted as input to the
   * ending task capsule. This is computed by intersecting the set of variable
   * names transported by this data channel and the set of input of the ending
   * task.
   *
   * @return the transmitted data
   */
  def data: Iterable[IData[_]]

  /**
   * Provides the variable for future consuption by the matching execution of
   * the ending task.
   * 
   * @param context the context containing the variables
   * @param ticket the ticket of the current execution
   * @param toClone the set of variable wich should be cloned
   * @param moleExecution the current mole execution
   */
  def provides(context: IContext, ticket: ITicket, moleExecution: IMoleExecution)
    
  /**
   * Consums the provided variables and construct a context for them.
   * 
   * @param ticket the ticket of the current execution
   * @param moleExecution the current mole execution
   * @return the variables wich have been transmited through this data channel
   */
  def consums(sticket: ITicket, moleExecution: IMoleExecution): Iterable[IVariable[_]]

}
