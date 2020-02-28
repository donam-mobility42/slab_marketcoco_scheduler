package com.slab.marketcoco

import com.bsidesoft.i18n.bsLanguage
import com.bsidesoft.translate.bsTranslateGoogle
import ein.core.value.eJsonObject
import ein.core.value.eString
import ein.core.value.eValue
import ein.spring.queue.eMySQLQueue
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.concurrent.atomic.AtomicBoolean

object Scheduler{
    fun run(){
        StdSchedulerFactory.getDefaultScheduler().run{
            this.start()
            scheduleJob(
                    JobBuilder.newJob(GoogleTranslateJob::class.java)
                            //.withIdentity("myJob", "group1")
                            //.usingJobData("cnt", 1)
                            .build(),
                    TriggerBuilder.newTrigger()
                            .startNow()
                            .withSchedule(
                                    SimpleScheduleBuilder.simpleSchedule()
                                            .withIntervalInSeconds(3)
                                            .repeatForever()
                            )
                            .build()
            )
        }
    }
}

class GoogleTranslateJob: Job {
    companion object{
        private val trans by lazy {
            bsTranslateGoogle("AIzaSyDqwslRjN3Lwv1hP7Wc6LLNxp110h3IWXw")
        }
        private val isRunning = AtomicBoolean(false)
    }
    override fun execute(context: JobExecutionContext) {
        if(isRunning.compareAndSet(true, true)){
            println("이미 작업중이라 쉬어감!")
            return
        }
        val q = eMySQLQueue("base")
        q.getList()?.forEach { (k, v)->
            (eValue.json(v) as? eJsonObject)?.let{src->
                println("번역대상 : $k = ${src.stringify()}")
                val from = bsLanguage[src.s("from")]
                val to = bsLanguage[src.s("to")]
                val args = src.o("data").map {(k,v)->
                    k to v.v as String
                }.toTypedArray()
                try{
                    val result = trans.get(from, to, *args).let{data->
                        eJsonObject().also {
                            it["from"] = eString(from.code)
                            it["to"] = eString(to.code)
                            it["data"] = eValue(data)
                        }
                    }.stringify()
                    q[k] = result
                }catch (e:Throwable){
                    println("번역실패 k=$k msg = ${e.message}")
                }
            }
        }?:let{
            println("번역할 내용이 없음")
        }
        isRunning.set(false)
    }
}