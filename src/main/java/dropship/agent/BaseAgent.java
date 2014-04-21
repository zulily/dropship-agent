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

public abstract class BaseAgent {

  private static final AtomicReference<BaseAgent> INSTANCE = new AtomicReference<BaseAgent>();

  private static BaseAgent get() {
    BaseAgent instance = INSTANCE.get();
    if (instance == null) {
      throw new IllegalStateException("Agent instance has not been set! Call BaseAgent.premain(String agentArg, Instrumentation inst, BaseAgent agent)");
    }
    return instance;
  }

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

