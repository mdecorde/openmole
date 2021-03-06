/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.plugin.hook.display

import java.io.PrintStream

import monocle.macros.Lenses
import org.openmole.core.context.{ Context, Val }
import org.openmole.core.workflow.builder.{ InputOutputBuilder, InputOutputConfig }
import org.openmole.core.workflow.mole._
import org.openmole.tool.random.RandomProvider

object ToStringHook {

  implicit def isIO: InputOutputBuilder[ToStringHook] = InputOutputBuilder(ToStringHook.config)

  def apply(prototypes: Val[_]*): ToStringHook =
    apply(System.out, prototypes: _*)

  def apply(out: PrintStream, prototypes: Val[_]*) =
    new ToStringHook(
      prototypes.toVector,
      config = InputOutputConfig()
    )

}

@Lenses case class ToStringHook(
    prototypes: Vector[Val[_]],
    config:     InputOutputConfig
) extends Hook {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    if (!prototypes.isEmpty) {
      val filtered = Context(prototypes.flatMap(p ⇒ context.variable(p.asInstanceOf[Val[Any]])): _*)
      executionContext.out.println(filtered.toString)
    }
    else executionContext.out.println(context.toString)
    context
  }

}
