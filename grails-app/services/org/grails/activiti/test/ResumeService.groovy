package org.grails.activiti.test

import grails.transaction.Transactional

@Transactional
class ResumeService {

    public void storeResume() {
        System.out.println("Storing resume ...");
    }
}
