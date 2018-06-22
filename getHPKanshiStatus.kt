import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.BufferedReader
import java.io.Reader
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.DocumentBuilder
import org.w3c.dom.*
import java.io.InputStream


class HPKanshiStatus(val url: String = "http://161.95.212.225:30000/HostSearch/") {

    val client: OkHttpClient = okhttp3.OkHttpClient()


    fun getReqParam(): String? {
        val request: Request = Request.Builder().url(url).build()
        val response : Response = client.newCall(request).execute()
        if (response.isSuccessful) {
            /*
            // java.io.Reader クラス　に読み出す
            val br: BufferedReader = BufferedReader(response.body()?.charStream())
            var lineStr: String? = br.readLine() // 最初の１行
            var strRes: String? = lineStr
            while ( lineStr != null ) {
                lineStr = br.readLine()
                strRes += lineStr
            }
            return strRes
*/
            // 読み込んだものを、javax.xml.parsers.DocumentBuilder に渡してXmLをパースするには、
            // java.io.BufferedReader で読み込んで渡すのではなく、
            // java.io.InputStreamReader で読み込んで、Streamを引数に渡して、xml parse する必要がある。
            //val inpStream: InputStream = response.body()?.byteStream()
            val xmlDoc: Document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.body()?.byteStream())

            xmlDoc.documentElement.normalize()

            val nodelist: NodeList =  xmlDoc.documentElement.getElementsByTagName("input")

            if ( nodelist.length > 0 ) {
                for(var aNode in nodelist)
            }


        } else {
            return "response error"
        }
    }


}

fun main(args: Array<String>) {
    val HPKanshi = HPKanshiStatus()

    println("response: ${HPKanshi.getReqParam()}")
}