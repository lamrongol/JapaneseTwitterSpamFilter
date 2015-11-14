package lamrongol.twitter.spamfilter

import twitter4j.Status

/**
  * Created by admin on 2015/10/15.
  */
trait SpamFilter {
  def calcSpamScore(status: Status): Double
  def isSpam(status: Status): Boolean = calcSpamScore(status)>0
}
