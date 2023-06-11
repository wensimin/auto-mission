# auto-mission
自带注入对象的一些方法参数  

# 日志对象  
试运行环境下会返回logger结果,生产环境下会记录到taskLogger模块中  
`val logger = bindings["logger"] as TaskLogger`  
标准方法  
`fun log(label: Label, message: String)`  
简易变体  
````
  fun debug(message: String) = log(Label.DEBUG, message)
  fun info(message: String) = log(Label.INFO, message)
  fun error(message: String) = log(Label.ERROR, message)
  fun warn(message: String) = log(Label.WARN, message)
````
# 通知服务
`val messageService = bindings["messageService"] as MessageService`    
目前有简易用法sendMessageToUser&sendMessageToTopic  
例子  
````
fun sendMessageToUser(
        title: String,
        body: String,
        user: String,
        url: String? = null,
        fromClient: String? = "autoTask",
        priority: MessageVo.Priority = MessageVo.Priority.NORMAL
    )
````
以及sendMail (仅管理员)  
` fun sendMail(title: String, body: String)`  
# store
`val store  =  bindings["store"] as KVStore`  
简易的kv store 仅存储string  
目前为全局key&jdbc持久化实现  
````
    fun get(key: String): String?
    fun set(key: String, value: String?)
````
# objectMapper 
jackson的objectMapper  
`val objectMapper = bindings["objectMapper"] as ObjectMapper`  
# webClient
spring boot 自带webClient  
`val webClient = bindings["webClient"] as WebClient`  
示例
````
    val formData = LinkedMultiValueMap<String,String>().apply {
        add("")
    }
    webClient.post().uri("")
        .header("cookie", cookie)
        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
        .body(BodyInserters.fromFormData(formData))
        .retrieve()
        .bodyToMono(JsonNode::class.java)
        .block()

````
# webDriver
[详细文档](https://www.selenium.dev/documentation/)  
需要预先安装driver并且设置vm变量,注意平台区分  
-Dwebdriver.chrome.driver=chromedriver.exe
````
// 需要额外导入
import org.openqa.selenium.chrome.*
import org.openqa.selenium.*
import org.openqa.selenium.remote.*
import java.net.URL

// 示例用法
RemoteWebDriver(URL("http://192.168.0.200:4444"), ChromeOptions()).run{
    // 注意添加cookie必须要打开网页一次后再添加
    get("https://www.douyu.com/$roomId")
    cookie.split(";").map { 
        val (left, right) = it.split("=")
        manage().addCookie(Cookie(left.trim(), right.trim()))
    }
    get("https://www.douyu.com/$roomId")
    logger.debug("title: ${getTitle()}")
    Thread.sleep(10*1000L)
    quit()
}

````