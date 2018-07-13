import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import io.ktor.features.ContentNegotiation
import io.ktor.gson.gson
import io.ktor.request.*
import java.text.DateFormat

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {

        install(ContentNegotiation) {
            gson {
                setDateFormat(DateFormat.LONG)
                setPrettyPrinting()
            }
        }

        routing {

            get("/") {
                call.respondText("Hello World!", ContentType.Text.Plain)
            }
            get("/demo") {
                call.respondText("HELLO WORLD!")
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
            }

            // post request test
            post("/posttest") {

                println("requested content type: ${call.request.contentType().toString()}")

                val requesteddata: List<String>? = call.receive<List<String>>()       //.request.queryString()

                println("size of requested data: ${requesteddata?.size ?:0}")

                call.respondText("requested string: ${requesteddata?.firstOrNull()}")


            }




        }
    }
    server.start(wait = true)
}
