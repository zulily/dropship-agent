dropship-agent
==============

Library for aiding in the development of "agents" or "plugins" for dropship.

What For?
---------

Implement a dropship agent if you wish to have code that runs before and after your target application. For example, you may wish to [start up some threads that send JMX metrics to statsd](https://github.com/zulily/dropship-statsd-agent).

How?
----

`BaseAgent` provides methods to configure basic interaction with the Dropship lifecycle. Instances of subclasses of BaseAgent are wired in to the `preRun`, `onError` and `onExit` methods of Dropship.

To build a Dropship agent,

1. Extend BaseAgent
    * Implement `onStart(Properties, String, Class, Method, Object[])` - which will be invoked just before Dropship hands control over to the target
    * Implement `onError(Throwable)` - which will be invoked in the event of an exception from the target (or an unhandled exception in any threads).
    * Implement `onExit()`, which will be invoked as Dropship is shutting down
1. Create a `public static void premain(String agentArgument, Instrumentation instrumentation)` method
    * In it, instantiate your agent
    * Pass that instance to premain(String, Instrumentation, BaseAgent)
1. Package your agent as a jar file. You will probably want to create a "shaded" jar
1. Add a `Premain-Class` entry to the `MANIFEST.MF` in your jar file
1. Use your agent from the command line: `java -javaagent:youragent.jar -jar dropship.jar ...`

## License

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
