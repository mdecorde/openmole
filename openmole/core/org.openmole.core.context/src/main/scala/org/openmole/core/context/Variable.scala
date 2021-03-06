/*
 * Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.context

import org.openmole.core.tools.io.Prettifier._
import org.openmole.core.workspace.{ ConfigurationLocation, Workspace }
import org.openmole.tool.random

import scala.util.Random

object Variable {
  implicit def tupleWithValToVariable[T](t: (Val[T], T)) = apply(t._1, t._2)
  implicit def tubleToVariable[T: Manifest](t: (String, T)) = apply(Val[T](t._1), t._2)

  def apply[T](p: Val[T], v: T) = new Variable[T] {
    val prototype = p
    val value = v
  }

  def unsecure[T](p: Val[T], v: Any) = new Variable[T] {
    val prototype = p
    val value = v.asInstanceOf[T]
  }

  val OpenMOLEVariablePrefix = ConfigurationLocation("Variable", "OpenMOLEVariablePrefix", Some("oM"))

  Workspace setDefault OpenMOLEVariablePrefix

  def prefixedVariable(name: String) = Workspace.preference(OpenMOLEVariablePrefix) + name

  val openMOLESeed = Val[Long](prefixedVariable("Seed"))

}

trait Variable[T] {
  def prototype: Val[T]
  def value: T

  override def toString: String = prettified(Int.MaxValue)

  def prettified(snipArray: Int) = prototype.name + "=" + (if (value != null) value.prettify(snipArray) else "null")
}

