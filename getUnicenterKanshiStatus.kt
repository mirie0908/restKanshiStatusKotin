import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import java.io.BufferedReader
import java.io.File
import java.net.URLEncoder

class UnicenterKanshiStatus(val url: String = "http://kanshi-portal.mx.toyota.co.jp/server-info/setup-info.aspx") {

    //**************************************************
    // 監視対象の全ホスト名 (=初期画面のドロップリスト）を取得
    //**************************************************
    fun getAllTargetHostnames(): List<String> {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        val request: Request = Request.Builder().url(url).build()
        val response : Response = client.newCall(request).execute()
        if (response.isSuccessful) {

            // 全Htmlを読み出す
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = "${lineStr}\n"
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += "${lineStr}\n"
            }

            // jsoupでHTMLでパースし、必要なパラメータを取得する
            val htmldoc: org.jsoup.nodes.Document = Jsoup.parse(strRes)

            //  selectタグ id=DropDownList の下の、すべての optionタグのテキストを読み込み

            //val AllHostnames: List<String> = htmldoc.getElementById("DropDownList1").getElementsByTag("option").eachText()
            val AllHostnamesElms: org.jsoup.select.Elements = htmldoc.getElementById("DropDownList1").getElementsByTag("option")
            val AllHostnames: List<String> = AllHostnamesElms.eachAttr("value")
/*
            println("size of AllHostnames : ${AllHostnames.size}")
            AllHostnames.forEach {
                println(it)
            }
*/

            response.close()  // don't forget

            return AllHostnames //paramStr

        } else {
            response.close()
            return emptyList()
        }
    }


    //**************************************************
    // postリクエストに必要なrequestbody用パラメータを取得
    //**************************************************
    fun getReqParam(): String {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        val request: Request = Request.Builder().url(url).build()
        val response : Response = client.newCall(request).execute()
        if (response.isSuccessful) {

            // 全Htmlを読み出す
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = "${lineStr}\n"
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += "${lineStr}\n"
            }

            // jsoupでHTMLでパースし、必要なパラメータを取得する
            val htmldoc: org.jsoup.nodes.Document = Jsoup.parse(strRes)

            //  formタグ id=ct100 の下の、すべての inputタグを読み込み
            var paramTab: HashMap<String,String>? = if (htmldoc.getElementById("__VIEWSTATE") != null) {
                hashMapOf("__VIEWSTATE" to htmldoc.getElementById("__VIEWSTATE").attributes().get("value"))
            } else {
                emptyMap<String,String>() as HashMap
            }

            if (htmldoc.getElementById("__EVENTTARGET") != null) {
                paramTab?.put(key = "__EVENTTARGET" , value = htmldoc.getElementById("__EVENTTARGET").attributes().get("value"))
            } else {
                println("__EVENTTARGET : no element")
            }

            if (htmldoc.getElementById("__EVENTARGUMENT") != null) {
                paramTab?.put(key = "__EVENTARGUMENT" , value = htmldoc.getElementById("__EVENTARGUMENT").attributes().get("value"))
            } else {
                println("__EVENTARGUMENT : no element")
            }

            /*
            if (htmldoc.getElementById("__VIEWSTATEGENERATOR") != null) {
                paramTab?.put(key = "__VIEWSTATEGENERATOR" , value = htmldoc.getElementById("__VIEWSTATEGENERATOR").attributes().get("value"))
            }
            */

            if (htmldoc.getElementById("__EVENTVALIDATION") != null) {
                paramTab?.put(key = "__EVENTVALIDATION" , value = htmldoc.getElementById("__EVENTVALIDATION").attributes().get("value"))
            }

            // request parameter文字列に組み立て
            var paramStr: String = ""
            paramTab?.forEach {
                paramStr += "${it.key}=${URLEncoder.encode(it.value)}&"
            }

            response.close()  // don't forget
            return paramStr

        } else {
            response.close()
            return "response error"
        }
    }

    //**************************************************
    // Unicenter監視 指定のhostnameの　監視状態を取得する
    //**************************************************
    fun CheckUnicenterHostStatus(targethostname: String): HashMap<String,String>? {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        // Unicenter監視URLにリクエストするためのパラメータの生成

        var bodyParam: String = this.getReqParam()
/*
        if ( ! ( """__EVENTTARGET""".toRegex().matches(bodyParam)) ) {
            bodyParam = "__EVENTTARGET=&${bodyParam}"
        }

        if ( ! ( """__EVENTARGUMENT""".toRegex().matches(bodyParam)) ) {
            bodyParam = "__EVENTARGUMENT=&${bodyParam}"
        }
*/
        // hostnameパラメータの整え
        val targethostnameStr: String = targethostname //"${targethostname}++++++++++++++++++++++++++++++++++++++++".substring(0,40)

        bodyParam = "${bodyParam}DropDownList1=${URLEncoder.encode(targethostnameStr)}&ctl01=${URLEncoder.encode("表　示")}"

        /*
        println("bodyParam : ")
        bodyParam.split('&').forEach {
            println(it)
        }
        */

        // Unicenter監視URLにリクエストし、監視抑止状態を取得
        val request: Request = Request.Builder()
                .url(url)
                .post(RequestBody.create(MediaType.parse("application/x-www-form-urlencoded; charset=utf-8"), bodyParam))
                .build()


        val response : Response = client.newCall(request).execute()

        // rewponse.bodyからHTML全文文字列として取得し、
        // これをHTMLとしてパースし、監視抑止状態のテキストを取り出す
        if (response.isSuccessful) {
            println("the url post call successful.")

            // HTML全文を文字列として取得
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = "${lineStr}\n"
            while (lineStr != null) {
                lineStr = br.readLine()
                strRes += lineStr
                strRes += "\n"
            }

            // test file 出録
            //File("/home/share/testUnicenter.html").bufferedWriter().use { out -> out.write(strRes) }

            // 取得した文字列を、jsoupでHTMLとしてパース
            val htmldoc: org.jsoup.nodes.Document = Jsoup.parse(strRes)

            // 1. table id=DataGrid1 の下の tbodyの下の
            // 2. ２番めのtrの下の、全てのtd要素をとり
            // 3. このうちの７番目と８番目と１０番目のtd要素

            val tbodyElm: Element = htmldoc.getElementById("DataGrid1").getElementsByTag("tbody").first()

            val tdElms: org.jsoup.select.Elements = tbodyElm.child(1) // tbody要素の下の２番めのtr要素
                    .getElementsByTag("td")                       //  その下のtdタグ

            var retval: HashMap<String,String>? = if ((tdElms != null) && (tdElms.size >= 10)) {
                hashMapOf("status" to tdElms.get(6).child(0).text(),
                        "date" to tdElms.get(7).child(0).text(),
                        "lastSupDate" to tdElms.get(9).child(0).text()
                )
            } else {
                null
            }


            response.close()
            return retval

        } else {
            println("post requestが失敗しました。")
            response.close()
            return null
        }


    }

}



fun main(args: Array<String>) {
    val UnicenterKanshi = UnicenterKanshiStatus()
/*
    println("Unicenter kanshi All hostnames : ")
    var hostnamestr: String? = ""
    UnicenterKanshi.getAllTargetHostnames().forEach {
        hostnamestr += "${it}\n"
    }
    // test all hostname write to textfile
    File("/home/share/testUnicenerHostnames.txt").bufferedWriter().use{ out -> out.write(hostnamestr)}
*/
/*
    println("Unicenter kanshi request param is : ")  // Unicenterでは、EVENTTARGETもEVENTARGUMENTも、ヌルとしてちゃんと取れていて、returnされる文字列にちゃんと含まれている。
    UnicenterKanshi.getReqParam().split('&').forEach {
        println(it)
    }
    */

    val targethostname = "APLQDTC1837".toLowerCase() //Unicenterの監視Status画面のselect option値=ホスト名は、全て小文字なので、targethostnameは小文字にして渡すこと。

    val targetHostnames: List<String> = UnicenterKanshi.getAllTargetHostnames()  //.map { it -> it.toUpperCase() }

    val theStr: String? = targetHostnames.matchedStr(targethostname)  //自作拡張関数

    var retval: HashMap<String,String>? = null

    if ( theStr != null ) {
        println("指定の${targethostname} は、Unicenter監視対象です。")
        retval= UnicenterKanshi.CheckUnicenterHostStatus(theStr)
    } else {
        println("指定の${targethostname} は、Unicenter監視対象ではありません。")
    }

    println("status= ${retval?.get("status")} : date= ${retval?.get("date")} : last Sup Date= ${retval?.get("lastSupDate")}")

}