// JK: this is needed to work around the fact that ammonite 
// for scala 2.3.17 has not yet been released.
// (https://github.com/com-lihaoyi/Ammonite/releases)
// Ammonite 2.5.0
import $ivy.`com.typesafe.akka::akka-actor-typed:2.6.18`
import $ivy.`com.lihaoyi:os-lib_2.11:0.8.0`

import akka.actor.typed.{Behavior, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.TimerScheduler

object DiskActor {
  def apply(logPath: os.Path, rotateSize: Int = 50): Behavior[String] = {
    var logSize = 0
    Behaviors.receiveMessage { s =>
      val newLogSize = logSize + s.length + 1
      if (newLogSize <= rotateSize) logSize = newLogSize
      else {
        logSize = s.length + 1
        os.move(logPath, oldPath(logPath), replaceExisting = true)
      }
      os.write.append(logPath, s + "\n", createFolders = true)
      Behaviors.same
    }
  }

  def oldPath(logPath: os.Path) = logPath / os.up / (logPath.last + "-old")
}

val actorSystem = ActorSystem[Nothing](
  Behaviors.setup[Nothing] { context =>
    // The book uses os.pwd / "log.txt",
    // which can be insightful because you can inspect the log afterwards.
    // But I didn't want to clutter the repo.
    val logPath = os.temp(suffix = "log.txt", deleteOnExit = true)
    var oldPath = DiskActor.oldPath(logPath)

    val logger = context.spawn(DiskActor(logPath), "log.txt")

    val messages = Array(
      "I am cow",
      "hear me moo",
      "I weight twice as much as you",
      "And I look good on the barbecue",
      "Yoghurt curds cream chees and butter",
      "Comes from the liquids from my udder",
      "I am cow, I am cow",
      "Hear me moo, moooo"
    )

    messages.foreach(m => logger ! m)

    // JK: I ran the script inside a loop,
    // and it seems that all messages have processed here.
    // But we are not allowed to assume this
    // (since every actor has their own thread of execution).
    assert(
      os.read.lines(oldPath) == Seq(
        "Comes from liquids from my udder"
      )
    )
    assert(
      os.read.lines(logPath) == Seq(
        "I am cow, I am cow",
        "Hear me moo, moooo"
      )
    )

    Behaviors.empty
  },
  "LoggingPipeline"
)
