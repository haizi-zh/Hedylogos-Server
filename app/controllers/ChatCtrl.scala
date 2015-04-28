package controllers

import core.Chat
import core.json.MessageFormatter
import org.bson.types.ObjectId
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, AnyContent, Controller, Request, Result}

import scala.concurrent.Future

/**
 * Created by zephyre on 4/23/15.
 */
object ChatCtrl extends Controller {

  case class MessageInfo(senderId: Long, receiverId: Option[Long], cid: Option[ObjectId], msgType: Int,
                         contents: Option[String])

  private def sendMessageImpl(msgInfo: MessageInfo): Future[Result] = {
    val cid = msgInfo.cid

    val futureMsg = if (cid.nonEmpty)
      Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), cid.get, msgInfo.senderId)
    else
      Chat.sendMessage(msgInfo.msgType, msgInfo.contents.getOrElse(""), msgInfo.receiverId.get, msgInfo.senderId)

    futureMsg.map(msg => {
      val result = JsObject(Seq(
        "conversation" -> JsString(msg.getConversation.toString),
        "msgId" -> JsNumber(msg.getMsgId.toLong),
        "timestamp" -> JsNumber(msg.getTimestamp.toLong)
      ))
      Helpers.JsonResponse(data = Some(result))
    })
  }

  def sendMessageQiniu(request: Request[AnyContent]) = {
    val postBody = request.body.asFormUrlEncoded.get
    val postMap = postBody.filter((item: (String, Seq[String])) => item._2.nonEmpty).mapValues(_(0))

    val senderId = postMap.get("sender").get.toLong
    val recvId = postMap.get("receiver").map(_.toLong)
    val cid = postMap.get("conversation").map(v => new ObjectId(v))
    val msgType = postMap.get("msgType").get.toInt
    val contents = None

    sendMessageImpl(MessageInfo(senderId, recvId, cid, msgType, contents))
  }

  def sendMessage() = Action.async {
    request => {
      val jsonNode = request.body.asJson.get
      val senderId = (jsonNode \ "sender").asOpt[Long].get
      val recvId = (jsonNode \ "receiver").asOpt[Long]
      val cid = (jsonNode \ "conversation").asOpt[String].map(v => new ObjectId(v))
      val msgType = (jsonNode \ "msgType").asOpt[Int].get
      val contents = (jsonNode \ "contents").asOpt[String]

      sendMessageImpl(MessageInfo(senderId, recvId, cid, msgType, contents))
    }
  }


  def acknowledge(user: Long) = Action.async {
    request => {
      val jsonNode = request.body.asJson.get
      val ackMessages = (jsonNode \ "msgList").asInstanceOf[JsArray].value.map(_.asOpt[String].get)

      Chat.acknowledge(user, ackMessages).flatMap(_ =>
        _fetchMessages(user).map(jsvalue => Helpers.JsonResponse(data = Some(jsvalue))))
    }
  }

  def fetchMessages(user: Long) = Action.async {
    Chat.fetchMessage(user).map(msgSeq => {
      Helpers.JsonResponse(data = Some(JsArray(msgSeq.map(MessageFormatter.format))))
    })
  }

  def _fetchMessages(user: Long): Future[JsValue] = {
    Chat.fetchMessage(user).map(msgSeq => {
      JsArray(msgSeq.map(MessageFormatter.format(_)))
    })
  }
}
