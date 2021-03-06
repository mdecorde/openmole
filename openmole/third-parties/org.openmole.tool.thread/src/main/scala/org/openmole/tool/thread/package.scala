/*
 * Copyright (C) 2011 Romain Reuillon
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

package org.openmole.tool

import java.util.concurrent._

import org.openmole.tool.logger.Logger

import scala.concurrent.duration.Duration
import scala.util.{ Failure, Success, Try }

package object thread {
  object L extends Logger
  import L._

  val daemonThreadFactory = new ThreadFactory {

    override def newThread(r: Runnable): Thread = {
      val t = new Thread(r)
      t.setDaemon(true)
      t
    }

  }

  val defaultExecutor = Executors.newCachedThreadPool(daemonThreadFactory)

  implicit def future2Function[A](f: Future[A]) = () ⇒ f.get
  implicit def function2Callable[F](f: ⇒ F) = new Callable[F] { def call = f }

  def fixedThreadPool(n: Int) = Executors.newFixedThreadPool(n, daemonThreadFactory)

  def background[F](f: ⇒ F)(implicit executor: ExecutorService = defaultExecutor): Future[F] = executor.submit(f)

  def timeout[F](f: ⇒ F)(duration: Duration)(implicit executor: ExecutorService = defaultExecutor): F = try {
    val r = executor.submit(f)
    try r.get(duration.toMillis, TimeUnit.MILLISECONDS)
    catch {
      case e: TimeoutException ⇒ r.cancel(true); throw e
    }
  }
  catch {
    case e: RejectedExecutionException ⇒
      Log.logger.log(Log.WARNING, "Execution rejected, operation executed in the caller thread with no timeout", e)
      f
  }

  def withThreadClassLoader[T](classLoader: ClassLoader)(f: ⇒ T): T = {
    val threadClassLoader = Thread.currentThread().getContextClassLoader
    Thread.currentThread().setContextClassLoader(classLoader)
    try f
    finally Thread.currentThread().setContextClassLoader(threadClassLoader)
  }

}
