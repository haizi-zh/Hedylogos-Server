package core.json

import models.Message

/**
 * Created by zephyre on 7/11/15.
 */
class InstantMessageSerializer[T <: Message] extends MessageSerializer[T]("IM")
