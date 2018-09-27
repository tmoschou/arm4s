import sbt.{Def, _}
import Keys._

object Build {

  lazy val scalaTest: Def.Initialize[Seq[ModuleID]] = Def.setting {
    CrossVersion.partialVersion(scalaVersion.value) match {
      case Some((2, v)) if v <= 12 =>
        Seq("org.scalatest" %% "scalatest" % "3.0.5" % "test")
      case _ =>
        Nil
    }
  }

}
