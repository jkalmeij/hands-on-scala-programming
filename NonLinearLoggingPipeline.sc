// JK: this is needed to work around the fact that ammonite
// for scala 2.3.17 has not yet been released.
// (https://github.com/com-lihaoyi/Ammonite/releases)
// Scala 2.13.6
import $ivy.`com.typesafe.akka::akka-actor-typed:2.6.18`
import $ivy.`com.typesafe.akka::akka-actor-testkit-typed:2.6.18`
import $ivy.`com.lihaoyi::os-lib:0.8.0`
import $ivy.`com.lihaoyi::requests:0.7.0`
import $ivy.`com.lihaoyi::upickle:1.4.3`

import akka.actor.typed.{Behavior, ActorRef}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.TimerScheduler
import akka.actor.testkit.typed.scaladsl.{ActorTestKit, TestProbe}
import scala.concurrent.duration._

implicit class BetterFunction[A](val f: Function1[A, Unit]) {
  def withOriginalInput = { v: A => f(v); v }
}

// JK: Very surprising akka does not define these.
// I am probably using actors while I should be using streams.
// Or I simply could not find the definitions.
implicit class BetterBehavior[A](val dest: Behavior[A]) {
  def contramap[B](f: B => A): Behavior[B] =
    Behaviors.setup { context =>
      val next = context.spawnAnonymous(dest)
      Behaviors.receiveMessage { s =>
        val ss = f(s)
        next ! ss
        Behaviors.same
      }
    }
}

implicit class BetterBehaviors[A](val dests: Iterable[Behavior[A]]) {
  def combineAll[T]: Behavior[A] = Behaviors.setup { context =>
    val nexts = dests.map(d => context.spawnAnonymous(d))
    Behaviors.receiveMessage { s =>
      nexts.map(next => next ! s)
      Behaviors.same
    }
  }
}

implicit class BetterTestProbe[A](val testProbe: TestProbe[A]) {
  def asBehavior: Behavior[A] = Behaviors.receiveMessage { s =>
    testProbe.ref ! s
    Behaviors.same
  }
}

// https://github.com/com-lihaoyi/Ammonite/issues/534
//
// Issue 534 is from the year 2017 and is apparently fixed (https://github.com/scala/bug/issues/9076),
// but when I run it with ammonite 2.2.0 (Scala 2.13.3 Java 11.0.7)
// I still run into it.
//
// From the issue, a workaround is to wrap your entire script inside a Main object.
object Main {
  private def sanetize(s: String) =
    s.replaceAll("([0-9]{4})[0-9]{8}([0-9]{4})", "<redacted>")

  private def toDisk(logPath: os.Path, rotateSize: Int = 50): (String => Unit) = {
    var logSize = 0
    s => {
      val newLogSize = logSize + s.length + 1
      if (newLogSize <= rotateSize) logSize = newLogSize
      else {
        logSize = s.length + 1
        os.move(logPath, makeOldPath(logPath), replaceExisting = true)
      }
      os.write.append(logPath, s + "\n", createFolders = true)
    }
  }

  private def makeOldPath(logPath: os.Path) = logPath / os.up / (logPath.last + "-old")

  private def encode(msg: String): String =
    java.util.Base64.getEncoder.encodeToString(msg.getBytes)

  private def decode(s: String): String = new String(
    java.util.Base64.getDecoder.decode(s)
  )

  private def upload(url: String)(msg: String) = {
    val res = requests.post(url, data = msg)
    println(s"response ${res.statusCode} " + ujson.read(res)("data"))
  }

  def run() {

    // The book uses os.pwd / "log.txt",
    // which can be insightful because you can inspect the log afterwards.
    // But I didn't want to clutter the repo.
    val logPath = os.temp(suffix = "log.txt", deleteOnExit = true)
    // val logPath = os.pwd / "log.txt"
    var oldPath = makeOldPath(logPath)

    var testKit = ActorTestKit()
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

    var uploadProbe = testKit.createTestProbe[String]()
    var diskProbe = testKit.createTestProbe[String]()

    // contramap is like map, except its in reverse.
    // so if you think of a pipeline, you are essentially starting at the end.
    val loggerBehavior =
      Seq(
        uploadProbe.asBehavior.contramap(
          (upload("https://httpbin.org/post")(_)).withOriginalInput
        ),
        diskProbe.asBehavior.contramap(
          toDisk(logPath).withOriginalInput
        )
      ).combineAll
        .contramap(encode(_))
        .contramap(sanetize(_))

    val logger = testKit.spawn(loggerBehavior)

    messages.foreach(m => logger ! m)

    val _ = diskProbe.receiveMessages(messages.length, 3.seconds)
    val _ = uploadProbe.receiveMessages(messages.length, 10.seconds)

    testKit.shutdownTestKit

    println("Done")

    assert(
      os.read.lines(oldPath).map(decode) == Seq(
        "Comes from the liquids from my udder"
      )
    )
    assert(
      os.read.lines(logPath).map(decode) == Seq(
        "I am cow, I am cow",
        "Hear me moo, moooo"
      )
    )
  }
}

Main.run
