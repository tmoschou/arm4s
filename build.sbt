import Build._

val arm = (project in file(".")).
  settings(
    organization := "io.tmos",
    name := "arm4s",
    scalaVersion := "2.12.6",
    crossScalaVersions := Seq("2.10.7", "2.11.12", "2.12.6" /*, "2.13.0-M5" */),
    scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),
    Compile / doc / scalacOptions ++= Seq("-groups", "-implicits"),
    autoAPIMappings := true,
    libraryDependencies ++= scalaTest.value,
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
    pomExtra :=
      <scm>
        <url>git@github.com:tmoschou/arm4s.git</url>
        <connection>scm:git:git@github.com:tmoschou/arm4s.git</connection>
      </scm>
      <developers>
        <developer>
          <id>tmoschou</id>
          <name>Terry Moschou</name>
          <url>http://tmos.io/</url>
        </developer>
      </developers>
    //useGpg := true
  )
