package org.grails.activiti.flow.variable

import org.activiti.engine.RepositoryService
import org.activiti.engine.RuntimeService
import org.grails.activiti.test.FakeFlowVariable
import spock.lang.Specification


/**
 * Created by hida on 31/08/15.
 */
class VariableSerializationSpec extends Specification {
    RuntimeService runtimeService
    RepositoryService repositoryService


    def "test serialization"() {
        given:
        repositoryService.createDeployment()
                .addClasspathResource("org/grails/activiti/flow/variable/SampleProcess.bpmn20.xml")
                .name("sampleProcess")
                .deploy()
        when:
        // start process

        def process = runtimeService.startProcessInstanceByKey("sampleProcess")
        and:
        // check flow variables
        runtimeService.setVariable(process.processInstanceId, "var1", new FakeFlowVariable(name: 'Matt', age: 21))

        then:
        def flowVariable = (FakeFlowVariable) runtimeService.getVariable(process.processInstanceId, "var1")
        flowVariable.name == "Matt"
        and:
        // kill process
        runtimeService.deleteProcessInstance(process.id, "Test is completed")
    }

}