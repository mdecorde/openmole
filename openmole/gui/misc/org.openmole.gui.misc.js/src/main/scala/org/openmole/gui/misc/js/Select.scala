package org.openmole.gui.misc.js

/*
 * Copyright (C) 13/01/15 // mathieu.leclaire@openmole.org
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

import rx._
import scalatags.JsDom.all._
import org.scalajs.jquery.jQuery

class Select[T <: Displayable with Identifiable](autoID: String,
                                                 val contents: Var[Seq[T]],
                                                 default: Option[T] = None,
                                                 key: ClassKeyAggregator = Forms.emptyCK) {

  val jQid = "#" + autoID

  lazy val content: Var[Option[T]] = Var(contents().size match {
    case 0 ⇒ None
    case _ ⇒ default match {
      case None ⇒ Some(contents()(0))
      case _ ⇒
        val ind = contents().indexOf(default.get)
        if (ind != -1) Some(contents()(ind)) else Some(contents()(0))
    }
  })

  val selector =
    Forms.select(key, contents().map { c ⇒ c.uuid -> c.name })(
      onchange := { () ⇒ applyOnChange }
    )

  def applyOnChange: Unit = {
    content() = Some(contents()(jQuery(jQid).find("option:selected").index()))
  }

  def set(t: T) = jQuery(jQid).`val`(t.uuid)
}