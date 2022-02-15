package com.example.activity;

import com.example.activity.util.SecurityUtil;
import lombok.extern.slf4j.Slf4j;
import org.activiti.api.process.model.ProcessDefinition;
import org.activiti.api.process.model.ProcessInstance;
import org.activiti.api.process.model.builders.ProcessPayloadBuilder;
import org.activiti.api.process.runtime.ProcessRuntime;
import org.activiti.api.runtime.shared.query.Page;
import org.activiti.api.runtime.shared.query.Pageable;
import org.activiti.api.task.model.Task;
import org.activiti.api.task.model.builders.TaskPayloadBuilder;
import org.activiti.api.task.runtime.TaskRuntime;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

/**
 *   使用注入activity7新版的api,受springSecurity影响，
 *   需要提供SecurityUtil模仿登陆，调用API必须有ROLE_ACTIVITI_USER才可以访问。
 *   activity7新特性可不通过监听器，手动配置候选人组，
 *   在画流程图时，可通过springSecurity中的GROUP_activitiTeam进行候选组配置。
 *   可根据系统的实际情况选择使用老版API还是接入新版API。
 */
@SpringBootTest
@Slf4j
class Activity7ApplicationTests {

    @Autowired
    private ProcessRuntime processRuntime;

    @Autowired
    private TaskRuntime taskRuntime;

    @Autowired
    private SecurityUtil securityUtil;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;

    /**
     * 自动建表...(如果bpmn的目录是processes的默目录下，还会自动部署流程。否则需要手动部署流程)
     */
    @Test
    void contextLoads() {
        log.info("自动建表...");
    }


    /**
     * 流程部署，单个文件
     *
     */
    @Test
    public void testDeployment(){

        //使用RepositoryService进行部署
        DeploymentBuilder builder = repositoryService.createDeployment();
        builder.addClasspathResource("process/demo1_process.bpmn");
//        builder.addClasspathResource("process/demo_process.png");
        builder.name("demo1");
        Deployment deployment = builder.deploy();

        //输出部署信息
        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());


    }

    @Test
    public void findProcess(){
        // activity7新版的api,受springSecurity影响,必须模拟登陆且拥有"ROLE_ACTIVITI_USER"权限
        securityUtil.logInAs("jack");
//        流程定义的分页对象
        Page<ProcessDefinition> definitionPage = processRuntime.processDefinitions(Pageable.of(0, 10));
        log.info("可用的流程定义总数：{}",definitionPage.getTotalItems());
        for (ProcessDefinition processDefinition : definitionPage.getContent()) {
            System.out.println("==============================");
            log.info("流程定义内容：{}",processDefinition);
            System.out.println("==============================");
        }
    }

    /**
     * 启动流程
     *  本次demo流程是先由jack发起申请，然后完成申请任务后
     *                 由rose或者tom完成经理审批完成任务
     */
    @Test
    public void startProcess(){
//        设置登录用户
        securityUtil.logInAs("jack");
        ProcessInstance processInstance = processRuntime.
                start(ProcessPayloadBuilder.
                        start().
                        withProcessDefinitionKey("demo1").
                        build());
        log.info("流程实例的内容，{}",processInstance);

    }

    /**
     * 任务查询,并执行任务
     */
    @Test
    public void doTask(){
//        设置登录用户
        securityUtil.logInAs("rose");
//        查询任务
        Page<org.activiti.api.task.model.Task> taskPage = taskRuntime.tasks(Pageable.of(0, 10));
        if(taskPage != null && taskPage.getTotalItems()>0){
            for (Task task : taskPage.getContent()) {
                // 拾取任务(当任务的待办人是候选组或者候选人形式，需要先拾取任务再完成)
                taskRuntime.claim(TaskPayloadBuilder.
                        claim().
                        withTaskId(task.getId()).
                        build());
                log.info("任务内容,{}",task);
                // 完成任务 (如果当前任务节点是员工申请节点，则不需要执行上面的拾取任务)
                taskRuntime.complete(TaskPayloadBuilder.
                        complete().
                        withTaskId(task.getId()).
                        build());
            }
        }


    }

    /**
     * 流程结束，或流程流转过程中的历史信息查询
     */
    @Test
    public void findHistoryInfo(){

        //获取 actinst表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        //查询 actinst表，条件：根据 InstanceId 查询
        instanceQuery.processInstanceId("7de62807-8e0c-11ec-944e-4eebbd9ecca7");
        //增加排序操作,orderByHistoricActivityInstanceStartTime 根据开始时间排序 asc 升序
        instanceQuery.orderByHistoricActivityInstanceStartTime().asc();
        //查询所有内容
        List<HistoricActivityInstance> activityInstanceList = instanceQuery.list();
        //输出结果
        for (HistoricActivityInstance hi : activityInstanceList) {

            System.out.println("");
            System.out.println("===================-===============");
            System.out.println(hi.getStartTime());
            System.out.println(hi.getAssignee());
            System.out.println(hi.getActivityId());
            System.out.println(hi.getActivityName());
            System.out.println(hi.getProcessDefinitionId());
            System.out.println(hi.getProcessInstanceId());
            System.out.println("===================-===============");
            System.out.println("");

        }
    }

}
