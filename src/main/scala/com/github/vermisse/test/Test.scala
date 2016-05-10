package com.github.vermisse.test

import org.apache.lucene.analysis._, org.apache.lucene.analysis.standard._, org.apache.lucene.store._
import java.nio.file._
import org.apache.lucene.index._, org.apache.lucene.document._, org.apache.lucene.search._
import org.apache.lucene.queryparser.classic._

/**
 * @author vermisse
 */
object Test {
  def main(arr: Array[String]) {
    val analyzer: Analyzer = new StandardAnalyzer

    //将索引存储到内存中
    val directory: Directory = new RAMDirectory
    //如下想把索引存储到硬盘上，使用下面的代码代替
    //val directory:Directory = FSDirectory.open(Paths.get("/tmp/testindex"))
    val config: IndexWriterConfig = new IndexWriterConfig(analyzer)
    val iwriter: IndexWriter = new IndexWriter(directory, config)

    val texts = Array("Mybatis分页插件 - 示例",
      "Mybatis 贴吧问答 第一期",
      "Mybatis 示例之 复杂(complex)属性(property)",
      "Mybatis极其(最)简(好)单(用)的一个分页插件",
      "Mybatis 的Log4j日志输出问题 - 以及有关日志的所有问题",
      "Mybatis 示例之 foreach （下）",
      "Mybatis 示例之 foreach （上）",
      "Mybatis 示例之 SelectKey",
      "Mybatis 示例之 Association (2)",
      "Mybatis 示例之 Association")

    texts.foreach {
      x =>
        val doc: Document = new Document
        doc.add(new Field("fieldname", x, TextField.TYPE_STORED))
        iwriter.addDocument(doc)
    }
    iwriter.close

    //读取索引并查询
    val ireader: DirectoryReader = DirectoryReader.open(directory)
    val isearcher: IndexSearcher = new IndexSearcher(ireader)
    //解析一个简单的查询
    val parser: QueryParser = new QueryParser("fieldname", analyzer)
    val query: Query = parser.parse("foreach")
    val hits: Array[ScoreDoc] = isearcher.search(query, 1000).scoreDocs
    //迭代输出结果
    0 to hits.length - 1 map {
      i =>
        val hitDoc: Document = isearcher.doc(hits(i).doc)
        println(hitDoc.get("fieldname"))
    }
    ireader.close
    directory.close
  }
}