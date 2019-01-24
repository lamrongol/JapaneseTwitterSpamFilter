package lamrongol.twitter.spamfilter

import java.io.InputStream
import java.net.URI
import java.util.regex.{Matcher, Pattern}

import org.apache.commons.lang.StringUtils
import twitter4j.Status

import scala.collection.mutable

/**
  * Created by admin on 2015/10/15.
  */
object SimpleSpamFilter extends SpamFilter {
  val SPAM_SOURCE_SET = loadResource("/spamSourceList.txt")
  val SPAM_DOMAIN_SET = loadResource("/spamSourceList.txt")
  val SPAM_HASHTAG_SET = loadResource("/spamHashtagList.txt")

  //For test
  def main(args: Array[String]) {
    println("test")
  }


  override def calcSpamScore(status: Status): Double = {
   if (SPAM_SOURCE_SET.contains(status.getSource)) return 1

    val sourceUrl = extractSourceUrl(status.getSource)
    if (sourceUrl == "www.yahoo.co.jp/" || sourceUrl == "www.google.co.jp/") return 1

    for (url <- status.getURLEntities) {
      val domain = getDomainName(url.getExpandedURL)
      if (SPAM_DOMAIN_SET.contains(domain)) return 1
    }

    for (hashtag <- status.getHashtagEntities) {
      if (SPAM_HASHTAG_SET.contains(hashtag.getText)) return 1
    }

    return -1
  }

  private val SOURCE_URL_PATTERN: Pattern = Pattern.compile("<a href=\"https?://([^\"]+)")

  def extractSourceUrl(source: String): String = {
    val sourceHostMatcher: Matcher = SOURCE_URL_PATTERN.matcher(source)
    return if (sourceHostMatcher.find) sourceHostMatcher.group(1) else source
  }

  def getDomainName(url: String): String = {
    val uri = new URI(url);
    val domain = uri.getHost();
    return if (domain.startsWith("www.")) domain.substring(4) else domain;
  }

  private def loadResource(fileName: String): mutable.Set[String] = {
    var set = scala.collection.mutable.Set.empty[String]

    val stream: InputStream = getClass.getResourceAsStream(fileName)
    val lines = scala.io.Source.fromInputStream(stream).getLines
    for (line <- lines if (!StringUtils.isBlank(line) && line.charAt(0) != '#')) {
      set += line
    }
    set
  }

}
