package com.slab.marketcoco

import com.bsidesoft.i18n.bsLanguage
import com.bsidesoft.translate.bsTranslateGoogle
import ein.core.resource.eLoader
import ein.core.sql.eQuery
import ein.core.value.eJsonObject
import ein.core.value.eString
import ein.core.value.eValue
import ein.jvm.crypto.eCrypto
import ein.jvm.queue.eQueue
import ein.spring.sql.eMySQL
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.concurrent.Executors

object Scheduler{
    fun run(){
        StdSchedulerFactory.getDefaultScheduler().run{
            start()
            scheduleJob(
                JobBuilder.newJob(GoogleTranslateJob::class.java)
                        //.withIdentity("myJob", "group1")
                        //.usingJobData("cnt", 1)
                        .build(),
                TriggerBuilder.newTrigger()
                        .startNow()
                        .withSchedule(
                                SimpleScheduleBuilder.simpleSchedule()
                                        .withIntervalInMilliseconds(500)
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
        const val threadPoolCnt = 5
        private val executor  = Executors.newFixedThreadPool(threadPoolCnt)
    }
    override fun execute(context: JobExecutionContext) {
        val q = GoogleTranslateQueue
        q.getReservedList()?.forEach { (k, v)->
            (eValue.json(v) as? eJsonObject)?.let{src->
                executor.execute {
                    println("번역대상 : $k = ${src.stringify()}")
                    val from = bsLanguage[src.s("from")]
                    val to = bsLanguage[src.s("to")]
                    val args = src.o("data").map {(k,v)->
                        k to v.v as String
                    }.toTypedArray()
                    try{
                        q.setResult(k, trans.get(from, to, *args).let{data->
                            eJsonObject().also {
                                it["from"] = eString(from.code)
                                it["to"] = eString(to.code)
                                it["data"] = eValue(data)
                            }
                        }.stringify())
                    }catch (e:Throwable){
                        println("번역실패 k=$k msg = ${e.message}")
                    }
                }
            }
        }?:let{
            println("번역할 내용이 없음")
        }
    }
}

object GoogleTranslateQueue: eQueue<String, String, String>(), eLoader {
    private const val IN_TABLE = "inqueue"
    private const val OUT_TABLE = "outqueue"
    private const val LIST_OF_RESERVED = "@queue/list/reserved"
    private const val RESERVE_READ = "@queue/update/reserved"
    private const val IS_VALID_KEY = "@queue/isvalidkey"
    private const val SET_RESULT = "@queue/set/result"
    private const val GET_RESULT = "@queue/get/result"
    private lateinit var dbKey:String
    override fun load(res: eJsonObject) {
        (res["googleTranslate"] as? eJsonObject)?.run{
            dbKey = s("dbKey")
            val db = eMySQL(dbKey)
            eQuery[LIST_OF_RESERVED] = {
                eQuery("""
                    |select k,v from $IN_TABLE where isread=0 
                    |and 0<(select (select count(*)from $IN_TABLE where isread=0)-(select count(*)from $IN_TABLE where isread=1)from dual) 
                    |limit ${GoogleTranslateJob.threadPoolCnt}
                """.trimMargin())
            }
            eQuery[RESERVE_READ] = {
                eQuery("update $IN_TABLE set isread=1 where k=@k:string@")
            }
            eQuery[IS_VALID_KEY] = {
                eQuery("""
                    |select if(sum(cnt)=0,0,1)from(
                        |select count(*)cnt from $IN_TABLE where k=@k:string@
                        |union
                        |select count(*)cnt from $OUT_TABLE where k=@k:string@
                    |)a
                """.trimMargin())
            }
            eQuery[SET_RESULT] = {
                eQuery("""
                    |delete from $IN_TABLE where k=@k1:string@
                    |;insert into $OUT_TABLE(k,v)values(@k2:trim|minlength[1]@,@v:string@)on duplicate key update v=values(v)
                """.trimMargin())
            }
            eQuery[GET_RESULT] = {
                eQuery("select v from $OUT_TABLE where k=@k:string@")
            }
        }
    }
    override fun existKey(value: String) = null
    override fun getReservedList():List<Pair<String,String>>? {
        var list:List<Pair<String,String>>? = null
        eMySQL(dbKey).tx {
            query(LIST_OF_RESERVED).run{
                list = second?.map {
                    queryThrow(RESERVE_READ, "k" to it[0])
                    "${it[0]}" to "${it[1]}"
                }
            }
        }
        return list
    }
    override fun setResult(key: String, v: String) {
        eMySQL(dbKey).query(SET_RESULT, "k1" to key, "k2" to key, "v" to v)
    }
    override fun keyGenerate(channel: String, value: String) = eCrypto.md5(value) + ":${eString.oneline(channel)}"
    override fun getChannel(key:String) = key.split(":")[1]
    override fun releaseKey(key: String) {}
    override fun releaseData(key: String) {}
    override fun reserve(key: String, value: String) = false
    override fun isValidKey(key: String) = eMySQL(dbKey).i(IS_VALID_KEY, 0,"k" to key) != 0
    override fun getResult(key: String) = eMySQL(dbKey).s(GET_RESULT, "", "k" to key).let {
        if(it.isBlank()) null else it
    }
}