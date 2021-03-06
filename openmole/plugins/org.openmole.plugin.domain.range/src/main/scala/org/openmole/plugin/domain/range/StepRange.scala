/*
 * Copyright (C) 24/10/13 Romain Reuillon
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

package org.openmole.plugin.domain.range

import org.openmole.core.context.Context
import org.openmole.core.expansion.FromContext
import org.openmole.core.workflow.domain._
import org.openmole.tool.random.RandomProvider

object StepRange {

  implicit def isFinite[T] = new Finite[StepRange[T], T] with Bounds[StepRange[T], T] with Center[StepRange[T], T] {
    override def computeValues(domain: StepRange[T]) = FromContext((context, rng) ⇒ domain.computeValues(context)(rng))
    override def max(domain: StepRange[T]) = FromContext((context, rng) ⇒ domain.max.from(context)(rng))
    override def min(domain: StepRange[T]) = FromContext((context, rng) ⇒ domain.min.from(context)(rng))
    override def center(domain: StepRange[T]) = Range.rangeCenter(domain.range)
  }

  def apply[T](range: Range[T], step: FromContext[T]) = new StepRange[T](range, step)
}

class StepRange[T](val range: Range[T], steps: FromContext[T]) extends SizeStep[T] {
  import range._

  def stepAndSize(minValue: T, maxValue: T, context: Context)(implicit rng: RandomProvider) = {
    import ops._
    val step = steps.from(context)
    val size = (maxValue - minValue) / step
    (step, if (size.toInt < 0) 0 else size.toInt)
  }

  def min = range.min
  def max = range.max
}
