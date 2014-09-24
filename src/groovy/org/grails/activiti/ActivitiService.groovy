/* Copyright 2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.grails.activiti

import org.activiti.engine.EngineServices
import org.activiti.engine.FormService
import org.activiti.engine.HistoryService
import org.activiti.engine.IdentityService
import org.activiti.engine.ManagementService
import org.activiti.engine.RepositoryService
import org.activiti.engine.RuntimeService
import org.activiti.engine.TaskService
import org.activiti.engine.runtime.ProcessInstance
import org.activiti.engine.task.Task
import grails.util.GrailsNameUtils
import grails.util.Holders
import org.activiti.engine.task.TaskQuery
import org.activiti.spring.SpringProcessEngineConfiguration

/**
 *
 * @author <a href='mailto:limcheekin@vobject.com'>Lim Chee Kin</a>
 *
 * @since 5.0.beta2
 */
class ActivitiService implements EngineServices {
    SpringProcessEngineConfiguration processEngineConfiguration
	RuntimeService runtimeService
	TaskService taskService
	IdentityService identityService
	FormService formService
    RepositoryService repositoryService
    HistoryService historyService
    ManagementService managementService
	String sessionUsernameKey = Holders.config.activiti.sessionUsernameKey?:ActivitiConstants.DEFAULT_SESSION_USERNAME_KEY
	String usernamePropertyName = Holders.config.grails.plugins.springsecurity.userLookup.usernamePropertyName

    ProcessInstance startProcess(Map params) {
		if (params.businessKey) {
		  runtimeService.startProcessInstanceByKey((String) params.controller, (String) params.businessKey, params)
		} else {
		  runtimeService.startProcessInstanceByKey(params.controller, params)
		}
	}
	
	private List<Task> findTasks(String methodName, String username, int firstResult, int maxResults, Map orderBy) {
		TaskQuery taskQuery = taskService.createTaskQuery()
		if (methodName) {
			taskQuery."${methodName}"(username)
		}
		if (orderBy) {
			orderBy.each { k, v ->
				taskQuery."orderByTask${GrailsNameUtils.getClassNameRepresentation(k)}"()."${v}"()
			}
		}			
		taskQuery.listPage(firstResult, maxResults)
	}
	
	private Long getTasksCount(String methodName, String username) {
        TaskQuery taskQuery = taskService.createTaskQuery()
		if (methodName) {
			taskQuery."${methodName}"(username)
		}
		taskQuery.count()
	}

    Long getAssignedTasksCount(String username) {
		getTasksCount("taskAssignee", username)
	}

    Long getUnassignedTasksCount(String username) {
		getTasksCount("taskCandidateUser", username)
	}

    Long getAllTasksCount() {
		getTasksCount(null, null)
	}

    List<Task> findAssignedTasks(Map params) {
		def orderBy = Holders.config.activiti.assignedTasksOrderBy?:[:]
        if (!params.sort) {
            params.sort = "createTime"
            params.order = "desc"
        }
		if (params.sort) {
			orderBy << ["${params.sort}":params.order]
		}
        if(!params[sessionUsernameKey])
            throw new RuntimeException("Expecting ${sessionUsernameKey} from parameter.")
		findTasks("taskAssignee", params[sessionUsernameKey], getOffset(params), getMax(params), orderBy)
	}

    List<Task> findUnassignedTasks(Map params) {
		def orderBy = Holders.config.activiti.unassignedTasksOrderBy?:[:]
        if (!params.sort) {
            params.sort = "createTime"
            params.order = "desc"
        }
		if (params.sort) {
			orderBy << ["${params.sort}":params.order]
		}
        if(!params[sessionUsernameKey])
            throw new RuntimeException("Expecting ${sessionUsernameKey} from parameter.")
		findTasks("taskCandidateUser", params[sessionUsernameKey], getOffset(params), getMax(params), orderBy)
	}

    List<Task> findAllTasks(Map params) {
		def orderBy = Holders.config.activiti.allTasksOrderBy?:[:]
        if (!params.sort) {
            params.sort = "createTime"
            params.order = "desc"
        }
		if (params.sort) {
			orderBy << ["${params.sort}":params.order]
		}
		findTasks(null, null, getOffset(params), getMax(params), orderBy)
	}
	
	private int getOffset(def params) {
		return params.offset? (params.offset instanceof String ? Integer.parseInt(params.offset) : params.offset) : 0
	}
    private int getMax(def params) {
        return params.max ? (params.max instanceof String ? Integer.parseInt(params.max) : params.max) : 10
    }
	
	String deleteTask(String taskId, String domainClassName = null) {
		String id = deleteDomainObject(taskId, domainClassName)
		taskService.deleteTask(taskId)
		return id
	}			  
	
	private def deleteDomainObject(String taskId, String domainClassName) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult()
		def id = getDomainObjectId(task)
		if (id) {
            String domainClzNm = domainClassName?:getDomainClassName(task),
                    findById = Holders.config.grails?.domain?.retrieveByIdMethod?."${domainClzNm.replaceAll("[.]". "?.")}" ?: "get"
			def domainClass = Holders.grailsApplication.classLoader.loadClass(domainClzNm)
			domainClass."$findById"(id)?.delete(flush: true)    //TODO: how if the domain class doesn't have get(id) method
		}
		return id
	}
	
	private def getDomainObjectId(Task task) {
		runtimeService.getVariable(task.executionId, "id")
	}
	
	private def getDomainClassName(Task task) {
		runtimeService.getVariable(task.executionId, "domainClassName")
	}	   
	
	Task getAssignedTask(String username, String processInstanceId) {
		getTask("taskAssignee", username, processInstanceId)
	}
	
	Task getUnassignedTask(String username, String processInstanceId) {
		getTask("taskCandidateUser", username, processInstanceId)
	}	
	
	private getTask(String methodName, String username, String processInstanceId) {
		taskService.createTaskQuery()
		.processInstanceId(processInstanceId)
		."${methodName}"(username)
		.singleResult()
	}
	
	void claimTask(String taskId, String username) {
		taskService.claim(taskId, username)
	}		
	
	void completeTask(String taskId, Map params) {
		String executionId = taskService.createTaskQuery().taskId(taskId).singleResult().executionId
		setIdAndDomainClassName(executionId, params)
		runtimeService.setVariable(executionId, "uri", null)
		taskService.complete(taskId, params)
	}
	
	private void setIdAndDomainClassName(String executionId, Map params) {
		if (params.id) {
			runtimeService.setVariable(executionId, "id", params.id)
			runtimeService.setVariable(executionId, "domainClassName", params.domainClassName as String)
		}
	}
	
	void setTaskFormUri(Map params) {
		String executionId = taskService.createTaskQuery().taskId((String)params.taskId).singleResult().executionId
		setIdAndDomainClassName(executionId, params)
		if (params.controller && params.action && params.id) {
			runtimeService.setVariable(executionId, "uri", "/${params.controller}/${params.action}/${params.id}")
		}
	}						
	
	String getTaskFormUri(String taskId, boolean useFormKey) {
		Task task = taskService.createTaskQuery().taskId(taskId).singleResult()
		String taskFormUri = runtimeService.getVariable(task.executionId, "uri")
		if (!taskFormUri) {
			def id = getDomainObjectId(task)?:"" 
			
			if (useFormKey) {
				String formKey = formService.getTaskFormData(taskId).formKey
				if (formKey) {
			    taskFormUri = "${formKey}/${id}"
				} else {
				  taskFormUri = "/${runtimeService.getVariable(task.executionId, 'controller')}/${task.taskDefinitionKey}/${id}"
				}
			} else {
			  taskFormUri = "/${runtimeService.getVariable(task.executionId, 'controller')}/${task.taskDefinitionKey}/${id}"
			}
		}
		if (taskFormUri) {
			taskFormUri += "?taskId=${taskId}"
		}
		return taskFormUri
	}
	
	void setAssignee(String taskId, String username) {
		taskService.setAssignee(taskId, username)
	}
	
	void setPriority(String taskId, int priority) {
		taskService.setPriority(taskId, priority)
	}
	
	def getCandidateUserIds(String taskId) {
		def identityLinks = taskService.getIdentityLinksForTask(taskId)
		def userIds = []
		def users
		identityLinks.each { identityLink -> 
			if (identityLink.groupId) {
				users = identityService.createUserQuery()
					      .memberOfGroup(identityLink.groupId)
								.orderByUserId().asc().list()
				if (Holders.applicationContext.pluginManager.hasGrailsPlugin('activitiSpringSecurity')) {
			    userIds << users?.collect { it."$usernamePropertyName" }
				} else { 
				  userIds << users?.collect { it.id }
				}
			} else {
			  userIds << identityLink.userId
			}
		}  		
		return userIds.flatten().unique()
	}

}