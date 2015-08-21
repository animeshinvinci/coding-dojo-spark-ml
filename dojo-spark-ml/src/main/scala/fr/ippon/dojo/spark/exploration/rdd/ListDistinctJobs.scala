package fr.ippon.dojo.spark.exploration.rdd

import org.apache.spark.{SparkConf, SparkContext}

object ListDistinctJobs extends App {

  val conf = new SparkConf()
    .setMaster("local[*]")
    .setAppName("list-distinct-jobs")
  val sc = new SparkContext(conf)

  // - load the CSV file ("src/main/resources/bank-full.csv")
  // val lines = sc...

  // - skip the header line

  // - extract the job column

  // - put the jobs in a tuple with a dummy value

  // - use a reduce-by-key operation to remove duplicates

  // - print the results

}
