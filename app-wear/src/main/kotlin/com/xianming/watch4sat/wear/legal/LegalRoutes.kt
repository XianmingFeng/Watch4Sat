package com.xianming.watch4sat.wear.legal

object LegalRoutes {
    const val DocumentIdArg = "documentId"
    const val DocumentPattern = "settings/legal/document/{$DocumentIdArg}"

    fun document(document: LegalDocument): String = "settings/legal/document/${document.id}"
}
