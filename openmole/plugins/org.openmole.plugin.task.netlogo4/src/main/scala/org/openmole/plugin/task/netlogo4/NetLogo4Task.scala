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

package org.openmole.plugin.task.netlogo4

import org.openmole.core.model.task.IPluginSet
import org.openmole.core.model.data._
import org.openmole.plugin.task.netlogo._
import NetLogoTask.Workspace
import org.openmole.core.implementation.task._
import java.io.File
import collection.JavaConversions._

object NetLogo4Task {
  
  def factory = new NetLogoFactory {
    def apply = new NetLogo4
  }
  
  def apply(
    name: String,
    workspace: File,
    script: String,
    launchingCommands: Iterable[String]
  )(implicit plugins: IPluginSet) = {
    val _launchingCommands = launchingCommands
    val (_workspace, _script) = (workspace, script)
    
    new NetLogoTaskBuilder { builder =>
      
      provided += workspace
      
      def toTask = new NetLogoTask(
        name,
        workspace = new Workspace(_workspace, _script),
        launchingCommands = _launchingCommands,
        inputs = builder.inputs,
        outputs = builder.outputs,
        parameters = builder.parameters,
        provided = builder.provided(),
        produced = builder.produced(),
        netLogoInputs = builder.netLogoInputs(),
        netLogoOutputs = builder.netLogoOutputs(),
        netLogoFactory = factory
      )
    }
  }
  
  def apply(
    name: String,
    script: File,
    launchingCommands: Iterable[String]
  )(implicit plugins: IPluginSet) = {
    val _launchingCommands = launchingCommands
    new NetLogoTaskBuilder { builder =>
  
      provided += script
      
      def toTask = new NetLogoTask(
        name,
        launchingCommands = _launchingCommands,
        workspace = new Workspace(script),
        inputs = builder.inputs,
        outputs = builder.outputs,
        parameters = builder.parameters,
        provided = builder.provided(),
        produced = builder.produced(),
        netLogoInputs = builder.netLogoInputs(),
        netLogoOutputs = builder.netLogoOutputs(),
        netLogoFactory = factory
      )
    }
  }
}


