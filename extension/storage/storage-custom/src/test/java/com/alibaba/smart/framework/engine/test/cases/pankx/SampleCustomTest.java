package com.alibaba.smart.framework.engine.test.cases.pankx;

import java.util.List;
import java.util.Map;

import com.alibaba.smart.framework.engine.SmartEngine;
import com.alibaba.smart.framework.engine.common.util.MapUtil;
import com.alibaba.smart.framework.engine.configuration.ProcessEngineConfiguration;
import com.alibaba.smart.framework.engine.configuration.impl.DefaultIdGenerator;
import com.alibaba.smart.framework.engine.configuration.impl.DefaultProcessEngineConfiguration;
import com.alibaba.smart.framework.engine.configuration.impl.DefaultSmartEngine;
import com.alibaba.smart.framework.engine.extension.scanner.SimpleAnnotationScanner;
import com.alibaba.smart.framework.engine.model.assembly.ProcessDefinition;
import com.alibaba.smart.framework.engine.model.instance.ExecutionInstance;
import com.alibaba.smart.framework.engine.persister.custom.session.PersisterSession;
import com.alibaba.smart.framework.engine.service.command.ExecutionCommandService;
import com.alibaba.smart.framework.engine.service.command.ProcessCommandService;
import com.alibaba.smart.framework.engine.service.command.RepositoryCommandService;
import com.alibaba.smart.framework.engine.service.query.ExecutionQueryService;

import org.junit.Test;

/**
 * @author pankx
 * @Date 2021/6/23 下午5:02
 */
public class SampleCustomTest {

    private   SimpleAnnotationScanner simpleAnnotationScanner ;


    @Test
    public void test(){
        PersisterSession.create();
        this.simpleAnnotationScanner = new SimpleAnnotationScanner(SmartEngine.class.getPackage().getName());


        /** 初始化 **/
        ProcessEngineConfiguration configuration = new DefaultProcessEngineConfiguration();
        configuration.setIdGenerator(new DefaultIdGenerator());

        SmartEngine smartEngine = new DefaultSmartEngine();
        smartEngine.init(configuration);

        /** 流程实例管理服务 start,abort **/
        ProcessCommandService processCommandService = smartEngine.getProcessCommandService();
        /** 驱动引擎流转服务 signal,jumpTo,reTry **/
        ExecutionCommandService executionCommandService = smartEngine.getExecutionCommandService();
        /** 查询执行实例服务 **/
        ExecutionQueryService executionQueryService = smartEngine.getExecutionQueryService();
        /** 负责解析 XML加载到内存 deploy  **/
        RepositoryCommandService repositoryCommandService = smartEngine.getRepositoryCommandService();

        /** 流程定义对象 **/
        ProcessDefinition processDefinition = repositoryCommandService.deploy("sample-diagram.bpmn.xml").getFirstProcessDefinition();
        Map<String,Object> request = MapUtil.newHashMap();
        request.put("","");

        /** 执行流程 **/
        processCommandService.start(processDefinition.getId(),processDefinition.getVersion(),request);

        /** <p>查询</p> **/
        /** 执行实例对象  **/
        List<ExecutionInstance> executionInstanceList = executionQueryService.findActiveExecutionList(processDefinition.getId());
        //String instanceId = executionInstanceList.get(0).getInstanceId();
        //System.out.println(instanceId);

        /** 对receiveTask 进行通知 **/
        //executionCommandService.signal(instanceId,request);

        PersisterSession.destroySession();
        simpleAnnotationScanner.clear();


    }
}
