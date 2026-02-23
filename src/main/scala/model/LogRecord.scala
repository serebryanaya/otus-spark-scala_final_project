//package model
//
//import org.apache.spark.sql.types._
//import java.sql.{Timestamp, Date}
//import java.time.LocalDateTime
//import java.time.format.DateTimeFormatter
//
//case class LogRecord(
//                      timestamp: String,
//                      level: String,
//                      component: String,
//                      request_id: String,
//                      instance_id: String,
//                      message: String,
//                      processing_time: Timestamp,
//                      is_error: Boolean,
//                      is_warning: Boolean,
//                      hour: Int,
//                      date: Date
//                    ) extends Serializable
//
//object LogRecord {
//
//  def schema: StructType = StructType(Array(
//    StructField("timestamp", StringType, true),
//    StructField("level", StringType, true),
//    StructField("component", StringType, true),
//    StructField("request_id", StringType, true),
//    StructField("instance_id", StringType, true),
//    StructField("message", StringType, true),
//    StructField("processing_time", TimestampType, true),
//    StructField("is_error", BooleanType, true),
//    StructField("is_warning", BooleanType, true),
//    StructField("hour", IntegerType, true),
//    StructField("date", DateType, true)
//  ))
//
//  def fromLogLine(line: LogLine): LogRecord = {
//    val now = new Timestamp(System.currentTimeMillis())
//    val isError = line.level == "ERROR"
//    val isWarning = line.level == "WARNING"
//
//    var hour = 0
//    var date: Date = null
//
//    if (line.timestamp != null) {
//      try {
//        val parsed = LocalDateTime.parse(line.timestamp,
//          DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"))
//        hour = parsed.getHour
//        date = Date.valueOf(parsed.toLocalDate)
//      } catch {
//        case _: Exception => // ignore
//      }
//    }
//
//    LogRecord(
//      timestamp = line.timestamp,
//      level = line.level,
//      component = line.component,
//      request_id = line.request_id,
//      instance_id = line.instance_id,
//      message = line.raw_message,
//      processing_time = now,
//      is_error = isError,
//      is_warning = isWarning,
//      hour = hour,
//      date = date
//    )
//  }
//}
//
//case class LogLine(
//                    timestamp: String,
//                    level: String,
//                    component: String,
//                    request_id: String,
//                    instance_id: String,
//                    raw_message: String
//                  ) extends Serializable