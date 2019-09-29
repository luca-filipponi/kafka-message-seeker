name := "kafka-message-seeker"
version := "0.1"
organization := "com.filiponi"
scalaVersion := "2.12.8"

assemblyJarName in assembly := s"${name.value}-${version.value}.jar"

libraryDependencies ++= Seq(
  "org.apache.kafka" % "kafka-clients" % "2.3.0",
  "com.github.scopt" %% "scopt" % "4.0.0-RC2",
  "ch.qos.logback" % "logback-classic" % "1.2.3",
  "org.scalatest" %% "scalatest" % "3.0.8" % Test,
  "com.whisk" %% "docker-testkit-scalatest" % "0.9.8" % Test,
  "com.whisk" %% "docker-testkit-impl-spotify" % "0.9.8" % Test,
  "org.mockito" % "mockito-all" % "1.10.19" % Test
)

