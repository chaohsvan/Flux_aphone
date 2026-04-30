package com.example.flux.feature.widget

object Labels {
    const val TODO = "\u5f85\u529e"
    const val NO_TODO = "\u4eca\u5929\u5f88\u6e05\u723d\uff0c\u6ca1\u6709\u5f85\u529e"
    val WEEK_DAYS = listOf("\u65e5", "\u4e00", "\u4e8c", "\u4e09", "\u56db", "\u4e94", "\u516d")

    fun todoListSubtitle(visibleCount: Int, pendingCount: Int): String {
        return if (visibleCount >= pendingCount) {
            "\u5168\u90e8\u672a\u5b8c\u6210"
        } else {
            "\u663e\u793a $visibleCount / $pendingCount"
        }
    }
}
