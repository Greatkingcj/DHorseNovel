package com.squirrel.base.help.book

import com.squirrel.base.data.entities.ReplaceRule

data class BookContent(
    val sameTitleRemoved: Boolean,
    val textList: List<String>,
    //起效的替换规则
    val effectiveReplaceRules: List<ReplaceRule>?
) {

    override fun toString(): String {
        return textList.joinToString("\n")
    }

}
