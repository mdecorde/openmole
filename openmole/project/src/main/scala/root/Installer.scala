package root

import sbt._
import Keys._
import org.clapper.sbt.izpack.IzPack
import org.clapper.sbt.izpack.IzPackSection
import IzPack.IzPack._
import org.openmole.buildsystem.OMKeys._
import Bin.{ openmole, openmoleRuntime }

object Installer extends Defaults {
  val dir = file("installer")

  lazy val installer = AssemblyProject("installer", "installer", baseDir = file("."), settings = IzPack.izPackSettings ++ resAssemblyProject) settings (
    assemble := false,
    packageBin := file("."),
    createInstaller in IzPack.IzPack.Config <<= (createInstaller in IzPack.IzPack.Config) dependsOn resourceAssemble,
    variables in Config <++= version { v ⇒ Seq(("version", v), "home" -> "$USER_HOME") },
    installSourceDir in Config <<= assemblyPath,
    configFile in Config <<= assemblyPath { _ / "resources/install.yml" },
    resourceSets <<= (assemblyPath in openmole, target in openmoleRuntime, tarGZName in openmoleRuntime, baseDirectory) map { (assembly, target, tarGz, bD) ⇒
      Set(
        assembly -> "openmole",
        bD / "resources" -> "resources"
      ) ++ (Set(tarGz getOrElse "assembly", "jvm-386", "jvm-x64") map (n ⇒ target / (n + ".tar.gz") -> "runtime"))
    }
  )
}