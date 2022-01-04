# Ch16 Message-based Parallelism with Actors in Akka

In [Chapter 16](https://github.com/handsonscala/handsonscala/issues/16) of [Hands-on Scala Programming](https://www.handsonscala.com/) Li Haoyi uses [com-lihaoyi/castor](https://github.com/com-lihaoyi/castor) to explain how actor systems work by building processing pipelines using actors.

To me, the pipelines he constructed looked very similar to pipelines i constructed in C# with [Dataflow (Task Parallel Library) | Microsoft Docs](https://docs.microsoft.com/en-us/dotnet/standard/parallel-programming/dataflow-task-parallel-library). And not so similar to what I had seen from [Microsoft Orleans](https://dotnet.github.io/orleans/).

This then raises the question, what would his examples look like in a larger and more complex actor framework such as [Akka](https://akka.io/)?

This repository attemps to reimplement the examples from Chapter 16:

- Simple Upload Actor (see `Simple.sc`)
- Batch Upload Actor (see `Batch.sc`)
- State Machine Upload Actor (see `StateMachine.sc`)
- Logging Pipeline (see `LoggingPipeline.sc`)
- Multi-stage Logging Pipeline (see `MultiStageLoggingPipeline.sc`)
- Non-Linear Pipeline (see `NonLinearLoggingPipeline.sc`)
- Debugging Actors (see `DebuggingActors.sc`)