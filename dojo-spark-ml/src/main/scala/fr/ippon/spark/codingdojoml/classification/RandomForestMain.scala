package fr.ippon.spark.codingdojoml.classification

import org.apache.spark.ml.Pipeline
import org.apache.spark.ml.classification.RandomForestClassifier
import org.apache.spark.ml.feature.{StringIndexer, VectorAssembler}
import org.apache.spark.sql.SQLContext
import org.apache.spark.{SparkConf, SparkContext}

/**
  * User: ludochane
  */
object RandomForestMain {

   def main(args: Array[String]) {
     val conf = new SparkConf()
       .setAppName("Spark coding dojo classification with a RandomForest")
       .setMaster("local[*]")

     val sc = new SparkContext(conf)

     val sqlContext = new SQLContext(sc)

     val df = sqlContext.read.format("com.databricks.spark.csv")
       .option("header", "true")
       .option("delimiter", ";")
       .option("inferSchema", "true")
       .load("src/main/resources/classification/bank-full.csv")

     // printSchema
     df.printSchema()

     // show sample data
     df.show(10)

     // feature engineering


     // machine learning
     // We separate our data : trainSet = 75% of our data, validationSet = 25% of our data
     val Array(trainSet, validationSet) = df.randomSplit(Array(0.75, 0.25))

     // Pipeline
     val jobIndexer = new StringIndexer().setInputCol("job").setOutputCol("jobIndex")
     val maritalIndexer = new StringIndexer().setInputCol("marital").setOutputCol("maritalIndex")
     val educationIndexer = new StringIndexer().setInputCol("education").setOutputCol("educationIndex")
     val defaultIndexer = new StringIndexer().setInputCol("default").setOutputCol("defaultIndex")
     val housingIndexer = new StringIndexer().setInputCol("housing").setOutputCol("housingIndex")
     val loanIndexer = new StringIndexer().setInputCol("loan").setOutputCol("loanIndex")
     val yIndexer = new StringIndexer().setInputCol("y").setOutputCol("label")
     val vectorAssembler = new VectorAssembler()
       .setInputCols(Array("age", "jobIndex", "maritalIndex", "educationIndex", "defaultIndex", "housingIndex", "loanIndex", "duration"))
       .setOutputCol("features")
     val randomForest = new RandomForestClassifier()

     val pipeline = new Pipeline().setStages(Array(
       jobIndexer,
       maritalIndexer,
       educationIndexer,
       defaultIndexer,
       housingIndexer,
       loanIndexer,
       yIndexer,
       vectorAssembler,
       randomForest
     ))

     println("Train pipeline")
     val fitModel = pipeline.fit(trainSet)

     println("Evaluate the model on the validation set.")
     val validationPredictions = fitModel.transform(validationSet)

     // We want to print the percentage of passengers we correctly predict on the validation set
     val total = validationPredictions.count()
     val goodPredictionCount = validationPredictions.filter(validationPredictions("label") === validationPredictions("prediction")).count()
     println(s"correct prediction percentage : ${goodPredictionCount / total.toDouble}")
   }
 }
