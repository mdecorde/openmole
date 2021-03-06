package org.openmole.gui.client.core.files

import java.text.SimpleDateFormat
import java.util.Date

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ CenterPagePosition, FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files.FileToolBar.{ FilterTool, PluginTool, TrashTool }
import org.openmole.gui.client.core.{ CoreUtils, OMPost }
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.utils.{ Utils, stylesheet }
import org.openmole.gui.shared._
import fr.iscpif.scaladget.api.{ Popup, BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw._

import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }

import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import TreeNode._
import autowire._
import rx._
import bs._
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import org.openmole.gui.misc.utils
import org.scalajs.dom
import sheet._

import scala.concurrent.Future
import scala.scalajs.js

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
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

object TreeNodePanel {
  val instance = new TreeNodePanel

  def apply() = instance

  def refreshAnd(todo: () ⇒ Unit) = {
    instance.invalidCacheAnd(todo)
  }

  def refreshAndDraw = instance.invalidCacheAndDraw

}

class TreeNodePanel {

  val selectionMode = Var(false)
  val treeWarning = Var(true)

  selectionMode.trigger {
    if (!selectionMode.now) manager.clearSelection
  }

  def turnSelectionTo(b: Boolean) = selectionMode() = b

  val fileDisplayer = new FileDisplayer
  val fileToolBar = new FileToolBar(this)
  val tree: Var[TypedTag[HTMLElement]] = Var(tags.div())

  val editNodeInput: Input = bs.input()(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    autofocus
  ).render

  lazy val fileControler = Rx {
    val current = manager.current()
    div(ms("tree-path"))(
      goToDirButton(manager.root, glyph_home +++ floatLeft +++ "treePathItems"),
      Seq(current.parent, current).filterNot { sp ⇒
        sp.isEmpty || sp == manager.root
      }.map { sp ⇒
        goToDirButton(sp, "treePathItems", s"| ${sp.name}")
      }
    )
  }

  lazy val labelArea =
    div(
      Rx {
        if (manager.copied().isEmpty) tags.div
        else tags.label("paste")(label_danger, stylesheet.pasteLabel, onclick := { () ⇒ paste(manager.copied(), manager.current()) })
      },
      fileToolBar.sortingGroup.div
    )

  lazy val view = {
    drawTree
    tags.div(
      Rx {
        tree()
      }
    )
  }

  private def paste(safePaths: Seq[SafePath], to: SafePath) = {
    def refreshWithNoError = {
      manager.noError
      invalidCacheAndDraw
    }

    def onpasted = {
      manager.emptyCopied
      // unselectTool
    }

    val same = safePaths.filter { sp ⇒
      sp == to
    }
    if (same.isEmpty) {
      CoreUtils.testExistenceAndCopyProjectFilesTo(safePaths, to).foreach { existing ⇒
        if (existing.isEmpty) {
          refreshWithNoError
          onpasted
        }
        else manager.setFilesInError(
          "Some files already exists, overwrite ?",
          existing,
          () ⇒ CoreUtils.copyProjectFilesTo(safePaths, to).foreach { b ⇒
            refreshWithNoError
            onpasted
          }, () ⇒ {
            refreshWithNoError
            // unselectTool
          }
        )
      }
    }
    else manager.setFilesInComment(
      "Paste a folder in itself is not allowed",
      same,
      () ⇒ manager.noError
    )
  }

  def filter: FileFilter = fileToolBar.fileFilter.now

  def downloadFile(safePath: SafePath, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) =
    FileManager.download(
      safePath,
      (p: ProcessState) ⇒ {
        fileToolBar.transferring() = p
      },
      onLoaded
    )

  def goToDirButton(safePath: SafePath, ck: ModifierSeq, name: String = "") = span(ck)(name)(
    onclick := {
      () ⇒
        fileToolBar.clearMessage
        manager.switch(safePath)
        fileToolBar.unselectTool
        drawTree
    }
  )

  def invalidCacheAndDraw = {
    invalidCacheAnd(() ⇒ {
      drawTree
    })
  }

  def invalidCacheAnd(todo: () ⇒ Unit) = {
    manager.invalidCurrentCache
    todo()
  }

  def computePluggables = fileToolBar.selectedTool.now match {
    case Some(PluginTool) ⇒ manager.computePluggables(() ⇒ if (!manager.pluggables.now.isEmpty) turnSelectionTo(true))
    case _                ⇒
  }

  def drawTree: Unit = {
    computePluggables
    tree() = manager.computeCurrentSons(filter).withFutureWaiter("Get files", (sons: ListFiles) ⇒ {

      tags.table(
        if (manager.isRootCurrent && manager.isProjectsEmpty) {
          div("Create a first OpenMOLE script (.oms)")(ms("message"))
        }
        else {
          tbody(
            backgroundColor := Rx {
              if (selectionMode()) stylesheet.BLUE else stylesheet.DARK_GREY
            },
            omsheet.fileList,
            Rx {
              if (sons.list.length < sons.nbFilesOnServer && treeWarning()) {
                div(stylesheet.moreEntries)(
                  div(
                    stylesheet.moreEntriesText,
                    div(
                      s"Only 1000 files maximum (${100000 / sons.nbFilesOnServer}%) can be displayed.",
                      div(
                        "Use the ",
                        span(
                          "Filter tool",
                          pointer +++ omsheet.color(stylesheet.BLUE),
                          onclick := { () ⇒ fileToolBar.selectTool(FilterTool) }
                        ), " to refine your search"
                      )
                    )
                  )
                )
              }
              else div()
            },
            for (tn ← sons.list) yield {
              drawNode(tn).render
            }

          )
        }
      )
    })

  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      ReactiveLine(fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ ReactiveLine(dn, TreeNodeType.folder, () ⇒ {
      manager switch (dn.name.now)
      fileToolBar.clearMessage
      fileToolBar.unselectTool
      treeWarning() = true
      drawTree
    })
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      val ext = DataUtils.fileToExtension(tn.name.now)
      val tnSafePath = manager.current.now ++ tn.name.now
      if (ext.displayable) {
        downloadFile(tnSafePath, false, (content: String) ⇒ {
          fileDisplayer.display(tnSafePath, content, ext)
          invalidCacheAndDraw
        })
      }
    case _ ⇒
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    AlertPanel.string(message, okaction, transform = RelativeCenterPosition, zone = FileZone)

  def stringAlertWithDetails(message: String, detail: String) =
    AlertPanel.detail(message, detail, transform = RelativeCenterPosition, zone = FileZone)

  def trashNode(safePath: SafePath): Unit = {
    stringAlert(
      s"Do you really want to delete ${
        safePath.name
      }?",
      () ⇒ {
        CoreUtils.trashNode(safePath) {
          () ⇒
            fileDisplayer.tabs -- safePath
            fileDisplayer.tabs.checkTabs
            invalidCacheAndDraw
        }
      }
    )
  }

  def extractTGZ(safePath: SafePath) =
    OMPost[Api].extractTGZ(safePath).call().foreach {
      r ⇒
        r.error match {
          case Some(e: org.openmole.gui.ext.data.Error) ⇒ stringAlertWithDetails("An error occurred during extraction", e.stackTrace)
          case _                                        ⇒ invalidCacheAndDraw
        }
    }

  object ReactiveLine {
    def apply(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) = new ReactiveLine(tn, treeNodeType, todo)
  }

  class ReactiveLine(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val tnSafePath = manager.current.now ++ tn.name.now

    case class TreeStates(settingsSet: Boolean, edition: Boolean, replication: Boolean, selected: Boolean = manager.isSelected(tn)) {
      def settingsOn = treeStates() = copy(settingsSet = true)

      def editionOn = treeStates() = copy(edition = true)

      def replicationOn = treeStates() = copy(replication = true)

      def settingsOff = treeStates() = copy(settingsSet = false)

      def editionOff = treeStates() = copy(edition = false)

      def replicationOff = treeStates() = copy(replication = false)

      def editionAndReplicationOn = treeStates() = copy(edition = true, replication = true)

      def setSelected(b: Boolean) = treeStates() = copy(selected = b)
    }

    private val treeStates: Var[TreeStates] = Var(TreeStates(false, false, false))

    val clickablePair = {
      val style = floatLeft +++ pointer +++ Seq(
        onclick := { (e: MouseEvent) ⇒
          if (!selectionMode.now) {
            todo()
          }
        }
      )

      tn match {
        case fn: FileNode ⇒ span(span(sheet.paddingTop(4)), stylesheet.file +++ style)(div(stylesheet.fileNameOverflow)(tn.name.now))
        case dn: DirNode ⇒
          span(
            span(ms(dn.isEmpty, emptyMod, omsheet.fileIcon +++ glyph_plus)),
            (stylesheet.dir +++ style)
          )(div(stylesheet.fileNameOverflow +++ sheet.paddingLeft(22))(tn.name.now))
      }
    }

    def renameNode(safePath: SafePath, newName: String, replicateMode: Boolean) = {
      def rename = OMPost[Api].renameFile(safePath, newName).call().foreach {
        newNode ⇒
          fileDisplayer.tabs.rename(safePath, newNode)
          treeStates.now.editionOff
          invalidCacheAndDraw
          fileDisplayer.tabs.checkTabs
      }

      fileDisplayer.tabs.saveAllTabs(() ⇒ {
        OMPost[Api].existsExcept(safePath.copy(path = safePath.path.dropRight(1) :+ newName), replicateMode).call().foreach {
          b ⇒
            if (b) stringAlert(s"${
              newName
            } already exists, overwrite ?", () ⇒ rename)
            else rename
        }
      })
    }

    def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter.now.fileSorting match {
      case TimeSorting ⇒ CoreUtils.longTimeToString(tn.time)
      case _           ⇒ CoreUtils.readableByteCount(tn.size)
    }

    def clearSelectionExecpt(safePath: SafePath) = {
      treeStates.now.setSelected(true)
      manager.clearSelectionExecpt(safePath)
    }

    def addToSelection(b: Boolean): Unit = {
      treeStates.now.setSelected(b)
      manager.setSelected(tnSafePath, treeStates.now.selected)
    }

    def addToSelection: Unit = addToSelection(!treeStates.now.selected)

    val render: TypedTag[dom.html.TableRow] = {
      val baseGlyph = sheet.marginTop(2) +++ "glyphitem"
      val settingsGlyph = ms("glyphitem") +++ glyph_settings +++ sheet.paddingLeft(4)
      val trash = baseGlyph +++ glyph_trash
      val edit = baseGlyph +++ glyph_edit
      val download_alt = baseGlyph +++ glyph_download_alt
      val archive = baseGlyph +++ glyph_archive
      val arrow_right_and_left = baseGlyph +++ glyph_arrow_right_and_left

      tr(
        Rx {
          if (treeStates().edition) {
            editNodeInput.value = tn.name.now
            td(
              height := 26,
              form(
                editNodeInput,
                onsubmit := {
                  () ⇒
                    {
                      treeStates().editionOff
                      renameNode(tnSafePath, editNodeInput.value, treeStates().replication)
                      false
                    }
                }
              )
            )
          }
          else
            td(
              onclick := { (e: MouseEvent) ⇒
                {
                  if (selectionMode.now) {
                    addToSelection
                    if (e.ctrlKey) clearSelectionExecpt(tnSafePath)
                  }
                }
              },
              clickablePair.tooltip(
                tags.span(tn.name()), popupStyle = whitePopup, arrowStyle = Popup.whiteBottomArrow, condition = () ⇒ tn.name().length > 24
              ), {
                div(stylesheet.fileInfo)(
                  if (treeStates().settingsSet) {
                    span(
                      span(onclick := { () ⇒ treeStates().settingsOff }, baseGlyph)(
                        raw("&#215")
                      ),
                      tags.span(onclick := { () ⇒
                        trashNode(tnSafePath)
                        treeStates().settingsOff
                      }, trash),
                      span(onclick := { () ⇒
                        treeStates().editionOn
                      }, edit),
                      a(
                        span(onclick := { () ⇒ treeStates().settingsOff })(download_alt),
                        href := s"downloadFile?path=${Utils.toURI(tnSafePath.path)}"
                      ),
                      DataUtils.fileToExtension(tn.name.now) match {
                        case FileExtension.TGZ | FileExtension.TAR ⇒
                          span(archive, onclick := { () ⇒
                            extractTGZ(tnSafePath)
                          })
                        case _ ⇒
                      },
                      span(onclick := { () ⇒
                        val newName = {
                          val prefix = tnSafePath.path.last
                          tn match {
                            case _: DirNode ⇒ prefix + "_1"
                            case _          ⇒ prefix.replaceFirst("[.]", "_1.")
                          }
                        }

                        val replicateInput = bs.input(newName).render
                        AlertPanel.div(
                          div(width := 250, sheet.floatRight, sheet.marginRight(70), replicateInput),
                          () ⇒ CoreUtils.replicate(tnSafePath, replicateInput.value),
                          transform = RelativeCenterPosition,
                          zone = FileZone,
                          alertType = btn_primary,
                          buttonGroupClass = stylesheet.divAlertPosition
                        )
                      })(arrow_right_and_left)
                    )
                  }
                  else
                    span(stylesheet.fileSize)(
                      tags.i(timeOrSize(tn)),
                      tags.span(onclick := { () ⇒
                        treeStates().settingsOn
                      }, settingsGlyph)
                    )
                )
              },
              div(
                width := "100%",
                if (treeStates().selected) {
                  fileToolBar.selectedTool() match {
                    case Some(TrashTool) ⇒ stylesheet.fileSelectedForDeletion
                    case Some(PluginTool) if manager.pluggables().contains(tn) ⇒ stylesheet.fileSelected
                    case _ ⇒ stylesheet.fileSelected
                  }
                }
                else stylesheet.fileSelectionOverlay,
                span(stylesheet.fileSelectionMessage)
              )
            )
        }
      )
    }
  }

}