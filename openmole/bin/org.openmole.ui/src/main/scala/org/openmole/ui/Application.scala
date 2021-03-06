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

package org.openmole.ui

import java.awt.Desktop
import java.io.{ File, FileOutputStream }
import java.net.URI

import org.openmole.core.project._
import org.openmole.core.console.ScalaREPL
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.logging.LoggerService
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.replication.DBServerRunning
import org.openmole.core.workspace.Workspace
import org.openmole.rest.server.RESTServer
import org.openmole.tool.logger.Logger

import annotation.tailrec
import org.openmole.gui.server.core._
import org.openmole.console._
import org.openmole.tool.file._
import org.openmole.tool.hash._
import org.openmole.core.module

object Application extends Logger {

  import Log._

  lazy val consoleSplash =
    """  ___                   __  __  ___  _     _____    __
      | / _ \ _ __   ___ _ __ |  \/  |/ _ \| |   | ____|  / /_
      || | | | '_ \ / _ \ '_ \| |\/| | | | | |   |  _|   | '_ \
      || |_| | |_) |  __/ | | | |  | | |_| | |___| |___  | (_) |
      | \___/| .__/ \___|_| |_|_|  |_|\___/|_____|_____|  \___/
      |      |_|
      |""".stripMargin

  lazy val consoleUsage = "(Type :q to quit)"

  def run(args: Array[String]): Int = DBServerRunning.useDB {

    sealed trait LaunchMode
    object ConsoleMode extends LaunchMode
    object GUIMode extends LaunchMode
    object HelpMode extends LaunchMode
    object RESTMode extends LaunchMode
    case class Reset(initialisePassword: Boolean) extends LaunchMode

    case class Config(
      userPlugins:          List[String]    = Nil,
      loadHomePlugins:      Option[Boolean] = None,
      workspaceDir:         Option[String]  = None,
      scriptFile:           Option[String]  = None,
      consoleWorkDirectory: Option[File]    = None,
      password:             Option[String]  = None,
      passwordFile:         Option[File]    = None,
      hostName:             Option[String]  = None,
      launchMode:           LaunchMode      = GUIMode,
      ignored:              List[String]    = Nil,
      port:                 Option[Int]     = None,
      loggerLevel:          Option[String]  = None,
      unoptimizedJS:        Boolean         = false,
      remote:               Boolean         = false,
      http:                 Boolean         = false,
      browse:               Boolean         = true,
      args:                 List[String]    = Nil
    )

    def takeArg(args: List[String]) =
      args match {
        case h :: t ⇒ h
        case Nil    ⇒ ""
      }

    def dropArg(args: List[String]) =
      args match {
        case h :: t ⇒ t
        case Nil    ⇒ Nil
      }

    def takeArgs(args: List[String]) = args.takeWhile(!_.startsWith("-"))
    def dropArgs(args: List[String]) = args.dropWhile(!_.startsWith("-"))

    def usage =
      """Usage: openmole [options]
      |
      |[-p | --plugin list of arg] plugins list of jar or category containing jars to be loaded
      |[-c | --console] console mode
      |[--port port] specify the port for the GUI or REST API
      |[--script path] a path of on OpenMOLE script to execute
      |[--password password] openmole password
      |[--password-file file containing a password] read the OpenMOLE password (--password option) in a file
      |[--rest] run the REST server
      |[--remote] enable remote connection to the web interface
      |[--http] force http connection instead of https in remote mode for the web interface
      |[--no-browser] don't automatically launch the browser in GUI mode
      |[--load-workspace-plugins] load the plugins of the OpenMOLE workspace (these plugins are always loaded in GUI mode)
      |[--console-work-directory] specify the workDirectory variable in console mode (it is set to the current directory by default)
      |[--reset] reset all preferences and authentications
      |[--reset-password] reset all preferences and ask for the a password
      |[--mem memory] allocate more memory to the JVM (not supported on windows yes), for instance --mem 2G
      |[--] end of options the remaining arguments are provided to the console in the args array
      |[-h | --help] print help""".stripMargin

    def parse(args: List[String], c: Config = Config()): Config = {
      def plugins(tail: List[String]) = parse(dropArgs(tail), c.copy(userPlugins = takeArgs(tail)))
      def help(tail: List[String]) = parse(tail, c.copy(launchMode = HelpMode))
      def script(tail: List[String]) = parse(dropArg(tail), c.copy(scriptFile = Some(takeArg(tail)), launchMode = ConsoleMode))
      def console(tail: List[String]) = parse(tail, c.copy(launchMode = ConsoleMode))
      args match {
        case "-p" :: tail                       ⇒ plugins(tail)
        case "--plugins" :: tail                ⇒ plugins(tail)
        case "-c" :: tail                       ⇒ console(tail)
        case "--console" :: tail                ⇒ console(tail)
        case "-s" :: tail                       ⇒ script(tail)
        case "--script" :: tail                 ⇒ script(tail)
        case "--port" :: tail                   ⇒ parse(tail.tail, c.copy(port = Some(tail.head.toInt)))
        case "--password" :: tail               ⇒ parse(dropArg(tail), c.copy(password = Some(takeArg(tail))))
        case "--password-file" :: tail          ⇒ parse(dropArg(tail), c.copy(passwordFile = Some(new File(takeArg(tail)))))
        case "--rest" :: tail                   ⇒ parse(tail, c.copy(launchMode = RESTMode))
        case "--load-workspace-plugins" :: tail ⇒ parse(tail, c.copy(loadHomePlugins = Some(true)))
        case "--console-work-directory" :: tail ⇒ parse(dropArg(tail), c.copy(consoleWorkDirectory = Some(new File(takeArg(tail)))))
        case "--logger-level" :: tail           ⇒ parse(tail.tail, c.copy(loggerLevel = Some(tail.head)))
        case "--remote" :: tail                 ⇒ parse(tail, c.copy(remote = true))
        case "--http" :: tail                   ⇒ parse(tail, c.copy(http = true))
        case "--no-browser" :: tail             ⇒ parse(tail, c.copy(browse = false))
        case "--reset" :: tail                  ⇒ parse(tail, c.copy(launchMode = Reset(initialisePassword = false)))
        case "--host-name" :: tail              ⇒ parse(tail.tail, c.copy(hostName = Some(tail.head)))
        case "--reset-password" :: tail         ⇒ parse(tail, c.copy(launchMode = Reset(initialisePassword = true)))
        case "--" :: tail                       ⇒ parse(Nil, c.copy(args = tail))
        case "-h" :: tail                       ⇒ help(tail)
        case "--help" :: tail                   ⇒ help(tail)
        case s :: tail                          ⇒ parse(tail, c.copy(ignored = s :: c.ignored))
        case Nil                                ⇒ c
      }
    }

    PluginManager.startAll.foreach { case (b, e) ⇒ logger.log(WARNING, s"Error staring bundle $b", e) }

    val config = parse(args.map(_.trim).toList)

    config.loggerLevel.foreach(LoggerService.level)

    def loadPlugins = {
      val (existingUserPlugins, notExistingUserPlugins) = config.userPlugins.span(new File(_).exists)

      if (!notExistingUserPlugins.isEmpty) logger.warning(s"""Some plugins or plugin folders don't exist: ${notExistingUserPlugins.mkString(",")}""")

      val userPlugins =
        existingUserPlugins.flatMap { p ⇒ PluginManager.listBundles(new File(p)) } ++ module.allModules

      logger.fine(s"Loading user plugins " + userPlugins)

      PluginManager.tryLoad(userPlugins)
    }

    def displayErrors(load: ⇒ Iterable[(File, Throwable)]) =
      load.foreach { case (f, e) ⇒ logger.log(WARNING, s"Error loading bundle $f", e) }

    def password =
      config.password orElse config.passwordFile.map(_.lines.head)

    def setPassword = {
      try password foreach Workspace.setPassword
      catch {
        case e: UserBadDataError ⇒
          logger.severe("Wrong password!")
          throw e
      }
    }

    if (!config.ignored.isEmpty) logger.warning("Ignored options: " + config.ignored.mkString(" "))

    config.launchMode match {
      case HelpMode ⇒
        println(usage)
        Console.ExitCodes.ok
      case Reset(initialisePassword) ⇒
        Workspace.reset()
        if (initialisePassword) Console.initPassword
        Console.ExitCodes.ok
      case RESTMode ⇒
        setPassword
        displayErrors(loadPlugins)
        if (!password.isDefined) Console.initPassword
        val server = new RESTServer(config.port, config.hostName)
        server.start()
        Console.ExitCodes.ok
      case ConsoleMode ⇒
        setPassword
        print(consoleSplash)
        println(consoleUsage)
        Console.dealWithLoadError(loadPlugins, !config.scriptFile.isDefined)
        val console = new Console(password, config.scriptFile)
        val variables = ConsoleVariables(args = config.args)
        console.run(variables, config.consoleWorkDirectory)
      case GUIMode ⇒
        setPassword
        // FIXME switch to a GUI display in the plugin panel
        displayErrors(loadPlugins)
        def browse(url: String) =
          if (Desktop.isDesktopSupported) Desktop.getDesktop.browse(new URI(url))
        GUIServer.lockFile.withFileOutputStream { fos ⇒
          val launch = (config.remote || fos.getChannel.tryLock != null)
          if (launch) {
            val port = config.port.getOrElse(Workspace.preference(GUIServer.port))
            def useHTTP = config.http || !config.remote
            def protocol = if (useHTTP) "http" else "https"
            val url = s"$protocol://localhost:$port"
            GUIServer.urlFile.content = url
            val webui = Workspace.file("webui")
            webui.mkdirs()
            val server = new GUIServer(port, config.remote, useHTTP)
            server.start()
            if (config.browse && !config.remote) browse(url)
            ScalaREPL.warmup
            logger.info(s"Server listening on port $port.")
            server.join() match {
              case GUIServer.Ok      ⇒ Console.ExitCodes.ok
              case GUIServer.Restart ⇒ Console.ExitCodes.restart
            }
          }
          else {
            browse(GUIServer.urlFile.content)
            Console.ExitCodes.ok
          }
        }
    }

  }

}
