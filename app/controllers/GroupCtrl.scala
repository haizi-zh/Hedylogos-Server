package controllers


import core.Group
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

  def createGroup() = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val name = (jsonNode \ "name").asOpt[String].get
      val groupType = (jsonNode \ "groupType").asOpt[String].get
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean].get

      for {
        group <- Group.createGroup(uid, name, groupType, isPublic)
      } yield {
        val result = JsObject(Seq(
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "name" -> JsString(group.getName),
          "creator" -> JsNumber(group.getCreator.toLong),
          "groupType" -> JsString(group.getType),
          "isPublic" -> JsBoolean(group.getVisible)
        ))
        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

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

  def getGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      for {
        group <- Group.getGroup(gid)
        creator <- Group.getUserInfo(Seq(group.getCreator)) map (_(0))
        admin <- Group.getUserInfo(group.getAdmin map scala.Long.unbox)
      } yield {

        val result = JsObject(Seq(
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

  def getGroupUsers(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      for {
        group <- Group.getGroup(gid)
        participant <- Group.getUserInfo(group.getParticipants map scala.Long.unbox)
      } yield {

        val result = JsObject(Seq(
          "groupId" -> JsNumber(group.getGroupId.toLong),
          "participants" -> JsArray(participant.map(UserInfoSimpleFormatter.format))
        )
        )

        Helpers.JsonResponse(data = Some(result))
      }
    }
  }

  def opGroup(gid: Long) = Action.async {
    request => {
      val uid = request.headers.get("UserId").get.toLong
      val jsonNode = request.body.asJson.get
      val groupId = (jsonNode \ "groupId").asOpt[Long].get
      val name = (jsonNode \ "name").asOpt[String]
      val desc = (jsonNode \ "desc").asOpt[String]
      val avatar = (jsonNode \ "avatar").asOpt[String]
      val maxUsers = (jsonNode \ "maxUsers").asOpt[Int]
      val isPublic = (jsonNode \ "isPublic").asOpt[Boolean]
      val action = (jsonNode \ "action").asOpt[String]
      val participants = (jsonNode \ "participants").asOpt[Array[String]]

      Group.modifyGroup(groupId, name, desc, avatar, maxUsers, isPublic).map(v => Helpers.JsonResponse())
    }
  }


}