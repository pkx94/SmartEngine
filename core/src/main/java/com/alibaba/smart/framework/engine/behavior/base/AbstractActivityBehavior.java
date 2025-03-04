package com.alibaba.smart.framework.engine.behavior.base;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.alibaba.smart.framework.engine.behavior.ActivityBehavior;
import com.alibaba.smart.framework.engine.bpmn.behavior.gateway.ExclusiveGatewayBehaviorHelper;
import com.alibaba.smart.framework.engine.common.util.MapUtil;
import com.alibaba.smart.framework.engine.common.util.MarkDoneUtil;
import com.alibaba.smart.framework.engine.configuration.ProcessEngineConfiguration;
import com.alibaba.smart.framework.engine.context.ExecutionContext;
import com.alibaba.smart.framework.engine.instance.factory.ActivityInstanceFactory;
import com.alibaba.smart.framework.engine.instance.factory.ExecutionInstanceFactory;
import com.alibaba.smart.framework.engine.instance.factory.ProcessInstanceFactory;
import com.alibaba.smart.framework.engine.instance.factory.TaskInstanceFactory;
import com.alibaba.smart.framework.engine.instance.storage.ExecutionInstanceStorage;
import com.alibaba.smart.framework.engine.model.assembly.Activity;
import com.alibaba.smart.framework.engine.model.assembly.IdBasedElement;
import com.alibaba.smart.framework.engine.model.instance.ActivityInstance;
import com.alibaba.smart.framework.engine.model.instance.ExecutionInstance;
import com.alibaba.smart.framework.engine.model.instance.ProcessInstance;
import com.alibaba.smart.framework.engine.pvm.PvmActivity;
import com.alibaba.smart.framework.engine.pvm.PvmTransition;
import com.alibaba.smart.framework.engine.pvm.event.PvmEventConstant;

import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author 高海军 帝奇  2016.11.11
 * @author ettear 2016.04.13
 */
public abstract class AbstractActivityBehavior<T extends Activity> implements ActivityBehavior {



    @Setter
    protected ProcessInstanceFactory processInstanceFactory;
    @Setter
    protected ExecutionInstanceFactory executionInstanceFactory;
    @Setter
    protected ActivityInstanceFactory activityInstanceFactory;
    @Setter
    protected TaskInstanceFactory taskInstanceFactory;
    @Setter
    protected ProcessEngineConfiguration processEngineConfiguration;
    @Setter
    protected ExecutionInstanceStorage executionInstanceStorage;

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractActivityBehavior.class);

    protected void fireEvent(ExecutionContext context,PvmActivity pvmActivity, PvmEventConstant event) {

        context.getProcessEngineConfiguration().getListenerExecutor().execute(event,pvmActivity.getModel(),context);

    }

    @Override
    public boolean enter(ExecutionContext context, PvmActivity pvmActivity) {

        ProcessInstance processInstance = context.getProcessInstance();

        synchronized (processInstance){

            ActivityInstance activityInstance = createSingleActivityInstanceAndAttachToProcessInstance(context, pvmActivity.getModel());

            ExecutionInstance executionInstance = this.executionInstanceFactory.create(activityInstance, context);
            List<ExecutionInstance> executionInstanceList = new ArrayList<ExecutionInstance>(1);
            executionInstanceList.add(executionInstance);

            activityInstance.setExecutionInstanceList(executionInstanceList);
            context.setExecutionInstance(executionInstance);

            IdBasedElement idBasedElement = context.getProcessDefinition().getIdBasedElementMap().get(executionInstance.getProcessDefinitionActivityId());
            context.setBaseElement(idBasedElement);

            fireEvent(context,pvmActivity,PvmEventConstant.ACTIVITY_START);


            return false;
        }

    }

    protected ActivityInstance createSingleActivityInstanceAndAttachToProcessInstance(ExecutionContext context, Activity activity) {
        ProcessInstance processInstance = context.getProcessInstance();

        ActivityInstance activityInstance = this.activityInstanceFactory.create(activity, context);
        processInstance.addActivityInstance(activityInstance);
        context.setActivityInstance(activityInstance);

        return activityInstance;
    }


    @Override
    public void execute(ExecutionContext context,PvmActivity pvmActivity) {

        fireEvent(context,pvmActivity,PvmEventConstant.ACTIVITY_EXECUTE);


        executeDelegation(context,pvmActivity.getModel());

        commonUpdateExecutionInstance(context);

    }

    protected void commonUpdateExecutionInstance(ExecutionContext context) {
        if (!context.isNeedPause()) {
            ExecutionInstance executionInstance = context.getExecutionInstance();
            MarkDoneUtil.markDoneExecutionInstance(executionInstance,executionInstanceStorage,
                processEngineConfiguration);
        }
    }

    protected void executeDelegation(ExecutionContext context, Activity activity) {

        context.getProcessEngineConfiguration().getDelegationExecutor().execute(context, activity);
    }



    @Override
    public void leave(ExecutionContext context, PvmActivity pvmActivity) {
        fireEvent(context,pvmActivity,PvmEventConstant.ACTIVITY_END);


        //执行每个节点的hook方法
        Map<String, PvmTransition> outcomeTransitions = pvmActivity.getOutcomeTransitions();

        if(MapUtil.isEmpty(outcomeTransitions)){
            LOGGER.info("No outcomeTransitions found for activity id: "+pvmActivity.getModel().getId()+", it's just fine, it should be the last activity of the process");

            return;
        }else{

            if( outcomeTransitions.size() ==1){
                for (Entry<String, PvmTransition> pvmTransitionEntry : outcomeTransitions.entrySet()) {
                    PvmActivity target = pvmTransitionEntry.getValue().getTarget();
                    target.enter(context);
                }
            }else {

                 ExclusiveGatewayBehaviorHelper.chooseOnlyOne(context, outcomeTransitions);

            }
        }

    }



}
