import $ivy.`com.typesafe.akka::akka-actor-typed:2.6.18`
import $ivy.`com.lihaoyi::requests:0.7.0`
import $ivy.`com.lihaoyi::upickle:1.4.3`

import akka.actor.typed.{Behavior, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.TimerScheduler
import scala.concurrent.duration._

object StateMachineUpload {
  sealed trait Msg
  final case class Text(s: String) extends Msg

  def apply(n: Int): Behavior[Msg] =
    Behaviors.withTimers(timers => new StateMachineUpload(n, timers).idle)

  private final case class Flush() extends Msg
}

class StateMachineUpload(
    n: Int,
    timers: TimerScheduler[StateMachineUpload.Msg]
) {
  import StateMachineUpload._

  var responseCount = 0

  private def idle(): Behavior[Msg] = Behaviors.receiveMessage { case Text(s) =>
    upload(s)
  }

  private def buffering(msgs: Vector[String]): Behavior[Msg] =
    Behaviors.receiveMessage {
      case Text(s) => buffering(msgs :+ s)
      case Flush() => if (msgs.isEmpty) idle() else upload(msgs.mkString)
    }

  private def upload(data: String): Behavior[Msg] = {
    println(s"Uploading $data")
    val res = requests.post("https://httpbin.org/post", data = data)
    responseCount += 1 // JK: was unused in book?
    println(s"response ${res.statusCode} ${ujson.read(res)("data")}")
    timers.startSingleTimer(Flush(), n.seconds)
    buffering(Vector.empty)
  }
}

var guardianBehavior = Behaviors.setup[Nothing] { context =>
  var uploader = context.spawn(StateMachineUpload(5), "uploader")

  import StateMachineUpload.Text

  uploader ! Text("hello")
  uploader ! Text("world")
  uploader ! Text("!")

  Behaviors.empty
}

val actorSystem = ActorSystem[Nothing](guardianBehavior, "Simple")
