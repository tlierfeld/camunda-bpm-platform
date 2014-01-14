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
package org.camunda.bpm.engine.impl.pvm.runtime;

import java.util.List;

import org.camunda.bpm.engine.impl.pvm.PvmActivity;
import org.camunda.bpm.engine.impl.pvm.PvmScope;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.engine.impl.pvm.process.ScopeImpl;

/**
 * @author Daniel Meyer
 *
 */
public class AtomicOperationActivityStartConcurrent extends AtomicOperationActivityInstanceStart {

  @Override
  protected InterpretableExecution eventNotificationsStarted(InterpretableExecution execution) {

    // TODO: make sure this is always invoked on a scope execution

    // the execution which will continue
    InterpretableExecution propagatingExecution = execution;

    // the activity which is to be executed concurrently
    PvmActivity concurrentActivity = execution.getActivityStartConcurrent();
    PvmScope concurrencyScope = concurrentActivity.getScope();

    List<? extends ActivityExecution> childExecutions = execution.getExecutions();

    if(childExecutions.isEmpty()) {

      if(execution.getActivity() == concurrencyScope) {

        // Expand tree (1):
        //
        //        ...                        ...
        //         |                          |
        //      +------+                  +-------+   s=tt
        //      |  e   |       =>         |   e   |   cc=ff
        //      +------+                  +-------+
        //          s=tt                      ^
        //         cc=ff                     / \
        //                                  /   \
        //                                 /     \          Both:
        //                          +-------+   +--------+    s=ff
        //                          | CCE-1 |   | PPE    |   cc=tt
        //                          +-------+   +--------+
        //

        // 1) create new concurrent execution (CCE-1) replacing the the active scope execution (e)
        InterpretableExecution replacingExecution = (InterpretableExecution) execution.createExecution();
        replacingExecution.replace(execution); // only copy tasks(?)
        replacingExecution.setActivity((ActivityImpl) execution.getActivity());
        replacingExecution.setActive(execution.isActive());
        replacingExecution.setScope(false);
        replacingExecution.setConcurrent(true);

        execution.setActive(false);
        execution.setActivity(null);

        // 2) create new concurrent execution (PPE) for new activity instance
        propagatingExecution = createConcurrentExecution(execution, concurrentActivity);

      } else {

        if(execution.getParent().isConcurrent()) {

          // Add to existing concurrency tree rooting at parent of parent
          //
          //                 ...
          //                  |
          //              +-------+   s=tt
          //              |   pp  |   cc=ff
          //              +-------+
          //                  ^
          //                 / \                   =>
          //                /   \
          //               /     \     all:
          //        +--------+   ....   s=ff
          //        | parent |          cc=tt
          //        +--------+
          //             |
          //        +-------+ s=tt
          //        | e     | cc=ff
          //        +-------+

          InterpretableExecution concurrentRoot = (InterpretableExecution) execution.getParent().getParent();
          propagatingExecution = createConcurrentExecution(concurrentRoot, concurrentActivity);

        } else {

          // Expand tree (2):
          //
          //        ...                         ...
          //         |                           |
          //      +------+ s=tt              +-------+ s=tt
          //      |  p   | cc=ff     =>      |   p   | cc=ff
          //      +------+                   +-------+
          //         |                           ^
          //         |                          / \
          //         |                         /   \
          //         |                        /     \          Both:
          //      +------+ s=tt        +-------+   +--------+    s=ff
          //      |  e   | cc=ff       |   e   |   |  PPE   |   cc=tt
          //      +------+             +-------+   +--------+
          //

          // 1) mark e concurrent
          execution.setConcurrent(true);

          // 2) create new concurrent execution (PPE) for new activity instance
          InterpretableExecution concurrentRoot = (InterpretableExecution) execution.getParent();
          propagatingExecution = createConcurrentExecution(concurrentRoot, concurrentActivity);
        }

      }

    } else if(execution.getExecutions().size()==1
      && execution.getActivity() == null
      && !execution.isActive()) {

      // Expand tree (2):
      //
      //        ...                         ...
      //         |                           |
      //      +------+ s=tt              +-------+ s=tt
      //      |  e   | cc=ff     =>      |   e   | cc=ff
      //      +------+                   +-------+
      //         |                           ^
      //         |                          / \
      //         |                         /   \
      //         |                        /     \          Both:
      //      +------+ s=tt        +-------+   +--------+    s=ff
      //      |child | cc=ff       | child |   |  PPE   |   cc=tt
      //      +------+             +-------+   +--------+
      //

      // 1) mark existing child concurrent
      InterpretableExecution existingChild = (InterpretableExecution) execution.getExecutions().get(0);
      existingChild.setConcurrent(true);

      // 2) create new concurrent execution (PPE) for new activity instance
      propagatingExecution = createConcurrentExecution(execution, concurrentActivity);



    } else { /* execution.getExecutions().size() > 1 */

      // Add to existing concurrency tree:
      //
      //                 ...
      //                  |
      //              +-------+   s=tt
      //              |   e   |   cc=ff
      //              +-------+
      //                  ^
      //                 / \
      //                /   \
      //               /     \     all:
      //        +-------+   ....     s=?
      //        |       |            cc=tt
      //        +-------+
      //

      InterpretableExecution concurrentRoot = execution;

      PvmScope parentScope = concurrentActivity.getParent();
      if(parentScope instanceof ActivityImpl) {
        ActivityImpl parentActivity = (ActivityImpl) parentScope;
        if(parentActivity.isScope()) {
          concurrentRoot = (InterpretableExecution) execution.getParent();
          if(concurrentRoot.isConcurrent()) {
            concurrentRoot = (InterpretableExecution) concurrentRoot.getParent();
          }
        }
      }

      propagatingExecution = createConcurrentExecution(concurrentRoot, concurrentActivity);

    }

    return super.eventNotificationsStarted(propagatingExecution);

  }

  @Override
  protected void eventNotificationsCompleted(InterpretableExecution execution) {
    super.eventNotificationsCompleted(execution);
    execution.performOperation(AtomicOperation.ACTIVITY_EXECUTE);
  }

  private InterpretableExecution createConcurrentExecution(InterpretableExecution execution, PvmActivity concurrentActivity) {
    InterpretableExecution newConcurrentExecution = (InterpretableExecution) execution.createExecution();
    newConcurrentExecution.setActivity((ActivityImpl) concurrentActivity);
    newConcurrentExecution.setScope(false);
    newConcurrentExecution.setActive(true);
    newConcurrentExecution.setConcurrent(true);
    return newConcurrentExecution;
  }

  /**
   * Returns true if the execution is the root of a concurrency tree:
   * @param execution
   * @return
   */
  protected boolean isConcurrentRoot(InterpretableExecution execution) {
    List<? extends ActivityExecution> executions = execution.getExecutions();
    if(executions == null || executions.size() == 0) {
      return false;
    } else {
      return executions.get(0).isConcurrent();
    }
  }

  /**
   * @param execution
   * @return
   */
  protected boolean isLeaf(InterpretableExecution execution) {
    return execution.getExecutions().isEmpty();
  }

  /**
   * @param execution
   * @return
   */
  protected PvmScope getCurrentScope(InterpretableExecution execution) {
    ActivityImpl activity = (ActivityImpl) execution.getActivity();
    if(activity == null) {
      return null;
    } else {
      return activity.isScope() ? activity : activity.getParent();
    }
  }

  public String getCanonicalName() {
    return "activity-start-concurrent";
  }

  protected ScopeImpl getScope(InterpretableExecution execution) {
    return (ScopeImpl) execution.getActivityStartConcurrent();
  }

  protected String getEventName() {
    return org.camunda.bpm.engine.impl.pvm.PvmEvent.EVENTNAME_START;
  }


}