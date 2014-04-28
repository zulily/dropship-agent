dropship-agent
==============

Library for aiding in the development of "agents" or "plugins" for dropship.

What For?
---------

Implement a dropship agent if you wish to have code that runs before and after your target application. For example, you may wish to [https://github.com/zulily/dropship-statsd-agent](start up some threads that send JMX metrics to statsd).

How?
----

`BaseAgent` provides methods to configure basic interaction with the Dropship lifecycle. Instances of subclasses of BaseAgent are wired in to the `preRun`, `onError` and `onExit` methods of Dropship.

To build a Dropship agent,

1. Extend BaseAgent
    * Implement `onStart(Properties, String, Class, Method, Object[])` - which will be invoked just before Dropship hands control over to the target
    * Implement `onError(Throwable)` - which will be invoked in the event of an exception from the target (or an unhandled exception in any threads).
    * Implement `onExit()`, which will be invoked as Dropship is shutting down
1. Create a public static void premain(String agentArgument, Instrumentation instrumentation) method
    * In it, instantiate your agent
    * Pass that instance to premain(String, Instrumentation, BaseAgent)
1. Package your agent as a jar file. You will probably want to create a "shaded" jar
1. Add a Premain-Class entry to the MANIFEST.MF in your jar file
Use your agent from the command line: java -javaagent:youragent.jar -jar dropship.jar ...
