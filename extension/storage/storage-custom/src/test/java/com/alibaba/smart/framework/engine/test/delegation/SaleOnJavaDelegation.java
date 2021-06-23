package com.alibaba.smart.framework.engine.test.delegation;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import com.alibaba.smart.framework.engine.common.util.DateUtil;
import com.alibaba.smart.framework.engine.common.util.StringUtil;
import com.alibaba.smart.framework.engine.constant.ExtensionElementsConstant;
import com.alibaba.smart.framework.engine.context.ExecutionContext;
import com.alibaba.smart.framework.engine.delegation.JavaDelegation;
import com.alibaba.smart.framework.engine.model.assembly.ExtensionElementContainer;
import com.alibaba.smart.framework.engine.model.assembly.ExtensionElements;

/**
 * @author pankx
 * @Date 2021/6/23 下午6:21
 */
public class SaleOnJavaDelegation implements JavaDelegation {

    @Override
    public void execute(ExecutionContext executionContext) {
        String processDefinitionActivityId = executionContext.getExecutionInstance().getProcessDefinitionActivityId();
        ExtensionElementContainer idBasedElement = (ExtensionElementContainer)executionContext.getProcessDefinition()
            .getIdBasedElementMap().get(processDefinitionActivityId);
        ExtensionElements extensionElements = idBasedElement.getExtensionElements();

        Map map = (Map)extensionElements.getDecorationMap().get(ExtensionElementsConstant.PROPERTIES);
        String runTime = (String)map.get("runDate");
        if(StringUtil.isNotEmpty(runTime)){
            try {
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                Date date = simpleDateFormat.parse(runTime);
                if(DateUtil.getCurrentDate().compareTo(date) > 0){
                    System.out.println("发送通知: 可以开始大促正式开始啦！！！");
                }
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
    }
}
