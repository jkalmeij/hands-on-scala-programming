# Ch16 Message-based Parallelism with Actors in Akka

In [Chapter 16](https://github.com/handsonscala/handsonscala/issues/16) of [Hands-on Scala Programming](https://www.handsonscala.com/) Li Haoyi uses [com-lihaoyi/castor](https://github.com/com-lihaoyi/castor) to explain how actor systems work by building processing pipelines using actors.

To me, the pipelines he constructed looked very similar to pipelines i constructed in C# with [Dataflow (Task Parallel Library) | Microsoft Docs](https://docs.microsoft.com/en-us/dotnet/standard/parallel-programming/dataflow-task-parallel-library). And not so similar to what I had seen from [Microsoft Orleans](https://dotnet.github.io/orleans/).

This then raises the question, what would his examples look like in a larger and more complex actor framework such as [Akka](https://akka.io/)?

This repository attemps to reimplement the examples from Chapter 16:

- Simple Upload Actor (see `Simple.sc`)
- Batch Upload Actor (see `Batch.sc`)
- State Machine Upload Actor (see `StateMachine.sc`)
- Multi-stage Logging Pipeline (see `MultiStageLoggingPipeline.sc`)
- Non-Linear Pipeline (see `NonLinearLoggingPipeline.sc`)
- Debugging Actors (see `DebuggingActors.sc`)

## What I learned about akka

I am not an expert. This repo is the result of me playing with Akka for a day. The chances of me being misinformed are quite high.

### Documentation?

Akka's API has evolved throughout the years.

I couldn't find the exact details on it, but if you're just going to google documentation you're going to find some outdated docs.

Typically you will want to work with `akka.actor.typed` ("the new actor API") instead of `akka.actor` ("the classic actor API") (as per the official documentation: [Classic Actors • Akka Documentation](https://doc.akka.io/docs/akka/current/actors.html)).

I guess if you stick with [](https://doc.akka.io/) you're hopefully fine.

### FP or OOP

Akka offers two flavors of the Actor API: Functional and OOP. The [Style guide • Akka Documentation](https://doc.akka.io/docs/akka/current/typed/style-guide.html#functional-versus-object-oriented-style) goes into more detail.

### `this` vs `Behaviors.same`

They are the same thing. Recommendation seems to be:

- `this` for OOP-style.
- `Behaviors.same` for FP-style.

https://stackoverflow.com/a/60296263

### Anything else?

Here is some documentation I found insightful:

[Interaction Patterns • Akka Documentation](https://doc.akka.io/docs/akka/current/typed/interaction-patterns.html#scheduling-messages-to-self)