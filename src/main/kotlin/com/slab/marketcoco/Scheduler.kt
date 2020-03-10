package com.slab.marketcoco

import com.bsidesoft.i18n.bsLanguage
import com.bsidesoft.translate.bsTranslateGoogle
import ein.core.resource.eLoader
import ein.core.sql.eQuery
import ein.core.value.eJsonObject
import ein.core.value.eValue
import ein.spring.sql.eMySQL
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import java.util.concurrent.Executors

object Scheduler{
    operator fun invoke(){
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
    companion object: eLoader {
        private var wait = true
        private const val threadPoolCnt = 5
        private val executor  = Executors.newFixedThreadPool(threadPoolCnt)
        private lateinit var trans:bsTranslateGoogle
        private lateinit var dbKey:String
        override fun load(res: eJsonObject) {
            (res["googleTranslateJob"] as? eJsonObject)?.run {
                dbKey = s("dbKey")
                trans = bsTranslateGoogle(s("transKey"))
                eQuery["inqueue/list"] = {
                    eQuery("""
                        select $threadPoolCnt-(select count(*)from inqueue where state=1),inqueue_rowid r,k from inqueue where state=0
                        limit $threadPoolCnt
                    """)
                }
                eQuery["inqueue/set/reading"] = {
                    eQuery("update inqueue set state=1 where inqueue_rowid=@r:long@")
                }
                eQuery["inqueue/detail/list"] = {
                    eQuery("select k,v from inqueuedetail where inqueue_rowid=@r:long@")
                }
                eQuery["gt/rowid"] = {
                    eQuery("select gt_rowid from gt where k=@k:string@")
                }
                eQuery["gt/add1"] = {
                    eQuery("insert into gt(k,regdate)values(@k:string@,utc_timestamp())on duplicate key update regdate=values(regdate);select last_insert_id()")
                }
                eQuery["gt/add2"] = {
                    eQuery("insert into gtdetail(gt_rowid,k,v,cdata,inv)values(@gt_rowid:long@,@k:string@,@v:string@,@cdata:string@,@inv:string@)on duplicate key update v=values(v),cdata=values(cdata),inv=values(inv)")
                }
                eQuery["inqueue/delete"] = {
                    eQuery("""
                        delete from inqueuedetail where inqueue_rowid=(select inqueue_rowid from inqueue where k=@k:string@)
                        ;delete from inqueue where k=@k:string@
                    """)
                }
            }
        }
    }
    override fun execute(context: JobExecutionContext) {
        inqueueList()?.forEach {(r, k)->
            println("번역요청 실시 inqueue key = $k")
            executor.execute {
                gtAdd(k, translate(r))
                wait = true
            }
        }?:let{
            if(wait) println("번역 대기중")
            wait = false
        }
    }
    private val db get() = eMySQL(dbKey)
    private fun inqueueList():List<Pair<Long,String>>?{
        var list:List<Pair<Long,String>>? = null
        db.tx {
            query("inqueue/list").run{
                second?.run{
                    val limit = this[0][0] as Long
                    list = (if(limit < size.toLong()) take(limit.toInt()) else asList()).map {
                        val r = "${it[1]}".toLong()
                        queryThrow("inqueue/set/reading", "r" to r)
                        r to "${it[2]}"
                    }
                    if(list?.isEmpty()?:true) println("inqueue에 아직 처리하는 작업(state=1)이 ${threadPoolCnt}개 이상 남아 있어서 작업수행할 수 없음")
                }
            }
        }
        return list
    }
    private fun inqueueDetailList(r:Long) = db.query("inqueue/detail/list", "r" to r).second?.map{
        "${it[0]}" to (eValue.json("${it[1]}") as eJsonObject)
    }
    private fun translate(r:Long):List<MutableList<Pair<String,Any>>>{
        val details = mutableListOf<MutableList<Pair<String,Any>>>()
        inqueueDetailList(r)?.groupBy {
            it.second.s("from") + it.second.s("to")
        }?.forEach {from, list->
            val from = list[0].second.s("from")
            val to = list[0].second.s("to")
            val origin = list.toMap()
            trans.get(bsLanguage[from], bsLanguage[to], *list.map{ (k,v)->k to v.s("text")}.toTypedArray()).forEach { (key, value) ->
                details.add(mutableListOf("k" to key, "v" to value, "cdata" to cdata(value, to), "inv" to (origin[key]?.stringify()?: "")))
            }
        }
        return details
    }
    private fun cdata(v:String, ln:String) = eJsonObject().also {
        it["cdata"] = eJsonObject().also { data->
            data["@ln"] = eJsonObject().also {cat->
                cat[ln] = eValue(v)
            }
        }
    }.stringify()
    private fun gtAdd(k:String, details:List<MutableList<Pair<String,Any>>>){
        db.tx {
            var gt_rowid = l("gt/rowid", 0L, "k" to k)
            if(gt_rowid == 0L) gt_rowid = l("gt/add1", 0L, "k" to k)
            if(gt_rowid != 0L) details.forEach {
                it += "gt_rowid" to gt_rowid
                queryThrow("gt/add2", *it.toTypedArray())
            }
            queryThrow("inqueue/delete", "k" to k)
        }
    }
}
/*
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
                    |select k from inqueue where state=0
                    |and 0<(select (select count(*)from $IN_TABLE where state=0)-(select count(*)from $IN_TABLE where state=1)from dual)
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
}*/