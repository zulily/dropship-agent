/*
 * Copyright (C) 2014 zulily, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dropship.agent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BaseAgent provides methods to configure basic interaction with the Dropship lifecycle. Instances
 * of subclasses of BaseAgent are wired in to the {@code preRun}, {@code onError} and {@code onExit}
 * methods of {@code Dropship}.
 * <p>
 * To build a Dropship agent,
 * <ol>
 * <li>Extend {@link dropship.agent.BaseAgent}</li>
 * <li>Implement {@link #onStart(java.util.Properties, String, Class, java.lang.reflect.Method, Object[])} -
 * which will be invoked just before Dropship hands control over to the target</li>
 * <li>Implement {@link #onError(Throwable)} - which will be invoked in the event of an exception from the
 * target (or an unhandled exception in any threads).</li>
 * <li>Implement {@link #onExit()} = which will be invoked as Dropship is shutting down</li>
 * <li>Create a {@code public static void premain(}{@link java.lang.String}{@code agentArgument,}
 * {@link java.lang.instrument.Instrumentation}{@code instrumentation)} method
 * <ul>
 * <li>In it, instantiate your agent</li>
 * <li>Pass that instance to {@link BaseAgent#premain(String, java.lang.instrument.Instrumentation, BaseAgent)}</li>
 * </ul>
 * </li>
 * <li>Package your agent as a jar file. You will probably want to create a "shaded" jar</li>
 * <li>Add a {@code Premain-Class} entry to the MANIFEST.MF in your jar file</li>
 * <li>Use your agent from the command line: {@code java -javaagent:youragent.jar -jar dropship.jar ...}</li>
 * </ol>
 * </p>
 */
public abstract class BaseAgent {

  /**
   * Adds a {@link java.lang.instrument.ClassFileTransformer} that will wire up the given
   * {@link dropship.agent.BaseAgent} to {@code Dropship} {@code preRun}, {@code onError} and
   * {@code onEnd} methods.
   * <p>
   * This method should be called from the {@code public static void premain} method of
   * a jar's Premain-Class as defined in its MANIFEST.MF.
   * </p>
   * <p>
   * This method should only be invoked once.
   * </p>
   *
   * @param agentArgument   agent argument from {@code premain}, null is ok
   * @param instrumentation instrumentation object from {@code premain}, may not be null
   * @param agent           instance of agent implementation, may not be null
   */
  protected static void premain(final String agentArgument, final Instrumentation instrumentation, final BaseAgent agent) {
    if (instrumentation == null) {
      throw new NullPointerException("instrumentation");
    }
    if (agent == null) {
      throw new NullPointerException("agent");
    }
    if (INSTANCE.get() != null) {
      throw new IllegalStateException("agent is already set");
    }
    if (!INSTANCE.compareAndSet(null, agent)) {
      throw new RuntimeException("could not set agent");
    }
    instrumentation.addTransformer(new DropshipTransformer());
  }

  private static final AtomicReference<BaseAgent> INSTANCE = new AtomicReference<BaseAgent>();

  private static BaseAgent get() {
    BaseAgent instance = INSTANCE.get();
    if (instance == null) {
      throw new IllegalStateException("Agent instance has not been set! Call BaseAgent.premain(String agentArg, Instrumentation inst, BaseAgent agent)");
    }
    return instance;
  }

  @SuppressWarnings("UnusedDeclaration")
  public static void start(Properties properties, String gav, Class<?> mainClass, Method mainMethod, Object[] args) {
    get().onStart(properties, gav, mainClass, mainMethod, args);
  }

  protected abstract void onStart(Properties properties,
                                  String groupArtifactString,
                                  Class<?> mainClass,
                                  Method mainMethod,
                                  Object[] arguments);

  @SuppressWarnings("UnusedDeclaration")
  public static void error(Throwable t) {
    get().onError(t);
  }

  protected abstract void onError(Throwable t);

  @SuppressWarnings("UnusedDeclaration")
  public static void exit() {
    get().onExit();
  }

  protected abstract void onExit();

  private static final class DropshipTransformer implements ClassFileTransformer {

    @Override
    public byte[] transform(ClassLoader loader,
                            String className,
                            Class<?> classBeingRedefined,
                            ProtectionDomain protectionDomain,
                            byte[] classBytes) throws IllegalClassFormatException {

      if (className.equals("dropship/Dropship")) {
        return transformDropship(classBytes);
      } else {
        return null;
      }
    }

    private byte[] transformDropship(byte[] dropship) {
      ClassPool pool = ClassPool.getDefault();
      CtClass cls = null;
      try {
        cls = pool.makeClass(new java.io.ByteArrayInputStream(dropship));

        /*
        preRun(Properties properties,
               String groupArtifactString,
               Class<?> mainClass,
               Method mainMethod,
               Object[] arguments)
         */
        CtMethod preRun = cls.getDeclaredMethod("preRun");
        preRun.insertBefore("{dropship.agent.BaseAgent.start($1, $2, $3, $4, $5);}");

        /*
        onError(Throwable e)
         */
        CtMethod onError = cls.getDeclaredMethod("onError");
        onError.insertBefore("{dropship.agent.BaseAgent.error($1);}");

        /*
        onExit()
         */
        CtMethod onExit = cls.getDeclaredMethod("onExit");
        onExit.insertBefore("{dropship.agent.BaseAgent.exit();}");

        dropship = cls.toBytecode();
      } catch (Exception e) {
        System.err.println("Could not instrument, exception : " + e.getMessage());
      } finally {
        if (cls != null) {
          cls.detach();
        }
      }
      return dropship;
    }
  }
}

