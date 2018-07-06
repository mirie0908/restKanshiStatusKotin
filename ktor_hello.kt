import io.ktor.application.*
import io.ktor.http.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken

fun main(args: Array<String>) {
    val server = embeddedServer(Netty, port = 8080) {
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
                val targetHostname = call.parameters["targetHostname"]
                val HPKanshi = HPKanshiStatus()
                val retval: HashMap<String, String>? = HPKanshi.CheckHostStatus(targetHostname)
            }




        }
    }
    server.start(wait = true)
}
