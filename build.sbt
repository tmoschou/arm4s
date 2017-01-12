val arm = (project in file(".")).
  settings(
    organization := "io.tmos",
    name := "arm4s",
    scalaVersion := "2.12.0",
    crossScalaVersions := Seq("2.10.6", "2.11.8", "2.12.1"),
    scalacOptions ++= Seq("-deprecation", "-feature", "-Xfatal-warnings"),
    scalacOptions in (Compile, doc) ++= Seq("-groups", "-implicits"),
    autoAPIMappings := true,
    libraryDependencies += "org.scalatest" %% "scalatest" % "3.0.1" % "test",
    licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause")),
    homepage := Some(url("https://github.com/tmoschou/arm4s")),
    publishMavenStyle := true,
    publishArtifact in Test := false,
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
      </developers>,
    pgpSigningKey := Some(-1091523304587826185L)
  )
