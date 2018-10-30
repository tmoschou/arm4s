Update ~/.sbt/1.0/sonatype.sbt with credentials
```
credentials += Credentials(
  "Sonatype Nexus Repository Manager",
  "oss.sonatype.org",
  "__USERNAME__",
  "__PASSWORD__"
)
```

Then publish all versions of signed artifacts to staging repo
```
$ sbt
sbt:arm4s> +publish
```

Then promote to release
```
sbt:arm4s> sonatypeRelease
```
