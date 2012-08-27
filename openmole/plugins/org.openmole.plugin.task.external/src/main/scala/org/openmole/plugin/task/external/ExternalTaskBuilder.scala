/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.openmole.plugin.task.external

import java.io.File
import org.openmole.core.implementation.task.TaskBuilder
import org.openmole.core.implementation.data._
import org.openmole.core.model.data.IPrototype
import scala.collection.mutable.ListBuffer

/**
 * Builder for task using external files or directories
 *
 * Use it to copy files or directories, from the dataflow or from your computer in the task
 * workspace and prior to the task execution and to get files generated by the
 * task after its execution.
 *
 */
abstract class ExternalTaskBuilder extends TaskBuilder {

  private var _inputFiles = new ListBuffer[(IPrototype[File], String, Boolean)]
  private var _outputFiles = new ListBuffer[(String, IPrototype[File])]
  private var _resources = new ListBuffer[(File, String, Boolean)]

  def inputFiles = _inputFiles.toList
  def outputFiles = _outputFiles.toList
  def resources = _resources.toList

  /**
   * Copy a file from your computer in the workspace of the task
   *
   * @param file the file or directory to copy in the task workspace
   * @param name the destination name of the file in the task workspace, by
   * default it is the same as the orginal file name
   * @param link tels if the entire content of the file should be copied or
   * if a symbolic link is suitable. In the case link is set to true openmole will
   * try to use a symbolic link if available on your system.
   *
   */
  def addResource(file: File, name: String, link: Boolean): ExternalTaskBuilder.this.type = {
    _resources += ((file, name, link))
    this
  }

  def addResource(file: File, name: String): this.type = this.addResource(file, name, false)
  def addResource(file: File, link: Boolean): this.type = this.addResource(file, file.getName, link)
  def addResource(file: File): this.type = this.addResource(file, false)

  /**
   * Copy a file or directory from the dataflow to the task workspace
   *
   * @param p the prototype of the data containing the file to be copied
   * @param name the destination name of the file in the task workspace
   * @param link @see addResouce
   *
   */
  def addInput(p: IPrototype[File], name: String, link: Boolean): this.type = {
    _inputFiles += ((p, name, link))
    this addInput p
    this
  }

  def addInput(p: IPrototype[File], name: String): ExternalTaskBuilder.this.type = this.addInput(p, name, false)

  /**
   * Get a file generate by the task and inject it in the dataflow
   *
   * @param name the name of the file to be injected
   * @param p the prototype that is injected
   *
   */
  def addOutput(name: String, p: IPrototype[File]): this.type = {
    _outputFiles += ((name, p))
    this addOutput p
    this
  }

}
