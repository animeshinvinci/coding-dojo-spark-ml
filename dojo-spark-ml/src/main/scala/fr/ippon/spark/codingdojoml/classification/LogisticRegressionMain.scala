package fr.ippon.spark.codingdojoml.classification

import java.sql.Date

import org.apache.spark.ml.{PipelineModel, Pipeline}
import org.apache.spark.ml.classification.LogisticRegression
import org.apache.spark.ml.evaluation.BinaryClassificationEvaluator
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.ml.tuning.{ParamGridBuilder, CrossValidator}
import org.apache.spark.sql.SQLContext
import org.apache.spark.sql.types.IntegerType
import org.apache.spark.{SparkConf, SparkContext}
import org.joda.time.format.{DateTimeFormatter, DateTimeFormat}
import org.joda.time.{Years, Period, DateTime}

/**
 * User: ludochane
 */
object LogisticRegressionMain {

  def main(args: Array[String]) {
    val conf = new SparkConf()
      .setAppName("Spark coding dojo classification with LogisticRegression")
      .setMaster("local[*]")

    val sc = new SparkContext(conf)

    val sqlContext = new SQLContext(sc)

    val df = sqlContext.read.format("com.databricks.spark.csv")
      .option("header", "true")
      .option("delimiter", ";")
      .option("inferSchema", "true")
      .load("src/main/resources/classification/bank-full-birthdate.csv")

    df.printSchema()
    df.show(10)

    // feature engineering
    // Calculate age from birthdate
    import org.apache.spark.sql.functions._
    val now = new DateTime()
    val birthToAge: (String) => Int = birthDate => {
      val formatter = DateTimeFormat.forPattern("yyyy-MM-dd")
      Years.yearsBetween(formatter.parseDateTime(birthDate), now).getYears
    }
    val dfAge = df.withColumn("age", callUDF(birthToAge, IntegerType, df("birth_date")))

    // Use StringIndexer to convert String columns to Double columns
    val dfJobIndexed = new StringIndexer().setInputCol("job").setOutputCol("jobIndex").fit(dfAge).transform(dfAge)
    val dfMaritalIndexed = new StringIndexer().setInputCol("marital").setOutputCol("maritalIndex").fit(dfJobIndexed).transform(dfJobIndexed)
    val dfEducationIndexer = new StringIndexer().setInputCol("education").setOutputCol("educationIndex").fit(dfMaritalIndexed).transform(dfMaritalIndexed)
    val dfDefaultIndexer = new StringIndexer().setInputCol("default").setOutputCol("defaultIndex").fit(dfEducationIndexer).transform(dfEducationIndexer)
    val dfHousingIndexer = new StringIndexer().setInputCol("housing").setOutputCol("housingIndex").fit(dfDefaultIndexer).transform(dfDefaultIndexer)
    val dfLoanIndexer = new StringIndexer().setInputCol("loan").setOutputCol("loanIndex").fit(dfHousingIndexer).transform(dfHousingIndexer)
    val dfYIndexer = new StringIndexer().setInputCol("y").setOutputCol("label").fit(dfLoanIndexer).transform(dfLoanIndexer)

    // machine learning
    // We separate our data : trainSet = 75% of our data, validationSet = 25% of our data
    val Array(trainSet, validationSet) = dfYIndexer.randomSplit(Array(0.75, 0.25))

    // Pipeline
    val vectorAssembler = new VectorAssembler()
      .setInputCols(Array("age", "jobIndex", "maritalIndex", "educationIndex", "defaultIndex", "housingIndex", "loanIndex", "duration"))
      .setOutputCol("features")
    val logisticRegression = new LogisticRegression()

    val pipeline = new Pipeline().setStages(Array(
      vectorAssembler,
      logisticRegression
    ))

    // We will cross validate our pipeline
    val crossValidator = new CrossValidator()
      .setEstimator(pipeline)
      .setEvaluator(new BinaryClassificationEvaluator)

    // Here are the params we want to validationPredictions
    val paramGrid = new ParamGridBuilder()
      .addGrid(logisticRegression.regParam, Array(1, 0.1, 0.01))
      .addGrid(logisticRegression.maxIter, Array(10, 50, 100))
      .build()
    crossValidator.setEstimatorParamMaps(paramGrid)

    // We will use a 3-fold cross validation
    crossValidator.setNumFolds(3)

    println("Cross Validation")
    val cvModel = crossValidator.fit(trainSet)

    println("Best model")
    for (stage <- cvModel.bestModel.asInstanceOf[PipelineModel].stages) println(stage.explainParams())

    println("Evaluate the model on the validation set.")
    val validationPredictions = cvModel.transform(validationSet)

    // We want to print the percentage of passengers we correctly predict on the validation set
    val total = validationPredictions.count()
    val goodPredictionCount = validationPredictions.filter(validationPredictions("label") === validationPredictions("prediction")).count()
    println(s"correct prediction percentage : ${goodPredictionCount / total.toDouble}")
  }
}
