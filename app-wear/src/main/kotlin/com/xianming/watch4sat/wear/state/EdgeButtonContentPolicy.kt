package com.xianming.watch4sat.wear.state

enum class EdgeButtonContentType {
    Text,
    Icon
}

enum class EdgeButtonContent {
    Text,
    Apply,
    Clear,
    Track
}

enum class EdgeButtonIcon {
    Check,
    DeleteSweep,
    TrackChanges
}

object EdgeButtonContentPolicy {
    fun contentType(content: EdgeButtonContent): EdgeButtonContentType {
        return if (content == EdgeButtonContent.Text) {
            EdgeButtonContentType.Text
        } else {
            EdgeButtonContentType.Icon
        }
    }

    fun icon(content: EdgeButtonContent): EdgeButtonIcon? {
        return when (content) {
            EdgeButtonContent.Text -> null
            EdgeButtonContent.Apply -> EdgeButtonIcon.Check
            EdgeButtonContent.Clear -> EdgeButtonIcon.DeleteSweep
            EdgeButtonContent.Track -> EdgeButtonIcon.TrackChanges
        }
    }

    fun usesOfficialIcon(content: EdgeButtonContent): Boolean =
        contentType(content) == EdgeButtonContentType.Icon
}
