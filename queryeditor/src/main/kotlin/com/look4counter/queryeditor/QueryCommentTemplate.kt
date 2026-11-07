package com.look4counter.queryeditor

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object QueryCommentTemplate {
    private val variable = Regex("[${'$'}#]\\{([^{}]+)}")
    private val datePattern = Regex("[yMdHhmsSa\\s/.:,_+\\-]+")

    fun prepend(template: String, sql: String, queryId: String, description: String,
                now: LocalDateTime = LocalDateTime.now(), resultType: String = ""): String {
        if (template.isBlank()) return sql
        val normalized = template.replace("\r\n", "\n").replace('\r', '\n')
        val comment = variable.replace(normalized) { match ->
            val key = match.groupValues[1]
            when (key) {
                "QUERY_ID" -> queryId
                "QUERY_DESCRIPTION" -> description
                "QUERY_RESULT_TYPE" -> resultType
                else -> if (datePattern.matches(key) && key.any { it in "yMdHhmsS" }) {
                    runCatching { now.format(DateTimeFormatter.ofPattern(key)) }.getOrDefault(match.value)
                } else match.value
            }
        }
        return comment + (if (comment.endsWith('\n')) "" else "\n") + sql
    }
}
