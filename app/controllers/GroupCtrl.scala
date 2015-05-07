package controllers


import core.{Chat, Group}
import core.json.{UserInfoSimpleFormatter, MessageFormatter}
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.mvc.{Action, Controller}
import scala.collection.JavaConversions._

/**
 * Created by topy on 2015/4/25.
 */
object GroupCtrl extends Controller {


  val ACTION_ADDMEMBERS = "addMembers"
  val ACTION_DELMEMBERS = "delMembers"

  /**
   * 创建群组
   *
   * @return
   */
  def createGroup() = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val name = (jsonNode \ "name").asOpt[String].get
      val groupType = (jsonNode \ "groupType").asOpt[String].get
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean].get
      val participants = (jsonNode \ "participants").asOpt[Array[Long]]
      for {
        group <- Group.createGroup(uid, name, groupType, isPublic, if (participants.nonEmpty) participants.get else null)
        conversation <- Chat.groupConversation(group)
      } yield {
        val result = JsObject(Seq(
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "name" -> JsString(group.getName),
          "creator" -> JsNumber(group.getCreator.toLong),
          "groupType" -> JsString(group.getType),
          "isPublic" -> JsBoolean(group.getVisible),
          "conversation" -> JsString(conversation.getId.toString)
        ))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 修改群组
   *
   * @param gid
   * @return
   */
  def modifyGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val groupId = (jsonNode \ "groupId").asOpt[Long].get
      val name = (jsonNode \ "name").asOpt[String]
      val desc = (jsonNode \ "desc").asOpt[String]
      val avatar = (jsonNode \ "avatar").asOpt[String]
      val maxUsers = (jsonNode \ "maxUsers").asOpt[Int]
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean]
      Group.modifyGroup(groupId, name, desc, avatar, maxUsers, isPublic).map(v => Helpers.JsonResponse())
    }
  }

  /**
   * 取得群组信息
   *
   * @param gid
   * @return
   */
  def getGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      for {
        group <- Group.getGroup(gid)
        creator <- Group.getUserInfo(Seq(group.getCreator)) map (_(0))
        admin <- Group.getUserInfo(group.getAdmin map scala.Long.unbox)
      } yield {

        val result = JsObject(Seq(
          "id" -> JsString(group.getId.toString),
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "name" -> JsString(group.getName),
          "creator" -> JsNumber(group.getCreator.toLong),
          "groupType" -> JsString(group.getType),
          "isPublic" -> JsBoolean(group.getVisible),
          "creator" -> JsObject(Seq(
            "userId" -> JsNumber(creator.getUserId.toLong),
            "nickName" -> JsString(creator.getNickName),
            "avatar" -> JsString(creator.getAvatar)
          )
          ),
          "admin" -> JsArray(admin.map(UserInfoSimpleFormatter.format)),
          "desc" -> JsString(group.getDesc),
          "maxUser" -> JsNumber(group.getMaxUsers.toInt),
          "createTime" -> JsNumber(group.getCreateTime.toLong),
          "updateTime" -> JsNumber(group.getUpdateTime.toLong),
          "visible" -> JsBoolean(group.getVisible),
          "participantCnt" -> JsNumber(group.getParticipantCnt.toInt)
        )
        )

        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 取得群组中的成员信息
   *
   * @param gid
   * @return
   */
  def getGroupUsers(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      for {
        group <- Group.getGroup(gid)
        participant <- Group.getUserInfo(group.getParticipants map scala.Long.unbox)
      } yield {
        val result = JsArray(participant.map(UserInfoSimpleFormatter.format))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  /**
   * 操作群组
   *
   * @param gid
   * @return
   */
  def opGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val action = (jsonNode \ "action").asOpt[String].get
      val participants = (jsonNode \ "participants").asOpt[Array[Long]].get
      Group.opGroup(gid, action, participants).map(v => Helpers.JsonResponse())
    }
  }

}
