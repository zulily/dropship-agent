package dropship.agent;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Properties;

/**
 * This class shows how a Dropship agent should be implemented. Note the {@code premain} method implementation.
 */
public final class TestAgent extends BaseAgent {

  public static void premain(final String agentArgument, final Instrumentation instrumentation) {
    BaseAgent.premain(agentArgument, instrumentation, new TestAgent());
  }

  @Override
  public void onStart(Properties properties, String groupArtifactString, Class<?> mainClass, Method mainMethod, Object[] arguments) {
    System.out.format("onStart(%s, %s, %s, %s, %s)%n", properties, groupArtifactString, mainClass, mainMethod, Arrays.toString(arguments));
  }

  @Override
  public void onError(Throwable t) {
    System.out.format("onError(%s)%n", t);
  }

  @Override
  public void onExit() {
    System.out.format("onExit()");
  }
}
