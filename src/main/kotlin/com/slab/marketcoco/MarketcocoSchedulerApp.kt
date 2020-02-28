package com.slab.marketcoco

import org.quartz.Job
import org.quartz.JobBuilder.newJob
import org.quartz.JobExecutionContext
import org.quartz.SimpleScheduleBuilder.simpleSchedule
import org.quartz.TriggerBuilder.newTrigger
import org.quartz.impl.StdSchedulerFactory
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class MarketcocoSchedulerApp: InitializingBean, CommandLineRunner {
    private val logger = LoggerFactory.getLogger(MarketcocoSchedulerApp::class.java)
    override fun afterPropertiesSet() {
        logger.info("afterPropertiesSet")

    }
    override fun run(vararg args: String?) {
        logger.info("start")
        StdSchedulerFactory.getDefaultScheduler().run{
            this.start()
            scheduleJob(
                    newJob(GoogleTranslateJob::class.java)
                            .withIdentity("myJob", "group1")
                            .usingJobData("cnt", 1)
                            .build(),
                    newTrigger()
                            .startNow()
                            .withSchedule(
                                    simpleSchedule()
                                            .withIntervalInSeconds(3)
                                            .repeatForever()
                            )
                            .build()
            )
        }
    }
}
class ServletInitializer : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(MarketcocoSchedulerApp::class.java)
    }
}
fun main(args: Array<String>) {
    runApplication<MarketcocoSchedulerApp>(*args)
}

class GoogleTranslateJob:Job{
    override fun execute(context: JobExecutionContext?) {
        println("테스트")
    }
}
