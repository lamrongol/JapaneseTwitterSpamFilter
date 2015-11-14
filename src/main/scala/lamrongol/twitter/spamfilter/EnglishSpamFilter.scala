package lamrongol.twitter.spamfilter

import java.io._

import com.typesafe.scalalogging.LazyLogging
import org.apache.spark.SparkContext
import org.apache.spark.mllib.classification.{SVMModel, SVMWithSGD}
import org.apache.spark.mllib.linalg.{Vector, Vectors}
import org.apache.spark.mllib.regression.LabeledPoint
import twitter4j.Status

/**
  * Created by admin on 2015/10/16.
  */
class EnglishSpamFilter extends SpamFilter with Serializable {
  /** 全体での出現回数がこの指定値を下回る単語を切り捨てるようにします */
  val WORD_COUNT_LOWER_THRESHOLD = 5

  /** SVM訓練の繰り返し回数 */
  val NUM_ITERATIONS = 100

  var wordIndexes: scala.collection.Map[String, Long] = null
  var model: SVMModel = null

  /**
    * @param trainFile ラベル\t本文 という形式のファイル
    * @param saveFile 作成したスパムフィルターを保存するファイル
    */
  def train(trainFile: String, saveFile: String): Unit = {
    val sc = new SparkContext("local", "demo")

    //SMSSpamCollection( https://archive.ics.uci.edu/ml/datasets/SMS+Spam+Collection )はスパムかどうかを示すラベル(spam or ham)と本文がタブ区切りになっている
    val preparedData = sc.textFile(trainFile)
      .map(removeUnnecessary) ////記号などを削除
      .map(splitBySpaces) //スペースで分割し単語列(Stringの配列）にする
      .map(wordsInSms => (wordsInSms.head, wordsInSms.tail)) //ラベル(wordsInSms.head)とそれ以外（本文)のMapに変換

    val texts = preparedData.flatMap(_._2)
    // 単語を特徴ベクトルのインデックスに変換できるように準備します
    wordIndexes = preparedData.flatMap(_._2) //2番目（学習対象のテキスト）のみ取り出す
      .map((_, 1)) //(各単語, 1)というtupleにする
      .reduceByKey(_ + _) //同じkey（単語）のtupleのvalue(上の1)を足し合わせる、すなわち単語の出現数を数える
      .filter(_._2 > WORD_COUNT_LOWER_THRESHOLD) //2番目（単語の出現数）が一定数以上のもののみを取り出す
      .keys //key, すなわち単語を取り出す
      .zipWithIndex() //Index化:（0番目の単語,0), (1番目の単語,1), (2番目の単語,2) ...というtupleに変換
      .collectAsMap() //mapに変換

    // LabeledPoint に変換します
    val dataSet = preparedData.map { case (label, words) =>
      LabeledPoint(
        if ("ham" == label) 0 else 1,
        convertToVector(words)
      )
    }

    //交差検定する場合
    //CrossValidation.printSVMEvaluations(dataSet)

    println("Start training... It may take long time")
    model = SVMWithSGD.train(dataSet, NUM_ITERATIONS)
    model.clearThreshold()

    val oos = new ObjectOutputStream(new FileOutputStream(saveFile))
    oos.writeObject(this)
    oos.close()
  }

  override def calcSpamScore(status: Status): Double = {
    val vector = convertToVector(splitBySpaces(removeUnnecessary(status.getText)))
    model.predict(vector)
  }

  private def removeUnnecessary(text: String): String = {
    text.toLowerCase.replaceAll("[,.!?\"#]", " ")
  }

  private def splitBySpaces(line: String): Array[String] = {
    line.split("\\s+")
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

object EnglishSpamFilter {
  def load(saveFile: String): EnglishSpamFilter = {
    val inFile = new FileInputStream(saveFile)
    val inObject = new ObjectInputStream(inFile)
    inObject.readObject.asInstanceOf[EnglishSpamFilter]
  }

  def loadDefault: EnglishSpamFilter = {
    val stream = EnglishSpamFilter.getClass.getResourceAsStream("/EnglishSpamFilter.dat")
    val inObject = new ObjectInputStream(stream)
    inObject.readObject.asInstanceOf[EnglishSpamFilter]
  }

  def main(args: Array[String]) {
    new EnglishSpamFilter().train("D:\\SpamData\\smsspamcollection\\SMSSpamCollection", "EnglishSpamFilter.dat")
  }
}