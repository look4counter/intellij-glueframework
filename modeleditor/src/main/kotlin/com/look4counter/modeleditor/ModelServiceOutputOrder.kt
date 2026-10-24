package com.look4counter.modeleditor

object ModelServiceOutputOrder {
    fun transitions(values: List<Pair<String, String>>): List<Pair<String, String>> {
        val byCondition = HashMap<String, String>()
        values.forEach { (condition, target) -> byCondition[condition.ifBlank { "success" }] = target }
        return byCondition.entries.map { it.key to it.value }
    }

    fun properties(values: List<ActivityProperty>): List<ActivityProperty> {
        // Count metadata too: removing it first changes the legacy bucket order.
        val bucketCount = values.size.toLong() * 2 + 1
        return values.sortedBy { (it.key.hashCode().toLong() and 0x7fffffffL) % bucketCount }
            .filter { it.key != "class" && it.key != "description" }
    }
}
