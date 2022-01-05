// JK: this is needed to work around the fact that ammonite
// for scala 2.3.17 has not yet been released.
// (https://github.com/com-lihaoyi/Ammonite/releases)
// Scala 2.13.6
import $ivy.`com.typesafe.akka::akka-actor-typed:2.6.18`
import $ivy.`com.typesafe.akka::akka-actor-testkit-typed:2.6.18`
import $ivy.`com.lihaoyi:os-lib_2.11:0.8.0`

import akka.actor.typed.{Behavior, ActorRef}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestInbox}
import scala.concurrent.duration._

object DiskActor {
  def apply(
      dest: ActorRef[String],
      logPath: os.Path,
      rotateSize: Int = 50
  ): Behavior[String] = {
    var logSize = 0
    Behaviors.receiveMessage { s =>
      val newLogSize = logSize + s.length + 1
      if (newLogSize <= rotateSize) logSize = newLogSize
      else {
        logSize = s.length + 1
        os.move(logPath, oldPath(logPath), replaceExisting = true)
      }
      os.write.append(logPath, s + "\n", createFolders = true)

      dest ! s

      Behaviors.same
    }
  }

  def oldPath(logPath: os.Path) = logPath / os.up / (logPath.last + "-old")
}

object Base64Actor {
  def apply(dest: ActorRef[String]): Behavior[String] =
    Behaviors.receiveMessage { msg =>
      dest ! encode(msg)
      Behaviors.same
    }

  def encode(msg: String): String =
    java.util.Base64.getEncoder.encodeToString(msg.getBytes)

  def decode(s: String): String = new String(
    java.util.Base64.getDecoder.decode(s)
  )
}

// The book uses os.pwd / "log.txt",
// which can be insightful because you can inspect the log afterwards.
// But I didn't want to clutter the repo.
val logPath = os.temp(suffix = "log.txt", deleteOnExit = true)
// val logPath = os.pwd / "log.txt"
var oldPath = DiskActor.oldPath(logPath)

var testKit = ActorTestKit()
var testProbe = testKit.createTestProbe[String]()
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

// testKit.shutdownTestKit gives no guarantee that all messages have been
// received.
// So, if we want our tests to be reliable, we need to insert a test probe
// of which we can monitor the inbox.
// Unfortunately, this requires me to make a code change to DiskActor:
// DiskActor now needs a ActorRef[String] as parameter,
// to which it relays the message when it is done processing.
// That way, we can sense when DiskActor is done processing.
val diskActorBehavior = DiskActor(testProbe.ref, logPath)
val diskActor = testKit.spawn(diskActorBehavior, "DiskLogger")
val logger = testKit.spawn(Base64Actor(diskActor), "Base64DiskLogger")

messages.foreach(m => logger ! m)

val receivedMessages = testProbe.receiveMessages(messages.length)
testKit.shutdownTestKit

assert(
  os.read.lines(oldPath).map(Base64Actor.decode) == Seq(
    "Comes from the liquids from my udder"
  )
)
assert(
  os.read.lines(logPath).map(Base64Actor.decode) == Seq(
    "I am cow, I am cow",
    "Hear me moo, moooo"
  )
)
