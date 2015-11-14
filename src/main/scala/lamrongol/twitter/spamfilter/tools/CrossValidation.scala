package lamrongol.twitter.spamfilter.tools

import org.apache.spark.mllib.classification.SVMWithSGD
import org.apache.spark.mllib.regression.LabeledPoint
import org.apache.spark.mllib.util.MLUtils
import org.apache.spark.rdd.RDD

/**
  * Created by admin on 2015/10/16.
  */
object CrossValidation {
  /** K-fold cross-validation の K に相当します */
  val NUM_FOLDS = 5

  def printSVMEvaluations(dataSet: RDD[LabeledPoint]) = {
    // 訓練データとテストデータの組を NUM_FOLDS 個用意します (K-fold cross-validation)
    val foldedDataSet = MLUtils.kFold(dataSet, NUM_FOLDS, System.currentTimeMillis().toInt)

    val result = foldedDataSet.map { case (trainingSet, testingSet) =>
      // モデルを構築します
      val model = SVMWithSGD.train(trainingSet, 100)

      // テストデータので分類をしつつ、その結果を評価します
      //0にならないように
      testingSet.map { t =>
        val predicted = model.predict(t.features)
        //println(predicted, t.label)

        (predicted, t.label) match {
          case (1, 1) => "TP"
          case (1, _) => "FP"
          case (_, 1) => "FN"
          case (_, _) => "TN"
        }
      }.countByValue()
    }

    // 精度を算出します
    val totalCount = dataSet.count()

    val truePositiveCount = result.map(_.getOrElse("TP", 0L)).reduce(_ + _).toDouble
    val trueNegativeCount = result.map(_.getOrElse("TN", 0L)).reduce(_ + _).toDouble
    val falsePositiveCount = result.map(_.getOrElse("FP", 0L)).reduce(_ + _).toDouble
    val falseNegativeCount = result.map(_.getOrElse("FN", 0L)).reduce(_ + _).toDouble

    val positiveCount = truePositiveCount + falseNegativeCount
    val negativeCount = falsePositiveCount + trueNegativeCount

    val accuracy = (truePositiveCount + trueNegativeCount) / totalCount
    val precision = truePositiveCount / (truePositiveCount + falsePositiveCount)
    val recall = truePositiveCount / positiveCount
    val falsePositiveRate = falsePositiveCount / (falsePositiveCount + trueNegativeCount)
    val falseNegativeRate = falseNegativeCount / (falseNegativeCount + truePositiveCount)

    println("Basic info: ")
    println("positive:negative rate = " + positiveCount / totalCount + ":" + negativeCount / totalCount)
    println()

    println("accuracy: " + accuracy)
    println("precision: " + precision) //陽性と判断したもののうち実際に陽性だった割合
    println("recall: " + recall) //陽性であるもののうち陽性と捕捉できた割合
    println("F-measure: " + 2 / (1 / recall + 1 / precision)) //precisionとrecallの調和平均
    println("false positive rate: " + falsePositiveRate) //誤って陽性と判断した割合
    println("false negative rate: " + falseNegativeRate) //誤って陰性と判断した割合
  }


}
