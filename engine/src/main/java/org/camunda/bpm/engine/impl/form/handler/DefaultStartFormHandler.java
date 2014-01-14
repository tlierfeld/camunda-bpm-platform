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

package org.camunda.bpm.engine.impl.form.handler;

import java.util.Map;

import org.camunda.bpm.engine.form.StartFormData;
import org.camunda.bpm.engine.impl.bpmn.parser.BpmnParse;
import org.camunda.bpm.engine.impl.form.StartFormDataImpl;
import org.camunda.bpm.engine.impl.persistence.entity.DeploymentEntity;
import org.camunda.bpm.engine.impl.persistence.entity.ProcessDefinitionEntity;
import org.camunda.bpm.engine.impl.pvm.delegate.ActivityExecution;
import org.camunda.bpm.engine.impl.util.xml.Element;


/**
 * @author Tom Baeyens
 */
public class DefaultStartFormHandler extends DefaultFormHandler implements StartFormHandler {

  @Override
  public void parseConfiguration(Element activityElement, DeploymentEntity deployment, ProcessDefinitionEntity processDefinition, BpmnParse bpmnParse) {
    super.parseConfiguration(activityElement, deployment, processDefinition, bpmnParse);
    if (formKey!=null) {
      processDefinition.setStartFormKey(true);
    }
  }

  public StartFormData createStartFormData(ProcessDefinitionEntity processDefinition) {
    StartFormDataImpl startFormData = new StartFormDataImpl();
    if (formKey != null) {
      startFormData.setFormKey(formKey.getExpressionText());
    }
    startFormData.setDeploymentId(deploymentId);
    startFormData.setProcessDefinition(processDefinition);
    initializeFormProperties(startFormData, null);
    initializeFormFields(startFormData, null);
    return startFormData;
  }

  public ActivityExecution submitStartFormData(ActivityExecution processInstance, Map<String, Object> properties) {
    submitFormProperties(properties, processInstance);
    return processInstance;
  }
}
