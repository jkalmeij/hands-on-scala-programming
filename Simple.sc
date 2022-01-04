import $ivy.`com.typesafe.akka::akka-actor-typed:2.6.18`
import $ivy.`com.lihaoyi::requests:0.7.0`

import akka.actor.typed.{Behavior, ActorSystem}
import akka.actor.typed.scaladsl.Behaviors

var guardianBehavior = Behaviors.setup[Nothing] { context =>
  var uploaderBehavior: Behavior[String] = Behaviors.receive {
    case (context, msg) => {
      // This is probably not the 'akka way' since we are now doing a blocking operation.
      // But let's stick true to the original example.
      val res = requests.post("https://httpbin.org/post", data = msg)
      println("response " + res.statusCode)
      Behaviors.same
    }
  }
  var uploader = context.spawn(uploaderBehavior, "uploader")

  println("Sending hello")
  uploader ! "hello"
  println("Sending world")
  uploader ! "world"
  println("Sending !")
  uploader ! "!"

  Behaviors.empty
}

val actorSystem = ActorSystem[Nothing](guardianBehavior, "Simple")

// Akka does not seem to offer a mechanism to ensure no messages are in the mailbox.
// `actorSystem.terminate` processes only a single message (we are sending 3) before terminating the problem.

// I could not find a simple solution to this, but I also could not find a rationale for this.

// This issue is also not addressed in the [quick start](https://developer.lightbend.com/guides/akka-quickstart-scala/index.html)
// where they just keep the system running indefinitely.
