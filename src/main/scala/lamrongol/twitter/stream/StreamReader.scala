package lamrongol.twitter.stream

import twitter4j._
import twitter4j.conf.{ConfigurationBuilder, Configuration}

/**
  * Created by admin on 2015/10/16.
  */
class StreamReader(twitterConfig: Configuration) {
  def this(oauthConsumerKey: String, oauthConsumerSecret: String, oauthAccessToken: String, oauthAccessTokenSecret: String) = {
    this(
      (new ConfigurationBuilder)
        .setDebugEnabled(true)
        .setOAuthConsumerKey(oauthConsumerKey)
        .setOAuthConsumerSecret(oauthConsumerSecret)
        .setOAuthAccessToken(oauthAccessToken)
        .setOAuthAccessTokenSecret(oauthAccessTokenSecret).build)
  }

  val stream = new TwitterStreamFactory(twitterConfig).getInstance

  def addProcessor(processor: StreamProcessor): Unit = {
    val _processer: StreamProcessor = processor
    val listener: StatusListener = new StatusListener() {
      def onStatus(status: Status) {
        _processer.processStatus(status)
      }

      def onDeletionNotice(statusDeletionNotice: StatusDeletionNotice) {
        _processer.onDelete(statusDeletionNotice.getStatusId)
      }

      def onTrackLimitationNotice(numberOfLimitedStatuses: Int) {
        //log.warn("Got track limitation notice:" + numberOfLimitedStatuses)
      }

      def onScrubGeo(userId: Long, upToStatusId: Long) {
      }

      def onStallWarning(warning: StallWarning) {
      }

      def onException(ex: Exception) {
      }
    }
    stream.addListener(listener)
  }

  def start = stream.sample()

  def stop = stream.shutdown

}
