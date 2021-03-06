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

object SizeRange {
  implicit def isFinite[T] = new Finite[SizeRange[T], T] with Bounds[SizeRange[T], T] with Center[SizeRange[T], T] {
    override def computeValues(domain: SizeRange[T]) = FromContext((context, rng) ⇒ domain.computeValues(context)(rng))
    override def max(domain: SizeRange[T]) = FromContext((context, rng) ⇒ domain.max.from(context)(rng))
    override def min(domain: SizeRange[T]) = FromContext((context, rng) ⇒ domain.min.from(context)(rng))
    override def center(domain: SizeRange[T]) = Range.rangeCenter(domain.range)
  }

  def apply[T: RangeValue](min: FromContext[T], max: FromContext[T], size: FromContext[Int]): SizeRange[T] =
    apply(Range(min, max), size)

  def apply[T](range: Range[T], size: FromContext[Int]): SizeRange[T] =
    new SizeRange[T](range, size)

}

class SizeRange[T](val range: Range[T], size: FromContext[Int]) extends SizeStep[T] {
  import range._

  def stepAndSize(minValue: T, maxValue: T, context: Context)(implicit rng: RandomProvider) = {
    import ops._
    val s = size.from(context) - 1
    val step = (maxValue - minValue) / fromInt(s)
    (step, s.toInt)
  }

  def min = range.min
  def max = range.max
}
