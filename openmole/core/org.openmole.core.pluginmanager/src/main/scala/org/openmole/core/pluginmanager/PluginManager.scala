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

package org.openmole.core.pluginmanager

import java.io.File
import java.util.concurrent.Semaphore

import org.openmole.core.exception.{ InternalProcessingError, UserBadDataError }
import org.openmole.core.pluginmanager.internal.Activator
import org.openmole.tool.file._
import org.openmole.tool.logger.Logger
import org.openmole.tool.hash._
import org.osgi.framework._
import org.osgi.framework.wiring.{ BundleWiring, FrameworkWiring }

import scala.collection.immutable.{ HashMap, HashSet }
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConversions._
import scala.concurrent.stm._
import scala.util.{ Failure, Success, Try }

case class BundlesInfo(
    files:                Map[File, (Long, Long)],
    providedDependencies: Set[Long]
) {
  lazy val hashes = files.keys.map(f ⇒ f → f.hash).toMap
}

object PluginManager extends Logger {

  import Log._

  private val bundlesInfo = Ref(None: Option[BundlesInfo])
  private val resolvedPluginDependenciesCache = TMap[Long, Iterable[Long]]()

  Activator.contextOrException.addBundleListener(
    new BundleListener {
      override def bundleChanged(event: BundleEvent) = {
        if (event.getType == BundleEvent.RESOLVED ||
          event.getType == BundleEvent.UNRESOLVED ||
          event.getType == BundleEvent.UPDATED) clearCaches()
      }
    }
  )

  private def clearCaches() = atomic { implicit ctx ⇒
    bundlesInfo() = None
    resolvedPluginDependenciesCache.clear()
  }

  def bundles = Activator.contextOrException.getBundles.filter(!_.isSystem).toSeq
  def bundleFiles = infos.files.keys

  def dependencies(file: File): Option[Iterable[File]] =
    infos.files.get(file).map { case (id, _) ⇒ allPluginDependencies(Activator.contextOrException.getBundle(id)).map { _.file } }

  def isClassProvidedByAPlugin(c: Class[_]) = {
    val b = FrameworkUtil.getBundle(c)
    if (b != null) !infos.providedDependencies.contains(b.getBundleId())
    else false
  }

  def fileProviding(c: Class[_]) =
    bundleForClass(c).map(b ⇒ Activator.contextOrException.getBundle(b.getBundleId).file.getCanonicalFile)

  def bundleForClass(c: Class[_]): Option[Bundle] =
    Option(FrameworkUtil.getBundle(c))

  def bundlesForClass(c: Class[_]): Iterable[Bundle] = {
    val bundle = bundleForClass(c).toSeq
    bundle.flatMap(allDependencies)
  }

  def pluginsForClass(c: Class[_]): Iterable[File] = {
    val bundle = bundleForClass(c)
    bundle.toSeq.flatMap(allPluginDependencies).map(_.file)
  }

  def allDepending(file: File, filter: Bundle ⇒ Boolean): Iterable[File] =
    bundle(file) match {
      case Some(b) ⇒ allDependingBundles(b, filter).map { _.file }
      case None    ⇒ Iterable.empty
    }

  def isBundle(file: File) = {
    def isDirectoryPlugin(file: File) = file.isDirectory && file./("META-INF")./("MANIFEST.MF").exists
    isDirectoryPlugin(file) || (!file.isDirectory && file.isJar)
  }

  def listBundles(path: File): Iterable[File] =
    if (isBundle(path)) List(path)
    else if (path.isDirectory) path.listFilesSafe.filter(isBundle)
    else Nil

  def tryLoad(files: Iterable[File]): Iterable[(File, Throwable)] = synchronized {
    val bundleFiles = files.flatMap { listBundles }
    val loaded = bundleFiles.map { b ⇒ b → Try(installBundle(b)) }
    def bundles = loaded.collect { case (f, Success(b)) ⇒ f → b }
    def loadError = loaded.collect { case (f, Failure(e)) ⇒ f → e }
    loadError ++ bundles.flatMap {
      case (f, b) ⇒
        Try(b.start) match {
          case Success(_) ⇒ None
          case Failure(e) ⇒
            b.uninstall()
            Some(f → e)
        }
    }
  }

  def load(files: Iterable[File]) = synchronized {
    val bundles = files.flatMap { listBundles }.map { installBundle }.toList
    bundles.foreach {
      b ⇒
        logger.fine(s"Stating bundle ${b.getLocation}")
        b.start
    }
    bundles
  }

  def loadIfNotAlreadyLoaded(plugins: Iterable[File]) = synchronized {
    val bundles = plugins.filterNot(f ⇒ infos.files.contains(f)).map(installBundle).toList
    bundles.foreach { _.start }
  }

  def load(path: File): Unit = load(List(path))

  def loadDir(path: File): Unit =
    if (path.exists && path.isDirectory) load(listBundles(path))

  def bundle(file: File) = infos.files.get(file.getCanonicalFile).map { id ⇒ Activator.contextOrException.getBundle(id._1) }

  private def allDependencies(b: Bundle) = dependencies(List(b))

  def isPlugin(b: Bundle): Boolean = isPlugin(b.getBundleId)
  def isPlugin(id: Long): Boolean = !infos.providedDependencies.contains(id)

  def allPluginDependencies(b: Bundle) = atomic { implicit ctx ⇒
    resolvedPluginDependenciesCache.
      getOrElseUpdate(b.getBundleId, dependencies(List(b)).map(_.getBundleId)).
      filter(isPlugin).map(l ⇒ Activator.contextOrException.getBundle(l))
  }

  def remove(b: Bundle) = synchronized {
    val additionalBundles = Seq(b) ++ allDependingBundles(b, b ⇒ !b.isProvided)
    additionalBundles.foreach(b ⇒ if (b.getState == Bundle.ACTIVE) b.uninstall())
    updateBundles()
  }

  private def getBundle(f: File) =
    Option(Activator.contextOrException.getBundle(f.toURI.toString))

  private def installBundle(f: File) =
    try {
      val bundle = Activator.contextOrException.installBundle(f.toURI.toString)
      bundlesInfo.single() = None
      bundle
    }
    catch {
      case t: Throwable ⇒ throw new InternalProcessingError(t, "Installing bundle " + f)
    }

  private def dependencies(bundles: Iterable[Bundle]): Iterable[Bundle] = {
    val seen = mutable.Set[Bundle]() ++ bundles
    var toProceed = new ListBuffer[Bundle] ++ bundles

    while (!toProceed.isEmpty) {
      val current = toProceed.remove(0)
      for {
        b ← directDependencies(current)
      } if (!seen(b)) {
        seen += b
        toProceed += b
      }
    }
    seen.toList
  }

  private def infos: BundlesInfo = atomic { implicit ctx ⇒
    bundlesInfo() match {
      case None ⇒
        val bs = bundles
        val providedDependencies = dependencies(bs.filter(b ⇒ b.isProvided)).map(_.getBundleId).toSet
        val files = bs.map(b ⇒ b.file.getCanonicalFile → ((b.getBundleId, b.file.lastModification))).toMap
        val info = BundlesInfo(files, providedDependencies)
        bundlesInfo() = Some(info)
        info
      case Some(bundlesInfo) ⇒ bundlesInfo
    }
  }

  def allDependingBundles(b: Bundle, filter: Bundle ⇒ Boolean): Iterable[Bundle] = {
    val seen = mutable.Set[Bundle]()
    val toProcess = ListBuffer[Bundle]()

    toProcess += b
    seen += b

    while (!toProcess.isEmpty) {
      val current = toProcess.remove(0)
      for {
        b ← directDependingBundles(current).filter(filter)
      } if (!seen(b)) {
        seen += b
        toProcess += b
      }
    }

    seen.toList
  }

  def directDependencies(b: Bundle) =
    if (b.isFullDynamic) List.empty
    else b.adapt(classOf[BundleWiring]).getRequiredWires(null).map(_.getProvider.getBundle).filter(_.getBundleId != Constants.SYSTEM_BUNDLE_ID).distinct

  def directDependingBundles(b: Bundle) =
    b.adapt(classOf[BundleWiring]).
      getProvidedWires(null).
      map(_.getRequirer.getBundle).
      filter(b ⇒ b.getBundleId != Constants.SYSTEM_BUNDLE_ID && !b.isFullDynamic).
      distinct

  def startAll: Seq[(Bundle, Throwable)] =
    Activator.contextOrException.getBundles.filter {
      _.getState match {
        case Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING ⇒ true
        case _ ⇒ false
      }
    }.map {
      b ⇒ b → Try(b.start)
    }.collect { case (b: Bundle, Failure(e)) ⇒ b → e }

  def updateBundles(bundles: Option[Seq[Bundle]] = None) {
    val listener = new FrameworkListener {
      val lock = new Semaphore(0)

      override def frameworkEvent(event: FrameworkEvent): Unit =
        if (event.getType == FrameworkEvent.PACKAGES_REFRESHED) lock.release()
    }

    val wiring = Activator.contextOrException.getBundle(0).adapt(classOf[FrameworkWiring])

    bundles match {
      case Some(s) ⇒ wiring.refreshBundles(s, listener)
      case None    ⇒ wiring.refreshBundles(null, listener)
    }

    listener.lock.acquire()
  }

  def bundleHashes = infos.hashes.values

  /* For debugging purposes */
  def printBundles = println(Activator.contextOrException.getBundles.mkString("\n"))
  def printDirectDependencies(b: Long) = println(directDependingBundles(Activator.contextOrException.getBundle(b)).mkString("\n"))
  def printIsPlugin(b: Long) = println(isPlugin(b))

}
