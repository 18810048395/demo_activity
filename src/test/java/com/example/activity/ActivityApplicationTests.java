package com.example.activity;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.HistoryService;
import org.activiti.engine.RepositoryService;
import org.activiti.engine.RuntimeService;
import org.activiti.engine.TaskService;
import org.activiti.engine.history.HistoricActivityInstance;
import org.activiti.engine.history.HistoricActivityInstanceQuery;
import org.activiti.engine.repository.Deployment;
import org.activiti.engine.repository.DeploymentBuilder;
import org.activiti.engine.runtime.ProcessInstance;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
@Slf4j
class ActivityApplicationTests {
    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private TaskService taskService;

    @Autowired
    private RepositoryService repositoryService;

    @Autowired
    private HistoryService historyService;

    /**
     * 当数据库中没有流程表的话，会先自动创建25张表
     */
    @Test
    void contextLoads() {
        log.info("自动建表并部署流程...");
    }

    /**
     * 流程部署，单个文件
     */
    @Test
    public void testDeployment(){

        //使用RepositoryService进行部署
        DeploymentBuilder builder = repositoryService.createDeployment();
        builder.addClasspathResource("process/demo_process.bpmn20.xml");
//        builder.addClasspathResource("process/demo_process.png");
        builder.name("demo_process");
        Deployment deployment = builder.deploy();

        //输出部署信息
        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());

        //流程部署id：125098e1-ffd9-11eb-8847-02004c4f4f50
        //流程部署名称：demo_process

    }

    /**
     * 流程实例启动
     */
    @Test
    public void testStartProcess(){
        //根据流程定义Id启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("myProcess_1");

        //输出实例信息
        System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id：" + processInstance.getId());
        System.out.println("当前活动Id：" + processInstance.getActivityId());

        //流程定义id：myProcess_1:1:12702ed4-ffd9-11eb-8847-02004c4f4f50
        //流程实例id：a9b162aa-ffda-11eb-bad1-02004c4f4f50
        //当前活动Id：null

    }

    /**
     * 任务查询
     */
    @Test
    public void testFindPersonalTaskList() {
        //任务负责人
        String assignee = "worker";

        //根据流程key 和 任务负责人 查询任务
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey("myProcess_1")
//                .taskAssignee(assignee)
                .list();

        for (Task task : list) {

            System.out.println("流程实例id：" + task.getProcessInstanceId());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());

        }

        //流程实例id：a9b162aa-ffda-11eb-bad1-02004c4f4f50
        //任务id：a9b5815e-ffda-11eb-bad1-02004c4f4f50
        //任务负责人：manager
        //任务名称：提交申请

    }

    /**
     * 完成任务
     */
    @Test
    public void completTask(){

        //根据流程key和任务的负责人查询任务并选择其中的一个任务处理,这里用的
        //是singleResult返回一条，真实环境中是通过步骤5中查询出所有的任务，然后在页面上选择一个任务进行处理.
        Task task = taskService.createTaskQuery()
                .processDefinitionKey("myProcess_1") //流程Key
                .taskAssignee("manager")  //要查询的负责人
                .singleResult();

        //完成任务,参数：任务id
        taskService.complete(task.getId());

    }

    /**
     * 流程结束，或流程流转过程中的历史信息查询
     */
    @Test
    public void findHistoryInfo(){

        //获取 actinst表的查询对象
        HistoricActivityInstanceQuery instanceQuery = historyService.createHistoricActivityInstanceQuery();
        //查询 actinst表，条件：根据 InstanceId 查询
        instanceQuery.processInstanceId("cebd8593-87da-11ec-8ed5-4eebbd9ecca7");
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
