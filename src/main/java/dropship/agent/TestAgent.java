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
