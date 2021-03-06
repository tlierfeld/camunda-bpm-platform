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

package org.camunda.bpm.engine.impl.form.type;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.camunda.bpm.engine.ProcessEngineException;


/**
 * @author Tom Baeyens
 */
public class DateFormType extends AbstractFormFieldType {

  protected String datePattern;
  protected Format dateFormat;

  public DateFormType(String datePattern) {
    this.datePattern = datePattern;
    this.dateFormat = new SimpleDateFormat(datePattern);
  }

  public String getName() {
    return "date";
  }

  public Object getInformation(String key) {
    if ("datePattern".equals(key)) {
      return datePattern;
    }
    return null;
  }

  public Object convertFormValueToModelValue(Object propertyValue) {
    if (propertyValue==null || "".equals(propertyValue)) {
      return null;
    }
    try {
      return dateFormat.parseObject(propertyValue.toString());
    } catch (ParseException e) {
      throw new ProcessEngineException("invalid date value "+propertyValue);
    }
  }

  public String convertModelValueToFormValue(Object modelValue) {
    if (modelValue==null) {
      return null;
    }
    return dateFormat.format(modelValue);
  }
}
