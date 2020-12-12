## Reproducing weird behaviour with Cats-effect 2.2.0 / 2.3.0

It appears that when a `TestContext#tick` call happens after a Blocker operation,
tasks on the TestContext doesn't seem to execute.

This seems to be an issue on both cats-effect `2.2.0` and `2.3.0`

Run
```
sbt run
```

Change cats-effect version to `2.1.4` and notice the same test now passes

Output for `2.1.4`:
```
task meant for ec2 running on ec2-0
Running TestContext.tick in ec1-0
State of test context before tick: State(1,0 days,TreeSet(Task(1,cats.effect.internals.IOShift$Tick@c55f5fb,0 days)),None)
Calling atomicInt.addAndGet on thread ec1-0 at 47602837
Tick done at 48885834
```

Output for `2.2.0+`
```
task for ec2 running on ec2-0
Running TestContext.tick in ec1-0
State of test context before tick: State(1,0 days,TreeSet(Task(1,cats.effect.internals.IOShift$Tick@72472438,0 days)),None)
Tick done at 37895936
Calling atomicInt.addAndGet on thread ec1-0 at 63633246
```

Note that somehow the task in EC runs after `tick()` is suppose to be done
