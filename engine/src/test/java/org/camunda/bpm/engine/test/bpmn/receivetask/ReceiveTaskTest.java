/* Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.camunda.bpm.engine.test.bpmn.receivetask;

import org.camunda.bpm.engine.impl.event.MessageEventHandler;
import org.camunda.bpm.engine.impl.test.PluggableProcessEngineTestCase;
import org.camunda.bpm.engine.runtime.EventSubscription;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.test.Deployment;

import java.util.List;

/**
 * see https://app.camunda.com/jira/browse/CAM-1612
 *
 * @author Daniel Meyer
 * @author Danny Gräf
 *
 */
public class ReceiveTaskTest extends PluggableProcessEngineTestCase {

  @Deployment(resources = "org/camunda/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.singleReceiveTask.bpmn20.xml")
  public void testSupportsMessageEventReceived() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // then: there is a message event subscription for the task
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery()
        .eventType(MessageEventHandler.EVENT_HANDLER_TYPE).singleResult();
    assertNotNull(subscription);

    // then: we can trigger the event subscription
    runtimeService.messageEventReceived(subscription.getEventName(), subscription.getExecutionId());

    // expect: this ends the process instance
    assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.multiReceiveTask.bpmn20.xml")
  public void testSupportsMessageEventReceivedOnMultiInstance() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // then: there are two message event subscriptions
    List<EventSubscription> subscriptions = runtimeService.createEventSubscriptionQuery()
        .eventType(MessageEventHandler.EVENT_HANDLER_TYPE).list();
    assertEquals(2, subscriptions.size());

    // then: we can trigger the both event subscription
    runtimeService.messageEventReceived(subscriptions.get(0).getEventName(), subscriptions.get(0).getExecutionId());
    runtimeService.messageEventReceived(subscriptions.get(1).getEventName(), subscriptions.get(1).getExecutionId());

    // expect: this ends the process instance
    assertProcessEnded(processInstance.getId());
  }

  @Deployment(resources = "org/camunda/bpm/engine/test/bpmn/receivetask/ReceiveTaskTest.singleReceiveTask.bpmn20.xml")
  public void testSupportsCorrelateMessage() {

    // given: a process instance waiting in the receive task
    ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("testProcess");

    // then: there is a message event subscription for the task
    EventSubscription subscription = runtimeService.createEventSubscriptionQuery()
        .eventType(MessageEventHandler.EVENT_HANDLER_TYPE).singleResult();
    assertNotNull(subscription);

    // then: there is a message event subscription for the task
    subscription = runtimeService.createEventSubscriptionQuery()
        .eventType(MessageEventHandler.EVENT_HANDLER_TYPE).singleResult();
    assertNotNull(subscription);

    // then: we can correlate the event subscription
    runtimeService.correlateMessage(subscription.getEventName());

    // expect: this ends the process instance
    assertProcessEnded(processInstance.getId());
  }

}
