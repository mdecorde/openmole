package org.openmole.gui.client.core

import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import org.openmole.gui.shared.Api
import org.scalajs.dom.raw.HTMLFormElement
import rx.{ Rx, Var }
import sheet._
import scalatags.JsDom.all._
import scalatags.JsDom.tags
import org.openmole.gui.misc.js.JsRxTags._

/*
 * Copyright (C) 07/11/16 // mathieu.leclaire@openmole.org
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

class Connection {

  val shutDown = new ShutDown

  lazy val connectButton = tags.button("Connect", btn_primary, `type` := "submit").render

  val passwordInput = bs.input("")(
    placeholder := "Password",
    `type` := "password",
    width := "130px",
    sheet.marginBottom(15),
    name := "password",
    autofocus := true
  ).render

  def cleanInputs = {
    passwordInput.value = ""
  }

  def connectionForm: HTMLFormElement =
    tags.form(
      method := "post",
      passwordInput,
      connectButton
    ).render

  val connectionDiv = div(
    shutDown.shutdownButton,
    Rx {
      div(omsheet.connectionTabOverlay)(
        div(
          img(src := "img/openmole.png", omsheet.openmoleLogo),
          div(
            omsheet.centerPage,
            div(
              if (shutDown.alert.now)
                shutDown.alertPanel
              else {
                div(
                  omsheet.connectionBlock,
                  connectionForm
                )
              }
            )
          )
        )
      )
    }
  )

}
