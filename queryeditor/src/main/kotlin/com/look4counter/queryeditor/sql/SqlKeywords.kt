package com.look4counter.queryeditor.sql

import java.util.Locale

internal object SqlKeywords {
    val words: Set<String> = requireNotNull(javaClass.getResourceAsStream("/sql/dbeaver-keywords.txt")) {
        "DBeaver SQL keyword resource is missing"
    }.bufferedReader(Charsets.UTF_8).useLines { lines ->
        lines.map(String::trim).filter { it.isNotEmpty() && !it.startsWith('#') }
            .map { it.uppercase(Locale.ROOT) }.toSortedSet()
    }
    val hyphenated: List<String> = words.filter { '-' in it }.sortedByDescending(String::length)
}
