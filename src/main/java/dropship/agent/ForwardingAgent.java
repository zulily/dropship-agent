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

import java.lang.reflect.Method;
import java.util.Properties;

final class ForwardingAgent extends BaseAgent {

  private final Iterable<? extends BaseAgent> agents;

  ForwardingAgent(Iterable<? extends BaseAgent> agents) {
    this.agents = agents;
  }

  @Override
  protected void onStart(Properties properties, String groupArtifactString, Class<?> mainClass, Method mainMethod, Object[] arguments) {
    for (BaseAgent agent : agents) {
      agent.onStart(properties, groupArtifactString, mainClass, mainMethod, arguments);
    }
  }

  @Override
  protected void onError(Throwable t) {
    for (BaseAgent agent : agents) {
      agent.onError(t);
    }
  }

  @Override
  protected void onExit() {
    for (BaseAgent agent : agents) {
      agent.onExit();
    }
  }
}
