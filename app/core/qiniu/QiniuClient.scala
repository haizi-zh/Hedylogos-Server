package core.qiniu

import com.qiniu.util.{ Auth, StringMap }
import play.api.{ Configuration, Play }
import play.api.inject.BindingKey

/**
 * Created by zephyre on 4/25/15.
 */
object QiniuClient {

  private val playConf = {
    import Play.current

    Play.application.injector instanceOf (BindingKey(classOf[Configuration]) qualifiedWith "default")
  }

  private def getConfig: Map[String, String] = {
    Map(
      "accessKey" -> playConf.getString("hedylogos.qiniu.accessKey").get,
      "secretKey" -> playConf.getString("hedylogos.qiniu.secretKey").get
    )
  }

  val secretKey = getConfig apply "secretKey"

  val accessKey = getConfig apply "accessKey"

  val auth = Auth.create(accessKey, secretKey)

  val publicBucket = "impubres"

  val privateBucket = "imprivres"

  def uploadToken(key: String, bucket: String = "imres", expire: Long = 3600, policy: Map[String, String] = null) = {
    val m = new StringMap
    if (policy != null) {
      for ((k, v) <- policy.iterator)
        m.putNotEmpty(k, v)
    }

    auth.uploadToken(bucket, key, expire, m)
  }

  def privateDownloadUrl(baseUrl: String, expireSec: Long): String =
    auth.privateDownloadUrl(baseUrl, expireSec)

  def privateDownloadUrl(baseUrl: String): String = auth.privateDownloadUrl(baseUrl)
}
