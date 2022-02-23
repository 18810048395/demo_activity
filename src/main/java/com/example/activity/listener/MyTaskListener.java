package com.example.activity.listener;

import lombok.extern.slf4j.Slf4j;
import org.activiti.engine.delegate.DelegateTask;
import org.activiti.engine.delegate.TaskListener;

import org.springframework.stereotype.Component;

/**
 * 当到达经理审批任务节点时，进行动态添加候选组成员
 */
@Slf4j
@Component
public class MyTaskListener implements TaskListener {
    @Override
    public void notify(DelegateTask delegateTask) {
        delegateTask.addCandidateUser("rose");//分配组任务的办理人
        delegateTask.addCandidateUser("tom");
        delegateTask.addCandidateUser("jack");
    }
}
