package com.github.missthee.controller.flowable;

import com.alibaba.fastjson.JSONObject;
import com.github.missthee.tool.Res;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.flowable.variable.api.history.HistoricVariableInstanceQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.github.missthee.controller.flowable.JOTool.getMapOrDefaultFromJO;
import static com.github.missthee.controller.flowable.JOTool.getStringOrDefaultFromJO;

@RestController
@RequestMapping("flowable/use")
public class ProcessUseController {
    //基础配置类
    //private final ProcessEngine processEngine;
    //流程部署、修改、删除服务。主要操作表：act_ge_bytearray,act_re_deployment,act_re_model,act_re_procdef
    private final RepositoryService repositoryService;
    //流程的运行。主要操作表：act_ru...
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    //查询历史记录。主要操作表：act_hi...
    private final HistoryService historyService;
    //页面表单服务（较少使用）
    private final FormService formService;
    //对工作流的用户管理的表操作。主要操作表：act_id...
    private final IdentityService identityService;
    //管理器
    private final ManagementService managementService;

    @Autowired
    public ProcessUseController(RuntimeService runtimeService, TaskService taskService, RepositoryService repositoryService, FormService formService, HistoryService historyService, IdentityService identityService, ManagementService managementService) {
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.repositoryService = repositoryService;
        this.formService = formService;
        this.historyService = historyService;
        this.identityService = identityService;
        this.managementService = managementService;
    }

    //流程定义。当流程图被部署之后，查询出来的数据。仅为定义的流程，没有实际执行。
    ProcessDefinition processDefinition;
    //流程定义的执行实例。
    ProcessInstance processInstance;
    //描述流程执行的每一个节点。ProcessDefinition分支时，ProcessDefinition有一个ProcessInstance；有分支时则有多个
    Execution execution;
    //任务实例
    Task task;

    //启动流程
    // act_ru_execution     流程启动一次，只要没执行完，就会有数据
    @RequestMapping("startProcess")
    public Res startProcess(@RequestBody(required = false) JSONObject bJO) {
        String processDefKey = getStringOrDefaultFromJO(bJO, "processDefKey", "DemoProcess");
        String businessKey = getStringOrDefaultFromJO(bJO, "businessKey", "审批表单1");
//        runtimeService.startProcessInstanceById(processId);
        //参数1：流程定义id
        //参数2：Map<String,Object> 流程变量
//        runtimeService.startProcessInstanceById(processId,map );
        //参数1：流程定义id
        //参数2：String 业务id (可设置此id为业务单号)
//        runtimeService.startProcessInstanceById(processId,businessKey );
        //实际开发中常用一下方法
//        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processIdOrKey, businessKey);
        Map variables = new HashMap<String, Object>() {{
            put("请假天数", 2);
            put("请假时间", new Date());
        }};
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(processDefKey, businessKey, variables);
        return Res.success(processInstance.getBusinessKey(), "启动成功");
    }

    //查询任务
    // act_ru_task          启动流程中的任务，仅存放正在执行的任务，执行完的任务不在本表。
    // act_ru_identitylink  存放正在执行任务的，办理人信息
    @RequestMapping("searchTask")
    public Res searchTask(@RequestBody(required = false) JSONObject bJO) {
        String assignee = getStringOrDefaultFromJO(bJO, "assignee", "test1");
        List taskList = taskService.createTaskQuery().taskAssignee(assignee).list().stream().map(JOTool::taskToJSON).collect(Collectors.toList());
        return Res.success(taskList);
    }

    //办理任务
    @RequestMapping("completeTask")
    public Res completeTask(@RequestBody(required = false) JSONObject bJO) {
        String taskId = getStringOrDefaultFromJO(bJO, "taskId", null);
        if (taskId == null) {
            return Res.failure("taskId is null");
        }
        taskService.complete(taskId);
        return Res.success();
    }

    //判断流程是否完成
    @RequestMapping("processIsFinished")
    public Res processIsFinished(@RequestBody(required = false) JSONObject bJO) {
        String processInstanceId = getStringOrDefaultFromJO(bJO, "processInstanceId", null);
        if (processInstanceId == null) {
            return Res.failure("processInstanceId is null");
        }
        HistoricProcessInstance historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        if (historicProcessInstance == null) {
            return Res.failure("Process not exist");
        }
        return Res.success(new JSONObject() {{
            put("isFinished", historicProcessInstance.getEndTime() != null);
        }});
    }

    //查询历史任务
    @RequestMapping("searchHistoryTask")
    public Res searchHistoryTask(@RequestBody(required = false) JSONObject bJO) {
        String assignee = getStringOrDefaultFromJO(bJO, "assignee", "test1");
        List hisTaskList = historyService.createHistoricTaskInstanceQuery().taskAssignee(assignee).list().stream().map(JOTool::historyTaskToJSON).collect(Collectors.toList());
        return Res.success(hisTaskList);
    }
    //HistoryService
    //act_hi_procinst       历史流程实例。   启动一个流程，记录一条数据
    //act_hi_taskinst       历史任务实例。   完成一个任务，记录一条数据（仅任务节点会记录）
    //act_hi_actinst        历史任务实例。   每有一个节点被使用，记录一条数据（所有节点均记录）
    //act_hi_identitylink   历史任务实例。   每有一个用户进行操作，记录一条数据

    //操作变量
    @RequestMapping("setVariableViaExecution")
    public Res setVariableViaExecution(@RequestBody(required = false) JSONObject bJO) {
        String taskId = getStringOrDefaultFromJO(bJO, "taskId", null);
        String executionId = getStringOrDefaultFromJO(bJO, "executionId", null);
        String variable = getStringOrDefaultFromJO(bJO, "variable", null);
        Map<String, Object> variableMap = getMapOrDefaultFromJO(bJO, "variableMap", null);
        //无变量则新增，有变量则覆盖。变量版本号增加，
        if (taskId != null) {
            if (variable != null) {
                taskService.setVariable(taskId, "自定义变量task", variable);
            }
            if (variableMap != null) {
                taskService.setVariables(taskId, variableMap);
            }
        } else if (executionId != null) {
            if (variable != null) {
                runtimeService.setVariable(executionId, "自定义变量execution", variable);
            }
            if (variableMap != null) {
                runtimeService.setVariables(executionId, variableMap);
            }
        } else {
            return Res.failure("need taskId or executionId");
        }
        return Res.success();
    }

    //获取变量
    @RequestMapping("getVariable")
    public Res getVariable(@RequestBody(required = false) JSONObject bJO) {
        String taskId = getStringOrDefaultFromJO(bJO, "taskId", null);
        String executionId = getStringOrDefaultFromJO(bJO, "executionId", null);
        //无变量则新增，有变量则覆盖。变量版本号增加，
        Object variable;
        Object variables;
        if (taskId != null) {
            variable = taskService.getVariable(taskId, "自定义变量task");
            variables = taskService.getVariables(taskId);
        } else if (executionId != null) {
            variable = runtimeService.getVariable(executionId, "自定义变量execution");
            variables = runtimeService.getVariables(executionId);
        } else {
            return Res.failure("need taskId or executionId");
        }
        HashMap<String, Object> stringObjectHashMap = new HashMap<>();
        stringObjectHashMap.put("variable", variable);
        stringObjectHashMap.put("variables", variables);
        return Res.success(stringObjectHashMap);
    }

    //获取历史变量
    @RequestMapping("getHistoryVariable")
    public Res getHistoryVariable(@RequestBody(required = false) JSONObject bJO) {
        String taskId = getStringOrDefaultFromJO(bJO, "taskId", null);
        String executionId = getStringOrDefaultFromJO(bJO, "executionId", null);
        String processInstanceId = getStringOrDefaultFromJO(bJO, "processInstanceId", null);
        //无变量则新增，有变量则覆盖。变量版本号增加，
        HistoricVariableInstanceQuery historicVariableInstanceQuery = historyService.createHistoricVariableInstanceQuery();
        if (taskId != null) {
            historicVariableInstanceQuery.taskId(taskId);
        }
        if (executionId != null) {
            historicVariableInstanceQuery.executionId(executionId);
        }
        if (processInstanceId != null) {
            historicVariableInstanceQuery.processInstanceId(processInstanceId);
        }
        List<Map<String, Object>> collect = historicVariableInstanceQuery.list()
                .stream().map(JOTool::historicVariableInstanceToJSON).collect(Collectors.toList());
        return Res.success(collect);
    }
}