val arm = (project in file(".")).
  settings(
    organization := "io.tmos",
    name := "arm4s",
    scalaVersion := "2.13.1",
    // remember to update travis CI
    crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.10", "2.13.1" ),
    scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),
    Compile / doc / scalacOptions ++= Seq("-groups", "-implicits"),
    autoAPIMappings := true,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.1.0" % Test,
    licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("https://github.com/tmoschou/arm4s")),
    publishMavenStyle := true,
    Test / publishArtifact := false,
    publishTo := {
      val nexus = "https://oss.sonatype.org/"
      if (isSnapshot.value)
        Some("snapshots" at nexus + "content/repositories/snapshots")
      else
        Some("releases"  at nexus + "service/local/staging/deploy/maven2")
    },
    pomIncludeRepository := { _ => false },
    scmInfo := Some(
      ScmInfo(
        homepage.value.get,
        "scm:git:git@github.com:tmoschou/arm4s.git"
      )
    ),
    developers := List(
      Developer(
        "tmoschou",
        "Terry Moschou",
        "tmoschou@gmail.com",
        url("https://github.com/tmoschou/")
      )
    )
  )
