package lamrongol.twitter.spamfilter

import java.io._

import com.atilika.kuromoji.ipadic.{Tokenizer, Token}
import com.twitter.Extractor
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import twitter4j.Status

/**
  * Created by admin on 2015/10/16.
  */
class JapaneseSpamFilter extends SpamFilter with Serializable {
  /** 全体での出現回数がこの指定値を下回る単語を切り捨てるようにします */
  val WORD_COUNT_LOWER_THRESHOLD = 5

  /** SVM訓練の繰り返し回数 */
  val NUM_ITERATIONS = 100

  var wordIndexes: scala.collection.Map[String, Long] = null
  var model: SVMModel = null

  @transient var tokenizer: Tokenizer = null

  /**
    * @param trainFile ラベル\tツイートID\tスクリーンネーム\tソース（発信クライアント）\t本文 という形式のファイル
    * @param saveFile 作成したスパムフィルターを保存するファイル
    */
  def train(trainFile: String, saveFile: String) = {
    val sc = new SparkContext("local", "demo")

    //SMSSpamCollection( https://archive.ics.uci.edu/ml/datasets/SMS+Spam+Collection )はスパムかどうかを示すラベル(spam or ham)と本文がタブ区切りになっている
    val preparedData = sc.textFile(trainFile)
      .map(_.split("\t"))
      //items: ラベル  ツイートID  スクリーンネーム  ソース（発信クライアント） テキスト
      .map(items => (items(0), convertToWordArray(items(4), items(3), true))) //ラベル(wordsInSms.head)とそれ以外（本文)のMapに変換


    // 単語を特徴ベクトルのインデックスに変換できるように準備します
    wordIndexes = preparedData
      .flatMap(_._2)
      //.map(_.getSurface)
      .map((_, 1)) //(各単語, 1)というtupleにする
      .reduceByKey(_ + _) //同じkey（単語）のtupleのvalue(上の1)を足し合わせる、すなわち単語の出現数を数える
      .filter(_._2 > WORD_COUNT_LOWER_THRESHOLD) //2番目（単語の出現数）が一定数以上のもののみを取り出す
      .keys //key, すなわち単語を取り出す
      .zipWithIndex() //Index化:（0番目の単語,0), (1番目の単語,1), (2番目の単語,2) ...というtupleに変換
      .collectAsMap() //mapに変換

    // LabeledPoint に変換します
    val dataSet = preparedData.map {
      case (label, words) =>
        LabeledPoint(
          if (label == "spam") 1 else 0,
          convertToVector(words)
        )
    }

    println("Start training... It may take long time")
    model = SVMWithSGD.train(dataSet, NUM_ITERATIONS)
    model.clearThreshold()

    //交差検定する場合
    //CrossValidation.printSVMEvaluations(dataSet)

    val oos = new ObjectOutputStream(new FileOutputStream(saveFile))
    oos.writeObject(this)
    oos.close()
  }

  override def calcSpamScore(status: Status): Double = {
    val vector = convertToVector(convertToWordArray(status.getText, status.getSource))
    model.predict(vector)
  }

  val SOURCE_TEXT_PATTERN = "<a href=\"https?://[^\"]+\" rel=\"nofollow\">(.*)<\\/a>".r
  val UNNECESSARY_PATTERN = "<.*?>|\\[\\[.*?\\]\\]|\\[.*?\\]||\\{\\{.*?\\}\\}|\\=\\=.*?\\=\\=|&gt|&lt|&quot|&amp|&nbsp|-|\\||\\!|\\*|'|^[\\:\\;\\/\\=]$|;|\\(|\\)|\\/|:"
  val URL_PATTERN = "(https?|ftp)(://[-_.!~*\\'()a-zA-Z0-9;/?:@&=+$,%#]+)"

  private def convertToWordArray(text: String, source: String, initializeKuromoji: Boolean = false): Array[String] = {
    //MLLibを使う場合SerializeできないKuromojiのTokenizerは毎回生成する必要がある
    val tokenizer = if (initializeKuromoji) new Tokenizer.Builder().build else this.tokenizer

    Array.concat({
      source match {
        case SOURCE_TEXT_PATTERN(sourceText) => {
          tokenizer.tokenize(sourceText.replaceAll(UNNECESSARY_PATTERN, "")).toArray(new Array[Token](0))
            .map("source_" + _.getSurface)
        }
        case _ => {
          //println("error source:" + source)
          new Array[String](0)
        }
      }
    }, {
      val hashtags = new Extractor().extractHashtags(text).toArray(new Array[String](0))
      for (i <- 0 until hashtags.length) {
        hashtags(i) = "hashtag_" + hashtags(i)
      }
      hashtags
    },
      tokenizer.tokenize(text.replaceAll(UNNECESSARY_PATTERN, "").replaceAll(URL_PATTERN, " UURRLL ")).toArray(new Array[Token](0))
        //非自立語（「の」「が」「ます」などの単語）を除く
        .filter(token => {
        val pos = token.getPartOfSpeechLevel1
        pos != "記号" && pos != "助動詞" && pos != "助詞"
      })
        .map(_.getBaseForm)
    )
  }

  private def convertToVector(words: Array[String]): Vector = {
    val wordCounts = words
      .filter(wordIndexes.contains) //うえで作った単語のIndexに含まれているもののみ取り出す
      .map(wordIndexes(_).toInt) //単語を上のIndexに変換
      .groupBy(_.toInt) //同じIndex（の単語）をまとめる
      .mapValues(_.length.toDouble) //（単語のIndex, 単語の数)のMapを作成

    Vectors.sparse(wordIndexes.size, wordCounts.toSeq)
  }
}

object JapaneseSpamFilter {
  def load(saveFile: String): JapaneseSpamFilter = {
    val inFile = new FileInputStream(saveFile)
    val inObject = new ObjectInputStream(inFile)
    val filter = inObject.readObject.asInstanceOf[JapaneseSpamFilter]
    filter.tokenizer = new Tokenizer.Builder().build
    filter
  }

  def loadDefault: JapaneseSpamFilter = {
    val stream = getClass.getResourceAsStream("/JapaneseSpamFilter.dat")
    val inObject = new ObjectInputStream(stream)
    val filter = inObject.readObject.asInstanceOf[JapaneseSpamFilter]
    filter.tokenizer = new Tokenizer.Builder().build
    filter
  }

  def main(args: Array[String]) {
    new JapaneseSpamFilter().train("D:\\SpamData\\TwitterSpamJa.tsv", "JapaneseSpamFilter.dat")
  }
}