package lamrongol.twitter.stream

import twitter4j.Status

/**
  * Created by admin on 2015/10/16.
  */
trait StreamProcessor {
  def processStatus(status: Status)

  def onDelete(statusId: Long) {}

}
