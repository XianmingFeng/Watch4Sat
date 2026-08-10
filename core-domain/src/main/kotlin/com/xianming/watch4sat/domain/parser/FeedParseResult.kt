package com.xianming.watch4sat.domain.parser

data class FeedValidationIssue(
    val field: String,
    val message: String
)

data class FeedRecordRejection(
    val recordNumber: Int,
    val reasons: List<FeedValidationIssue>
)

data class FeedParseResult<T>(
    val records: List<T>,
    val inputRecordCount: Int,
    val ignoredRecordCount: Int = 0,
    val rejections: List<FeedRecordRejection> = emptyList(),
    val duplicateStableIds: Set<String> = emptySet(),
    val syntaxErrors: List<String> = emptyList()
) {
    val acceptedRecordCount: Int
        get() = records.size

    val rejectedRecordCount: Int
        get() = rejections.size
}
