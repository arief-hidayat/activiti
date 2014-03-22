grails.project.class.dir = "target/classes"
grails.project.test.class.dir = "target/test-classes"
grails.project.test.reports.dir = "target/test-reports"

grails.project.fork = [
        // configure settings for compilation JVM, note that if you alter the Groovy version forked compilation is required
        //  compile: [maxMemory: 256, minMemory: 64, debug: false, maxPerm: 256, daemon:true],

        // configure settings for the test-app JVM, uses the daemon by default
        test: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, daemon:true],
        // configure settings for the run-app JVM
        run: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
        // configure settings for the run-war JVM
        war: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256, forkReserve:false],
        // configure settings for the Console UI JVM
        console: [maxMemory: 768, minMemory: 64, debug: false, maxPerm: 256]
]

grails.project.dependency.resolver = "maven" // or ivy
grails.project.dependency.resolution = {
    // inherit Grails' default dependencies
    inherits("global") {
        // uncomment to disable ehcache
        // excludes 'ehcache'
    }
    log "warn" // log level of Ivy resolver, either 'error', 'warn', 'info', 'debug' or 'verbose'
    repositories {
        grailsCentral()
        mavenLocal()
        mavenCentral()
        // uncomment the below to enable remote dependency resolution
        // from public Maven repositories
        //mavenRepo "http://repository.codehaus.org"
        //mavenRepo "http://download.java.net/maven/2/"
        //mavenRepo "http://repository.jboss.com/maven2/"
        mavenRepo name: "Activiti", root: "https://maven.alfresco.com/nexus/content/groups/public"
    }
    dependencies {
        // specify dependencies here under either 'build', 'compile', 'runtime', 'test' or 'provided' scopes eg.
        // runtime 'mysql:mysql-connector-java:5.1.27'
        compile ('org.activiti:activiti-engine:5.15') {
            excludes 'livetribe-jsr223', 'spring-beans'
        }
        runtime ('org.activiti:activiti-spring:5.15') {
            excludes 'spring-context', 'spring-jdbc', 'spring-orm', 'slf4j-log4j12', 'commons-dbcp'
        }
        //runtime 'org.springframework:spring-asm:3.1.4.RELEASE'
        runtime 'javax.mail:mail:1.4.1'
        test ('org.subethamail:subethasmtp-smtp:1.2') {
            excludes 'commons-logging'
        }
        test ('org.subethamail:subethasmtp-wiser:1.2') {
            excludes 'commons-logging'
        }
    }

    plugins {
        compile ":scaffolding:2.0.3"
        build(":release:3.0.1",
                ":rest-client-builder:1.0.3") {
            export = false
        }
        build ':tomcat:7.0.52.1'
        runtime ':hibernate:3.6.10.10'
    }
}
