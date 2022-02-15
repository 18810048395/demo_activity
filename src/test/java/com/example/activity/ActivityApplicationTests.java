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
import org.activiti.engine.task.IdentityLink;
import org.activiti.engine.task.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;


/**
 *   使用注入activity原先的api,不会受springSecurity影响
 *   根据业务需要，考虑是否使用activity7新特性
 *   注意：新特性需要依赖springSecurity，需要配置一些activity的默认权限 (具体参考：Activity7ApplicationTests)
 */
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
     * 自动建表...(如果bpmn的目录是processes的默目录下，还会自动部署流程。否则需要手动部署流程)
     */
    @Test
    void contextLoads() {
        log.info("自动建表...");
    }

    /**
     * 流程部署，单个文件
     */
    @Test
    public void testDeployment(){

        //使用RepositoryService进行部署
        DeploymentBuilder builder = repositoryService.createDeployment();
        builder.addClasspathResource("process/demo_process.bpmn");
//        builder.addClasspathResource("process/demo_process.png");
        builder.name("demo");
        Deployment deployment = builder.deploy();

        //输出部署信息
        System.out.println("流程部署id：" + deployment.getId());
        System.out.println("流程部署名称：" + deployment.getName());

    }

    /**
     * 流程实例启动
     */
    @Test
    public void testStartProcess(){
        //根据流程定义Id启动流程
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("demo","1001");

        //输出实例信息
        System.out.println("流程定义id：" + processInstance.getProcessDefinitionId());
        System.out.println("流程实例id：" + processInstance.getId());
        System.out.println("当前活动Id：" + processInstance.getActivityId());
        System.out.println("当前业务id：" + processInstance.getBusinessKey());

        // todo 如拓展到实际业务中，可以将流程的业务id和自身的业务表关联起来
        // holiday.setBusinessKey()


    }

    /**
     * 任务查询
     */
    @Test
    public void testFindPersonalTaskList() {
        //任务负责人
        String assignee = "jack";
        String candidateUser = "rose";
        //根据流程key 和 任务负责人 查询任务
        List<Task> list = taskService.createTaskQuery()
                .processDefinitionKey("demo")
//                .taskAssignee(assignee)
                .taskCandidateUser(candidateUser)// 指定组任务查询
                .list();
        for (Task task : list) {
            System.out.println("流程实例id：" + task.getProcessInstanceId());
            ProcessInstance processInstance = runtimeService.createProcessInstanceQuery().
                                                   processInstanceId(task.getProcessInstanceId()).singleResult();
            System.out.println("流程的业务id：" + processInstance.getBusinessKey());
            System.out.println("任务id：" + task.getId());
            System.out.println("任务负责人：" + task.getAssignee());
            System.out.println("任务名称：" + task.getName());
        }
    }

    /**
     * 查看组任务成员列表
     */
    @Test
    public void findGroupUser() {
        String taskId = "07659bbb-8e2b-11ec-a55d-4eebbd9ecca7";
        List<IdentityLink> list = taskService
                .getIdentityLinksForTask(taskId);// 获取列表
        if (list != null && list.size() > 0) {
            for (IdentityLink il : list) {
                System.out.println("用户：" + il.getUserId());
            }
        }
    }

    /**
     * 将组任务分配给个人任务，拾取任务
     * 注意：认领任务的时候，可以是组任务成员中的人，也可以不是组任务成员的人，此时通过Type的类型为participant来指定任务的办理人
     * 由一个人去完成任务
     */
    @Test
    public void claim() {
        String taskId = "4fda5b4a-8e2f-11ec-89fc-4eebbd9ecca7";//任务ID
        String userId = "rose";//分配的办理人
        taskService.claim(taskId, userId);
    }

    /**
     * 完成任务
     */
    @Test
    public void completTask(){

        //根据流程key和任务的负责人查询任务并选择其中的一个任务处理,这里用的
        //是singleResult返回一条，真实环境中是通过步骤5中查询出所有的任务，然后在页面上选择一个任务进行处理.
        Task task = taskService.createTaskQuery()
                .processDefinitionKey("demo") //流程Key
                .taskAssignee("rose")  //要查询的负责人
                .singleResult();

        //完成任务,参数：任务id
        if(null != task ){
            taskService.complete(task.getId());
            System.out.println("任务完成==>taskId: "+task.getId());
        }else {
            System.out.println("没有查到可执行的任务");
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
