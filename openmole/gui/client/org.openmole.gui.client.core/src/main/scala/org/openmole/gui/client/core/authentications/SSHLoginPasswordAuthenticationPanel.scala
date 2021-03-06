package org.openmole.gui.client.core.authentications

import org.openmole.gui.client.core.OMPost
import org.openmole.gui.ext.data.LoginPasswordAuthenticationData
import org.openmole.gui.misc.utils.Utils
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import org.openmole.gui.ext.dataui.PanelUI
import org.openmole.gui.shared.Api
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import scalatags.JsDom.all._
import AuthenticationUtils._

/*
 * Copyright (C) 16/06/15 // mathieu.leclaire@openmole.org
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

class SSHLoginPasswordAuthenticationPanel(data: LoginPasswordAuthenticationData = LoginPasswordAuthenticationData()) extends PanelUI {

  val login = loginInput(data.login)
  val target = targetInput(data.target)
  val port = portInput(data.port)
  val password = passwordInput(data.cypheredPassword)

  val view = form(sheet.formInline)(
    for {
      e ← Seq(login, password, target, port)
    } yield {
      e.render
    }
  )

  def save(onsave: () ⇒ Unit) = {
    OMPost[Api].removeAuthentication(data).call().foreach { d ⇒
      OMPost[Api].addAuthentication(LoginPasswordAuthenticationData(login.value, password.value, target.value, port.value)).call().foreach { b ⇒
        onsave()
      }
    }
  }

}