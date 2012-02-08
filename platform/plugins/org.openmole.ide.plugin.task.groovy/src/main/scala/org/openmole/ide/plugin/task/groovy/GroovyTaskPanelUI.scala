/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.task.groovy

import java.awt.Dimension
import org.openmole.ide.core.model.data.ITaskDataUI
import org.openmole.ide.core.model.panel.ITaskPanelUI
import org.openmole.ide.misc.widget.multirow.MultiChooseFileTextField
import scala.swing.FileChooser.SelectionMode._
import org.openmole.ide.misc.widget.GroovyEditor
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.Label

class GroovyTaskPanelUI(pud: GroovyTaskDataUI) extends MigPanel("fillx,wrap","[left,grow,fill]","") with ITaskPanelUI {
  
  val codeTextArea = new GroovyEditor
  codeTextArea.editor.text = pud.code
  codeTextArea.minimumSize = new Dimension(150,150)
  val libMultiTextField = new MultiChooseFileTextField("Lib",pud.libs,"Select a file", Some("Lib files"), FilesOnly,Some("jar"))
  val pluginMultiTextField = new MultiChooseFileTextField("Plugin",pud.plugins,"Select a file", Some("Plugin files"), FilesOnly,Some("jar"))
  
  contents += (new Label("Code"),"left")
  contents += (codeTextArea,"span,growx")
  contents += libMultiTextField.panel
  contents += pluginMultiTextField.panel
  
  override def saveContent(name: String):ITaskDataUI = new GroovyTaskDataUI(name,
                                                                            codeTextArea.text,
                                                                            libMultiTextField.content.filterNot(_.isEmpty),
                                                                            pluginMultiTextField.content.filterNot(_.isEmpty))
}

//  val editorPane= new JEditorPane
//    val kit= CloneableEditorSupport.getEditorKit("text/x-groovy")
//    editorPane.setEditorKit(kit)
//    val fob= FileUtil.createMemoryFileSystem().getRoot().createData("tmp","groovy")
//    val dob= DataObject.find(fob)
//    editorPane.getDocument.putProperty(Document.StreamDescriptionProperty, dob)
//    editorPane.setText("package dummy;")
//    
