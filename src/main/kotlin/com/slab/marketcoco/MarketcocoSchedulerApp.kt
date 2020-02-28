package com.slab.marketcoco

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer

@SpringBootApplication
class MarketcocoSchedulerApp
class ServletInitializer : SpringBootServletInitializer() {
    override fun configure(application: SpringApplicationBuilder): SpringApplicationBuilder {
        return application.sources(MarketcocoSchedulerApp::class.java)
    }
}
fun main(args: Array<String>) {
    runApplication<MarketcocoSchedulerApp>(*args)
}

