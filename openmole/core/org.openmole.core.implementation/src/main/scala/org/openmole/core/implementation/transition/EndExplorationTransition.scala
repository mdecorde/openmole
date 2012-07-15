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

package org.openmole.core.implementation.transition

import org.openmole.core.model.data.IContext
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.transition.ICondition._
import org.openmole.core.model.mole.ISubMoleExecution
import org.openmole.core.model.mole.ITicket
import org.openmole.core.model.transition.ICondition
import org.openmole.core.model.transition.ISlot
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.exception.UserBadDataError

class EndExplorationTransition(start: ICapsule, end: ISlot, trigger: ICondition, filtered: Iterable[String] = Iterable.empty) extends Transition(start, end, True, filtered) {

  override protected def _perform(context: IContext, ticket: ITicket, subMole: ISubMoleExecution) = {
    if (trigger.evaluate(context)) {
      val parentTicket = ticket.parent.getOrElse(throw new UserBadDataError("End exploration transition should take place after an exploration."))
      val subMoleParent = subMole.parent.getOrElse(throw new InternalProcessingError("Submole execution has no parent"))
      super._perform(context, parentTicket, subMoleParent)
      subMole.cancel
    }
  }

}
