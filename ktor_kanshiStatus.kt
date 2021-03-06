//import com.sun.xml.internal.ws.client.ContentNegotiation
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.DefaultHeaders
import io.ktor.features.ContentNegotiation
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.applicationEngineEnvironment
import io.ktor.server.engine.connector
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.gson.*
import io.ktor.request.receive
import io.ktor.routing.post
import java.text.DateFormat

data class kanshiStatusData(
        val hostname: String,
        val kanshiSystem: String,
        val status: String,
        val date: String,
        val lastSuppressedDate: String   // Unicenter only
)

fun main(args: Array<String>) {
    val env = applicationEngineEnvironment {
        module {
            main()
        }
        // Private API
        connector {
            host = "127.0.0.1"
            port = 9090
        }
        // Public API
        connector {
            host = "161.93.186.81" //"0.0.0.0"
            port = 8080
        }
    }
    embeddedServer(Netty, env).start(true)
}

fun Application.main() {

    install(ContentNegotiation) {
        gson {
            setDateFormat(DateFormat.LONG)
            setPrettyPrinting()
        }
    }

    routing {

        get("/") {
            if (call.request.local.port == 8080) {
                call.respondText("Connected to public api")
            } else {
                call.respondText("Connected to private api")
            }
        }

        // Unicenterに登録されている、全ての監視対象のホスト名一覧を、テキストで返す
        get("/Unicenter/AllTargetHostnames") {
            val UnicenterKanshi = UnicenterKanshiStatus()
            val AllHostnames: List<String> = UnicenterKanshi.getAllTargetHostnames()
            var respText: String = ""
            AllHostnames.forEach {
                respText += "${it}\n"
            }
            call.respondText(respText)
        }

        // HP監視　で、指定のhostnameの監視抑止状態を、JSONで返す
        get( "HP/KanshiStatus/{targetHostname}") {
            val targetHostname: String? = call.parameters["targetHostname"]
            val HPKanshi = HPKanshiStatus()
            val retval: HashMap<String, String>? = HPKanshi.CheckHostStatus(targetHostname!!)

            if ( retval != null) {
                val retvalGson = kanshiStatusData(
                        hostname = retval?.get("hostname") ?: "",
                        kanshiSystem = retval?.get("kanshiSystem") ?: "",
                        status = retval?.get("status") ?: "",
                        date = retval?.get("date") ?: "",
                        lastSuppressedDate = ""
                )
                call.respond(retvalGson)
            } else {
                println("Httpstatus : ${HttpStatusCode.NotFound}")
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // JP1監視　で、指定のhostnameの監視抑止状態を、JSONで返す
        get( "JP1/KanshiStatus/{targetHostname}") {
            val targetHostname: String? = call.parameters["targetHostname"]
            val JP1Kanshi = JP1KanshiStatus()
            val retval: HashMap<String, String>? = JP1Kanshi.CheckJP1HostStatus(targetHostname!!)

            if ( retval != null) {
                val retvalGson = kanshiStatusData(
                        hostname = retval?.get("hostname") ?: "",
                        kanshiSystem = retval?.get("kanshiSystem") ?: "",
                        status = retval?.get("status") ?: "",
                        date = retval?.get("date") ?: "",
                        lastSuppressedDate = ""
                )
                call.respond(retvalGson)
            } else {
                println("Httpstatus : ${HttpStatusCode.NotFound}")
                call.respond(HttpStatusCode.NotFound)
            }
        }

        // Unicenter監視　で、指定のhostnameの監視抑止状態を、JSONで返す
        get( "Unicenter/KanshiStatus/{targetHostname}") {
            val targetHostname: String? = call.parameters["targetHostname"]
            val UnicenterKanshi = UnicenterKanshiStatus()
            val retval: HashMap<String, String>? = UnicenterKanshi.CheckUnicenterHostStatus(targetHostname!!)

            if ( retval != null) {
                val retvalGson = kanshiStatusData(
                        hostname = retval?.get("hostname") ?: "",
                        kanshiSystem = retval?.get("kanshiSystem") ?: "",
                        status = retval?.get("status") ?: "",
                        date = retval?.get("date") ?: "",
                        lastSuppressedDate = retval?.get("lastSuppressedDate") ?: ""
                )
                call.respond(retvalGson)
            } else {
                println("Httpstatus : ${HttpStatusCode.NotFound}")
                call.respond(HttpStatusCode.NotFound)
            }
        }


        // 複数のホスト名の監視抑止状態を、HP,JP1,Unicenterの順に問い合わせて、結果一覧をJSON返す
        post("KanshiStatusEachHosts") {
            val targetHostnames: List<String>? = call.receive<List<String>>()
            println("size of hostnames: ${targetHostnames?.size ?:0}")
            if (targetHostnames?.size ?: 0 > 0) {

                val HPKanshi = HPKanshiStatus()
                val JP1Kanshi = JP1KanshiStatus()
                val UnicenterKanshi = UnicenterKanshiStatus()

                var retvalGsonList: ArrayList<kanshiStatusData> = ArrayList()

                targetHostnames?.forEach {
                    val targethostname: String = it

                    // check HP
                    val retval: HashMap<String,String>? = HPKanshi.CheckHostStatus(targethostname)
                    if ((retval != null) and (retval?.get("kanshiSystem") == "HP")) {
                        retvalGsonList.add(kanshiStatusData(
                                hostname = retval?.get("hostname") ?: "",
                                kanshiSystem = retval?.get("kanshiSystem") ?: "",
                                status = retval?.get("status") ?: "",
                                date = retval?.get("date") ?: "",
                                lastSuppressedDate = ""
                        ))
                    } else {
                        // check JP1
                        val retval2: HashMap<String,String>? = JP1Kanshi.CheckJP1HostStatus(targethostname)
                        if ((retval2 != null) and (retval2?.get("kanshiSystem") == "JP1")) {
                            retvalGsonList.add(kanshiStatusData(
                                    hostname = retval2?.get("hostname") ?: "",
                                    kanshiSystem = retval2?.get("kanshiSystem") ?: "",
                                    status = retval2?.get("status") ?: "",
                                    date = retval2?.get("date") ?: "",
                                    lastSuppressedDate = ""
                            ))
                        } else {
                            // check Unicenter
                            val retval3: HashMap<String,String>? = UnicenterKanshi.CheckUnicenterHostStatus(targethostname)
                            if ((retval3 != null) and (retval3?.get("kanshiSystem") == "Unicenter")) {
                                retvalGsonList.add(kanshiStatusData(
                                        hostname = retval3?.get("hostname") ?: "",
                                        kanshiSystem = retval3?.get("kanshiSystem") ?: "",
                                        status = retval3?.get("status") ?: "",
                                        date = retval3?.get("date") ?: "",
                                        lastSuppressedDate = retval3?.get("lastSuppressedDate") ?: ""
                                ))
                            } else {
                                println("${targethostname} は、HP,JP1,Unicenter いずれの監視もしていません。")
                                retvalGsonList.add(kanshiStatusData(
                                        hostname = retval3?.get("hostname") ?: "",
                                        kanshiSystem = retval3?.get("kanshiSystem") ?: "",
                                        status = retval3?.get("status") ?: "",
                                        date = retval3?.get("date") ?: "",
                                        lastSuppressedDate = retval3?.get("lastSuppressedDate") ?: ""
                                ))
                            }
                        }
                    }
                } // end of forEach

                call.respond(retvalGsonList ?: emptyList<kanshiStatusData>() )

            } else {
                println("指定の対象ホスト名が０個以下です。")
                call.respondText("指定の対象ホスト名が０個以下です。")
            }
        }





    } // end of routing
}

