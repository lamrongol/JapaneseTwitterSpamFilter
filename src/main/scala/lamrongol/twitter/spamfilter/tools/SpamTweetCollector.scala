package lamrongol.twitter.spamfilter.tools

import java.io.{FileOutputStream, OutputStreamWriter, PrintWriter}

import lamrongol.twitter.spamfilter.{JapaneseSpamFilter, SimpleSpamFilter, SpamFilter}
import lamrongol.twitter.stream.{StreamProcessor, StreamReader}
import twitter4j.Status

/**
  * Created by admin on 2015/10/15.
  */
class SpamTweetCollector(lang: String, filePath: String, preSpamFilter: SpamFilter, hamThr: Double = 0.0, spamThr: Double = 0.0, hamSuppressFactor: Int = 25) extends StreamProcessor {
  val pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(filePath, false), "UTF-8"), true)
  var count = 0

  override def processStatus(status: Status): Unit = {
    if (status.isRetweet) return
    if (status.getLang != lang) return

    count += 1

    var label: String = null
    val score = preSpamFilter.calcSpamScore(status)
    if (score > spamThr) label = "spam"
    else if (score < hamThr) {
      if (count % hamSuppressFactor != 0) return //spamでないものは多いため数を減らす
      label = "ham"
    }
    pw.println(label + "\t" + status.getId + "\t" + status.getUser.getScreenName + "\t" + status.getSource + "\t" + SpamTweetCollector.removeSpaceTabLinebreak(status.getText))
  }
}

object SpamTweetCollector {
  private val KAIGYO: String = System.getProperty("line.separator")

  def removeSpaceTabLinebreak(text: String): String = {
    return text.replaceAll( """(\r\n?|[\n\s\t])+""", " ")
  }

  def main(args: Array[String]) {
    if (args.size < 4) {
      println("Set twitter oauth consumer key, consumer secret key,access token, access token secret to program arguments")
      System.exit(0)
    }

    val oauthConsumerKey = args(0)
    val oauthConsumerSecret = args(1)
    val oauthAccessToken = args(2)
    val oauthAccessTokenSecret = args(3)

    val reader = new StreamReader(oauthConsumerKey, oauthConsumerSecret, oauthAccessToken, oauthAccessTokenSecret)
    //collectSpamTweetUsingSimpleSpamFilter(reader)
    collectSpamTweetUsingSimpleSpamFilter(reader)
  }

  def collectSpamTweetUsingSimpleSpamFilter(reader: StreamReader): Unit = {
    val collector = new SpamTweetCollector("ja", "D:\\SpamData\\TwitterSpamJa.tsv", SimpleSpamFilter)
    reader.addProcessor(collector)
    reader.start
  }

  def collectSpamTweetUsingDefaultFilter(reader: StreamReader): Unit = {
    val collector = new SpamTweetCollector("ja", "D:\\SpamData\\SemiSupervisedTweets.tsv", JapaneseSpamFilter.loadDefault, -1.0, 1.0, 10)
    reader.addProcessor(collector)
    reader.start
  }
}