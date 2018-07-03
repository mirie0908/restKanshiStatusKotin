import okhttp3.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import org.w3c.dom.Document
import org.w3c.dom.NodeList
import java.net.URLEncoder
import javax.xml.parsers.DocumentBuilderFactory
import java.io.BufferedReader
import java.io.File

class JP1KanshiStatus(val baseurl: String = "http://161.93.233.94/") {

    /*
     * JP1監視Status問い合わせ用URLにrequestをするために必要なパラメータを予め取得する
     */
    fun getReqParam(): String? {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        val url = baseurl + "HostSearch.aspx"

        val request: Request = Request.Builder().url(url).build()
        val response : Response = client.newCall(request).execute()
        if (response.isSuccessful) {

            // java.io.Reader クラス　に読み出す
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = "${lineStr}\n"
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += "${lineStr}\n"
            }

            // JP1監視画面は、xmlではパースできなかった（Powershellでのテスト）ので、
            // jsoupでHTMLでパースし、必要なパラメータを取得する
            val htmldoc: org.jsoup.nodes.Document = Jsoup.parse(strRes)

            //  inputタグの情報取り出し

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

            if (htmldoc.getElementById("__VIEWSTATEGENERATOR") != null) {
                paramTab?.put(key = "__VIEWSTATEGENERATOR" , value = htmldoc.getElementById("__VIEWSTATEGENERATOR").attributes().get("value"))
            }

            if (htmldoc.getElementById("__EVENTVALIDATION") != null) {
                paramTab?.put(key = "__EVENTVALIDATION" , value = htmldoc.getElementById("__EVENTVALIDATION").attributes().get("value"))
            }

            // request parameter文字列に組み立て
            var paramStr: String? = ""
            paramTab?.forEach {
                paramStr += "${it.key}=${URLEncoder.encode(it.value)}&"
            }

            response.close()  // don't forget

            return paramStr

        } else {
            return "response error"
        }
    }

    /*
　　* JP1監視Status問い合わせ用URLにrequestをするために必要なパラメータを予め取得する
　　*/
    fun CheckJP1HostStatus(targethostname: String): HashMap<String,String>? {

        val client: OkHttpClient = okhttp3.OkHttpClient()

        val url = baseurl + "ItemList.aspx"

        // JP1監視URLにリクエストするためのパラメータの生成
        val additionalParam: String = "__EVENTTARGET=&__EVENTARGUMENT=&"  // ともにvalueはヌル"
        val bodyParam: String = "${additionalParam}${this.getReqParam()}hostname=${targethostname}&Hyouji=%E8%A1%A8%E7%A4%BA"

        // JP1監視URLにリクエストし、監視抑止状態を取得
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
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += lineStr
                strRes += "\n"
            }

            // test file 出録
            File("/home/share/testJP1.html").bufferedWriter().use{ out -> out.write(strRes)}

            // 取得した文字列を、jsoupでHTMLとしてパース
            val htmldoc: org.jsoup.nodes.Document = Jsoup.parse(strRes)

            // パースしたHTMLのなかから目的のコンテンtぷを拾う
            //1. innerHTML="２．抑止情報"の<td>タグのElm取得
            //2. その親の<tr>タグの 最初の兄弟の<tr>タグElmを取得
            //3. その2番目の子供の<td>タグElmを取得
            //4. その子供の<table>タグElmを取得
            //5. その子供の<tr>タグのうち2番目のElmを取得
            //6. その子供の<td>タグ（4個）を取得
            //7. そのうち、3番目の<td>タグElmのinnerHTML = 抑止Status
            //8. および、  4番目の<td>タグElmのinnerHTML = 抑止/抑止解除最終変更日付

            var firstTargetElm: Element? = null
            htmldoc.getElementsByTag("td")?.forEach {
                if ("""２．抑止情報""".toRegex().matches(it.text())) {
                    firstTargetElm = it
                }
            }

            if (firstTargetElm == null) {
                println("２．関し抑止のtdタグ要素は見つかりませんでした。以降のhtmlトラバースができないので処理を中止します。")
                response.close()
                return null
            }

            val tableElm: Element? = firstTargetElm?.parent()?.nextElementSibling() //親のtrの　最初の(0番目の)兄弟tr
                    ?.child(1)                    // trの　２番めの子供の td
                    ?.child(0)                    // その１番目の子供の table

            if ( tableElm != null) {
                println("tag name of tableElm : ${tableElm.tagName()}")
            } else {
                println("tableElm is null")
            }

            println("child node size of tableElm: ${tableElm?.childNodeSize() ?: 0}")  // ->これが１なんだけど。つまりtableの下に子は一人

            val targetAllTDtxt: List<String>? = tableElm?.child(0)     // tableの下の２番めのtr -> １個しか認識しないようなので0に修正。
                    ?.getElementsByClass("style22")                // class指定にすることで、なんとか、目的の４つのtdタグに絞り込んで取得できた。
                    ?.eachText()                                              //  それらすべてのテキスト

            println("targetAllTDtxt size : ${targetAllTDtxt?.size ?: 0}")     // -> 最初、これ１１個も取れていた。。。 これ パースが失敗しているっぽい。

            val targetTxtMap: HashMap<String,String>? = if (( targetAllTDtxt != null ) && ( targetAllTDtxt.size >= 4)) {
                hashMapOf("status" to targetAllTDtxt.get(2), "date" to targetAllTDtxt.get(3))  // <td>の４個のうち、最後の２個のテキスト
            } else {
                null
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
    val JP1Kanshi = JP1KanshiStatus()

    val targethost = "G-HASHIGO"
    //println("JP1 kanshi request param of ${targethost} is : ${JP1Kanshi.getReqParam()}")

    val retval: HashMap<String,String>? = JP1Kanshi.CheckJP1HostStatus(targethost)
    println("status= ${retval?.get("status")} : date= ${retval?.get("date")}")

}