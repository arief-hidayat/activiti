package org.grails.activiti.test

import grails.plugin.greenmail.GreenMail
import org.activiti.engine.HistoryService
import org.activiti.engine.RuntimeService
import org.activiti.engine.TaskService
import org.activiti.engine.runtime.ProcessInstance
import org.activiti.engine.task.Task
import spock.lang.Specification

class HiringDeveloperSpec extends Specification {
    RuntimeService runtimeService

    GreenMail greenMail
    TaskService taskService;
    HistoryService historyService;

    def "test happy path"() {
        given:
        Applicant applicant = new Applicant(name:  "John Doe", email: "john@activiti.org", phoneNumber: "12344");
        applicant.save(failOnError: true, flush: true)
        when:
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("hireProcessWithJpa", [applicant : applicant]);

        then:
        Task task = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .taskCandidateGroup("dev-managers")
                .singleResult();
        task.name == "Telephone interview"

        when:
        taskService.complete(task.getId(), [telephoneInterviewOutcome : true]);

        then:
        List<Task> tasks = taskService.createTaskQuery()
                .processInstanceId(processInstance.getId())
                .orderByTaskName().asc()
                .list()
        tasks.size() == 2
        tasks.get(0).name == "Financial negotiation"
        tasks.get(1).name == "Tech interview"

        when:
        taskService.complete(tasks.get(0).id, [techOk : true])
        taskService.complete(tasks.get(1).id, [financialOk : true])

        then:
        greenMail.messagesCount == 1
        and:
        historyService.createHistoricProcessInstanceQuery().finished().count() == 1
    }

}