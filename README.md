## Reproducing weird behaviour with Cats-effect 2.2.0 / 2.3.0

It appears that when a `TestContext#tick` call happens after a Blocker operation,
tasks on the TestContext doesn't seem to execute.

This seems to be an issue on both cats-effect `2.2.0` and `2.3.0`

Run
```
sbt test
```

Change cats-effect version to `2.1.4` and notice the same test now passes

