package com.kousuan.helper

import java.util.regex.Pattern

object KousuanSolver {

    /**
     * 解析识别文本并计算比大小结果 (">", "<", "=") 或四则运算结果
     */
    fun solve(rawText: String): String? {
        if (rawText.isBlank()) return null

        val text = rawText.replace("\\s+".toRegex(), "")
            .replace("o", "0", ignoreCase = true)
            .replace("l", "1", ignoreCase = true)
            .replace("I", "1")
            .replace("x", "×", ignoreCase = true)

        // 1. 匹配两个数字比大小 (例如 "5?8", "12?9", "5 8", "14 O 18")
        val matcher = Pattern.compile("(\\d+)[^0-9]?(\\d+)").matcher(text)
        if (matcher.find()) {
            val num1 = matcher.group(1)?.toLongOrNull()
            val num2 = matcher.group(2)?.toLongOrNull()
            if (num1 != null && num2 != null) {
                return when {
                    num1 > num2 -> ">"
                    num1 < num2 -> "<"
                    else -> "="
                }
            }
        }

        // 2. 匹配加减四则运算 (如 5+8=, 19-7=)
        val mathMatcher = Pattern.compile("(\\d+)([\\+\\-\\×\\÷])(\\d+)").matcher(text)
        if (mathMatcher.find()) {
            val n1 = mathMatcher.group(1)?.toIntOrNull()
            val op = mathMatcher.group(2)
            val n2 = mathMatcher.group(3)?.toIntOrNull()
            if (n1 != null && n2 != null && op != null) {
                return when (op) {
                    "+" -> (n1 + n2).toString()
                    "-" -> (n1 - n2).toString()
                    "×" -> (n1 * n2).toString()
                    "÷" -> if (n2 != 0) (n1 / n2).toString() else null
                    else -> null
                }
            }
        }

        return null
    }
}
