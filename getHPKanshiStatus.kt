import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Attributes
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.Reader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.DocumentBuilder
import org.w3c.dom.*
import java.io.File
import java.io.InputStream
import javax.print.attribute.IntegerSyntax
import java.util.regex.Pattern
import java.net.URLEncoder






class HPKanshiStatus(val url: String = "http://161.95.212.225:30000/HostSearch/") {



    /*
     * HP監視Status問い合わせ用URLにrequestをするために必要なパラメータを予め取得する
     */
    fun getReqParam(): String? {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        val request: Request = Request.Builder().url(url).build()
        val response : Response = client.newCall(request).execute()
        if (response.isSuccessful) {
            /*
            // java.io.Reader クラス　に読み出す
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = "${lineStr}\n"
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += "${lineStr}\n"
            }
            return strRes
*/
            // 読み込んだものを、javax.xml.parsers.DocumentBuilder に渡してXmLをパースするには、
            // java.io.BufferedReader で読み込んで渡すのではなく、
            // java.io.InputStreamReader で読み込んで、Streamを引数に渡して、xml parse する必要がある。
            //val inpStream: InputStream = response.body()?.byteStream()
            val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.body()?.byteStream())

            xmlDoc.documentElement.normalize()

            // <input>タグの要素だけを取り出す
            val ndlist: NodeList =  xmlDoc.documentElement.getElementsByTagName("input")

            // <input>タグの要素中、所定の３つのIDのタグだけをとりだし、そのkey=valueを"&"で連結し、URLエンコードした文字列にする
            var paramStr: String? = ""

            if ( ndlist.length > 0 ) {

                var param_table: HashMap<String,String> = HashMap()

                val regexIds = """(__VIEWSTATE|__VIEWSTATEGENERATOR|__EVENTVALIDATION)""".toRegex()

                (0..(ndlist.length - 1)).iterator().forEach {
                    if ( regexIds.matches( ndlist.item(it).attributes.getNamedItem("id").nodeValue ) ) {
                        param_table.put(
                                key = ndlist.item(it).attributes.getNamedItem("id").nodeValue,       // .nodeValueをつけないと、id="xxxx"の文字列で取り出される
                                value = ndlist.item(it).attributes.getNamedItem("value").nodeValue
                        )
                    } else {
                        println("no match.")
                    }
                }


                param_table.forEach {
                    //if ( paramStr != "") { paramStr += "&" }
                    paramStr += "${it.key}=${URLEncoder.encode(it.value)}&"
                }

                response.close()  // don't forget
                return paramStr

            } else {
                println("nodelist count less than 0")
                response.close()
                return null
            }


        } else {
            response.close()  // don't forget
            //return "response error"
            return null
        }
    }



    /*
 　　* HP監視 指定のhostnameの　監視状態を取得する
 　　*/
    fun CheckHostStatus(theHostname: String): HashMap<String,String>? {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        // hostnameがFQDNだったら短縮名を取り出す
        val targethostname = if ( """\.""".toRegex().matches(theHostname)) {
            theHostname.split('.')[0]
        } else {
            theHostname
        }

        // HP監視URLにリクエストするためのパラメータの生成
        val getReqParam: String? = this.getReqParam()
        var bodyParam: String = ""
        if ( getReqParam != null) {
            bodyParam = "${getReqParam}txtHostname=${targethostname}&btnShow=%E8%A1%A8%E7%A4%BA"
        } else {
            println("cannot getReqParam")
            return null
        }

        // HP監視URLにリクエストし、監視抑止状態を取得
        val request: Request = Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), bodyParam))
                //.post(RequestBody.create(MediaType.parse("text/plain;  charset=utf-8"), bodyParam))
                //.post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"), bodyParam))
                .build()

        val response : Response = client.newCall(request).execute()

        // HTMLから全文文字列として取得し、
        // これをHTMLとしてパースし、監視抑止状態のテキストを取り出す
        if (response.isSuccessful) {
            println("the url post call successful.")

            // HTMLから全文を文字列として取得
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = "${lineStr}\n"
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += lineStr
                strRes += "\n"
            }
            //println("strRes : ${strRes}")

            // test file 出録
            //File("/home/share/test.html").bufferedWriter().use{out -> out.write(strRes)}

            // 取得した文字列を、jsoupでHTMLとしてパース
            val htmldoc: org.jsoup.nodes.Document = Jsoup.parse(strRes)

            // パースしたHTMLのなかから目的のコンテンtぷを拾う
            val targetElm: Element? = htmldoc.getElementById("GridSupInfo")
            /*
            if ( targetElm != null ) { println("targetElm is not null") } else { println("targetElm is null") }
            val attribs: Attributes? = targetElm?.attributes()
            if ( attribs != null ) {
                println("attributes : ")
                attribs.asList().forEach { println("key=${it.key} : val=${it.value}")}
            } else {
                println("no attributes exist.")
            }
            */
            val targetAllTDtxt: List<String>? = targetElm?.getElementsByTag("td")?.eachText()

            println("size of targetAllTDtxt: ${targetAllTDtxt?.size ?: "zero"}")

            val targetTxtMap: HashMap<String,String>? = if (( targetAllTDtxt != null ) && ( targetAllTDtxt.size >= 4)) {
                hashMapOf("hostname" to targethostname,
                        "kanshiSystem" to "HP",
                        "status" to targetAllTDtxt.get(2),
                        "date" to targetAllTDtxt.get(3))  // <td>の４個のうち、最後の２個のテキスト
            } else {
                //null
                hashMapOf("hostname" to targethostname,
                        "kanshiSystem" to "",
                        "status" to "",
                        "date" to "")
            }

            response.close()
            return targetTxtMap

        } else {
            response.close()
            return null
        }



    }


}

fun main(args: Array<String>) {
    val HPKanshi = HPKanshiStatus()

    //println("response: ${HPKanshi.getReqParam()}")

    val targethost = "VMMGR001" //"vmmgr001.au.toyota.co.jp"
    println("HP kanshi statos of ${targethost} is : ")

    val retval: HashMap<String,String>? = HPKanshi.CheckHostStatus(targethost)
    println("status= ${retval?.get("status")} : date= ${retval?.get("date")}")
}