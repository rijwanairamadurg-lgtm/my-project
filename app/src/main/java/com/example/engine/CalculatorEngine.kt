package com.example.engine

import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import java.util.Stack

object CalculatorEngine {

    private val MATH_CONTEXT = MathContext(16, RoundingMode.HALF_UP)

    sealed class CalculationResult {
        data class Success(val value: BigDecimal, val formatted: String) : CalculationResult()
        data class Error(val message: String) : CalculationResult()
    }

    /**
     * Evaluates a mathematical expression string.
     * @param expression The mathematical expression (can contain +, -, *, /, ×, ÷, %, ^, (, ), numbers)
     * @param isBodmas Whether to use standard operator precedence (BODMAS) or sequential left-to-right calculation.
     */
    fun evaluate(expression: String, isBodmas: Boolean = true): CalculationResult {
        if (expression.isBlank()) {
            return CalculationResult.Error("Empty expression")
        }

        val sanitized = sanitizeExpression(expression)
        if (sanitized.isBlank()) {
            return CalculationResult.Error("Invalid input")
        }

        return try {
            if (isBodmas) {
                evaluateBodmas(sanitized)
            } else {
                evaluateSequential(sanitized)
            }
        } catch (e: ArithmeticException) {
            CalculationResult.Error(e.message ?: "Calculation error")
        } catch (e: Exception) {
            CalculationResult.Error("Invalid expression")
        }
    }

    private fun sanitizeExpression(expr: String): String {
        return expr
            .replace("×", "*")
            .replace("÷", "/")
            .replace("−", "-")
            .replace("—", "-")
            .replace(",", "")
            .replace(" ", "")
    }

    // Token representation
    private sealed class Token {
        data class Number(val value: BigDecimal) : Token()
        data class Operator(val op: Char) : Token()
        data object OpenBracket : Token()
        data object CloseBracket : Token()
        data object Percent : Token()
    }

    private fun tokenize(expr: String): List<Token> {
        val tokens = mutableListOf<Token>()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]

            when {
                c.isDigit() || c == '.' -> {
                    val sb = StringBuilder()
                    while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                        sb.append(expr[i])
                        i++
                    }
                    val numStr = sb.toString()
                    val value = if (numStr == ".") BigDecimal.ZERO else BigDecimal(numStr)
                    tokens.add(Token.Number(value))
                    continue
                }
                c == '+' || c == '*' || c == '/' || c == '^' -> {
                    tokens.add(Token.Operator(c))
                }
                c == '-' -> {
                    // Check if unary minus
                    val isUnary = tokens.isEmpty() ||
                            tokens.last() is Token.Operator ||
                            tokens.last() is Token.OpenBracket

                    if (isUnary) {
                        // Lookahead for number
                        i++
                        val sb = StringBuilder("-")
                        while (i < len && (expr[i].isDigit() || expr[i] == '.')) {
                            sb.append(expr[i])
                            i++
                        }
                        if (sb.length > 1) {
                            val numStr = sb.toString()
                            val value = if (numStr == "-.") BigDecimal.ZERO else BigDecimal(numStr)
                            tokens.add(Token.Number(value))
                            continue
                        } else {
                            // It was just an operator or invalid, put operator
                            tokens.add(Token.Operator('-'))
                            continue
                        }
                    } else {
                        tokens.add(Token.Operator('-'))
                    }
                }
                c == '%' -> {
                    tokens.add(Token.Percent)
                }
                c == '(' -> {
                    // Handle implicit multiplication: e.g. 5(2) -> 5*(2)
                    if (tokens.isNotEmpty() && (tokens.last() is Token.Number || tokens.last() is Token.CloseBracket || tokens.last() is Token.Percent)) {
                        tokens.add(Token.Operator('*'))
                    }
                    tokens.add(Token.OpenBracket)
                }
                c == ')' -> {
                    tokens.add(Token.CloseBracket)
                }
            }
            i++
        }

        return tokens
    }

    private fun evaluateBodmas(expr: String): CalculationResult {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return CalculationResult.Error("Empty expression")

        // Preprocess percentages in context of addition/subtraction/multiplication:
        // E.g. [Num(100), Op(+), Num(10), Percent] -> [Num(100), Op(+), Num(10)] where 10% of 100 is 10
        val processedTokens = processPercentages(tokens)

        // Shunting-Yard Algorithm to convert to Postfix (RPN)
        val outputQueue = mutableListOf<Token>()
        val operatorStack = Stack<Token>()

        fun precedence(op: Char): Int = when (op) {
            '^' -> 4
            '*', '/' -> 3
            '+', '-' -> 2
            else -> 0
        }

        for (token in processedTokens) {
            when (token) {
                is Token.Number -> outputQueue.add(token)
                is Token.Percent -> {
                    // Solo percent: divide top output number by 100
                    if (outputQueue.isNotEmpty() && outputQueue.last() is Token.Number) {
                        val lastNum = (outputQueue.removeAt(outputQueue.size - 1) as Token.Number).value
                        outputQueue.add(Token.Number(lastNum.divide(BigDecimal(100), MATH_CONTEXT)))
                    }
                }
                is Token.OpenBracket -> operatorStack.push(token)
                is Token.CloseBracket -> {
                    var foundOpen = false
                    while (operatorStack.isNotEmpty()) {
                        val top = operatorStack.pop()
                        if (top is Token.OpenBracket) {
                            foundOpen = true
                            break
                        } else {
                            outputQueue.add(top)
                        }
                    }
                    if (!foundOpen) {
                        return CalculationResult.Error("Mismatched parentheses")
                    }
                }
                is Token.Operator -> {
                    while (operatorStack.isNotEmpty()) {
                        val top = operatorStack.peek()
                        if (top is Token.Operator &&
                            (precedence(top.op) > precedence(token.op) ||
                                    (precedence(top.op) == precedence(token.op) && token.op != '^'))
                        ) {
                            outputQueue.add(operatorStack.pop())
                        } else {
                            break
                        }
                    }
                    operatorStack.push(token)
                }
            }
        }

        while (operatorStack.isNotEmpty()) {
            val top = operatorStack.pop()
            if (top is Token.OpenBracket || top is Token.CloseBracket) {
                return CalculationResult.Error("Mismatched parentheses")
            }
            outputQueue.add(top)
        }

        // Evaluate RPN
        val evalStack = Stack<BigDecimal>()

        for (token in outputQueue) {
            when (token) {
                is Token.Number -> evalStack.push(token.value)
                is Token.Percent -> {
                    if (evalStack.isNotEmpty()) {
                        val v = evalStack.pop()
                        evalStack.push(v.divide(BigDecimal(100), MATH_CONTEXT))
                    }
                }
                is Token.Operator -> {
                    if (evalStack.size < 2) {
                        return CalculationResult.Error("Invalid syntax")
                    }
                    val b = evalStack.pop()
                    val a = evalStack.pop()
                    val res = applyOp(a, b, token.op)
                    evalStack.push(res)
                }
                else -> {}
            }
        }

        if (evalStack.size != 1) {
            return CalculationResult.Error("Invalid expression")
        }

        val resultVal = evalStack.pop()
        return CalculationResult.Success(resultVal, formatResult(resultVal))
    }

    private fun processPercentages(tokens: List<Token>): List<Token> {
        val result = mutableListOf<Token>()
        var i = 0
        while (i < tokens.size) {
            val current = tokens[i]
            if (i >= 2 && current is Token.Percent && tokens[i - 1] is Token.Number) {
                val percentNum = (tokens[i - 1] as Token.Number).value
                val prevOpToken = tokens.getOrNull(i - 2)

                if (prevOpToken is Token.Operator && (prevOpToken.op == '+' || prevOpToken.op == '-')) {
                    // Look back for the base number if simple
                    val baseNumToken = tokens.getOrNull(i - 3)
                    if (baseNumToken is Token.Number) {
                        // Calculate percentage of base
                        val percentageOfBase = baseNumToken.value
                            .multiply(percentNum, MATH_CONTEXT)
                            .divide(BigDecimal(100), MATH_CONTEXT)
                        // Replace the last number in result
                        result.removeAt(result.size - 1)
                        result.add(Token.Number(percentageOfBase))
                        i++
                        continue
                    }
                }
                // Otherwise normal percentage = num / 100
                result.removeAt(result.size - 1)
                result.add(Token.Number(percentNum.divide(BigDecimal(100), MATH_CONTEXT)))
                i++
                continue
            }
            result.add(current)
            i++
        }
        return result
    }

    /**
     * Sequential Left-to-Right evaluation (Non-BODMAS).
     * Example: 10 + 5 * 2 = 30.
     */
    private fun evaluateSequential(expr: String): CalculationResult {
        val tokens = tokenize(expr)
        if (tokens.isEmpty()) return CalculationResult.Error("Empty expression")

        var currentTotal: BigDecimal? = null
        var pendingOp: Char? = null

        var i = 0
        while (i < tokens.size) {
            val token = tokens[i]
            when (token) {
                is Token.Number -> {
                    var numVal = token.value
                    // Check if followed by percent
                    if (i + 1 < tokens.size && tokens[i + 1] is Token.Percent) {
                        if (currentTotal != null && (pendingOp == '+' || pendingOp == '-')) {
                            numVal = currentTotal.multiply(numVal, MATH_CONTEXT).divide(BigDecimal(100), MATH_CONTEXT)
                        } else {
                            numVal = numVal.divide(BigDecimal(100), MATH_CONTEXT)
                        }
                        i++ // skip percent
                    }

                    if (currentTotal == null) {
                        currentTotal = numVal
                    } else if (pendingOp != null) {
                        currentTotal = applyOp(currentTotal, numVal, pendingOp)
                        pendingOp = null
                    } else {
                        currentTotal = numVal
                    }
                }
                is Token.Operator -> {
                    pendingOp = token.op
                }
                is Token.Percent -> {
                    if (currentTotal != null) {
                        currentTotal = currentTotal.divide(BigDecimal(100), MATH_CONTEXT)
                    }
                }
                is Token.OpenBracket -> {
                    // For brackets in sequential mode, evaluate sub-expression inside brackets first
                    var depth = 1
                    val subTokens = mutableListOf<Token>()
                    i++
                    while (i < tokens.size && depth > 0) {
                        if (tokens[i] is Token.OpenBracket) depth++
                        else if (tokens[i] is Token.CloseBracket) {
                            depth--
                            if (depth == 0) break
                        }
                        subTokens.add(tokens[i])
                        i++
                    }
                    val subExpr = subTokens.joinToString("") { tokenToString(it) }
                    val subRes = evaluateSequential(subExpr)
                    if (subRes is CalculationResult.Error) return subRes
                    val subVal = (subRes as CalculationResult.Success).value

                    if (currentTotal == null) {
                        currentTotal = subVal
                    } else if (pendingOp != null) {
                        currentTotal = applyOp(currentTotal, subVal, pendingOp)
                        pendingOp = null
                    } else {
                        currentTotal = subVal
                    }
                }
                is Token.CloseBracket -> {}
            }
            i++
        }

        return if (currentTotal != null) {
            CalculationResult.Success(currentTotal, formatResult(currentTotal))
        } else {
            CalculationResult.Error("Invalid expression")
        }
    }

    private fun tokenToString(token: Token): String {
        return when (token) {
            is Token.Number -> token.value.stripTrailingZeros().toPlainString()
            is Token.Operator -> token.op.toString()
            is Token.OpenBracket -> "("
            is Token.CloseBracket -> ")"
            is Token.Percent -> "%"
        }
    }

    private fun applyOp(a: BigDecimal, b: BigDecimal, op: Char): BigDecimal {
        return when (op) {
            '+' -> a.add(b, MATH_CONTEXT)
            '-' -> a.subtract(b, MATH_CONTEXT)
            '*' -> a.multiply(b, MATH_CONTEXT)
            '/' -> {
                if (b.compareTo(BigDecimal.ZERO) == 0) {
                    throw ArithmeticException("Cannot divide by zero")
                }
                a.divide(b, MATH_CONTEXT)
            }
            '^' -> {
                // Integer power or double power
                try {
                    val intVal = b.intValueExact()
                    if (intVal in -1000..1000) {
                        a.pow(intVal, MATH_CONTEXT)
                    } else {
                        BigDecimal(Math.pow(a.toDouble(), b.toDouble()), MATH_CONTEXT)
                    }
                } catch (e: Exception) {
                    BigDecimal(Math.pow(a.toDouble(), b.toDouble()), MATH_CONTEXT)
                }
            }
            else -> a
        }
    }

    fun formatResult(value: BigDecimal, precision: Int = 8): String {
        // Strip trailing zeros and remove unnecessary scientific notation for reasonable numbers
        val stripped = value.stripTrailingZeros()
        val plain = stripped.toPlainString()

        // If number is huge (> 1e12 or < 1e-6 and not zero), use scientific notation
        val absVal = value.abs()
        if (absVal > BigDecimal("1000000000000") || (absVal < BigDecimal("0.000001") && absVal > BigDecimal.ZERO)) {
            val symbols = DecimalFormatSymbols(Locale.US)
            val df = DecimalFormat("0.######E0", symbols)
            return df.format(value)
        }

        // Split integer and decimal parts
        val parts = plain.split(".")
        val intPart = parts[0]
        val formattedInt = formatIndianNumber(intPart)

        return if (parts.size > 1) {
            var decPart = parts[1]
            if (precision in 1..10 && decPart.length > precision) {
                decPart = decPart.substring(0, precision)
            }
            "$formattedInt.$decPart"
        } else {
            formattedInt
        }
    }

    /**
     * Formats integer part using Indian comma system (e.g. 12,34,567).
     */
    fun formatIndianNumber(numberStr: String): String {
        val isNegative = numberStr.startsWith("-")
        val clean = if (isNegative) numberStr.substring(1) else numberStr

        if (clean.length <= 3) {
            return if (isNegative) "-$clean" else clean
        }

        val lastThree = clean.substring(clean.length - 3)
        val remaining = clean.substring(0, clean.length - 3)

        val sb = StringBuilder()
        var count = 0
        for (i in remaining.length - 1 downTo 0) {
            sb.append(remaining[i])
            count++
            if (count % 2 == 0 && i != 0) {
                sb.append(",")
            }
        }

        val formattedRemaining = sb.reverse().toString()
        val result = "$formattedRemaining,$lastThree"
        return if (isNegative) "-$result" else result
    }

    /**
     * Formats numbers inside an active mathematical expression with Indian commas (e.g. 1,00,000 + 5,000).
     */
    fun formatExpressionWithIndianCommas(expr: String): String {
        if (expr.isEmpty()) return ""
        val sb = StringBuilder()
        var i = 0
        val len = expr.length

        while (i < len) {
            val c = expr[i]
            if (c.isDigit()) {
                val start = i
                while (i < len && expr[i].isDigit()) {
                    i++
                }
                val intStr = expr.substring(start, i)
                sb.append(formatIndianNumber(intStr))

                // If followed by decimal point and decimal digits
                if (i < len && expr[i] == '.') {
                    sb.append('.')
                    i++
                    while (i < len && expr[i].isDigit()) {
                        sb.append(expr[i])
                        i++
                    }
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
    }

    /**
     * Maps an offset in a raw expression to the corresponding offset in the formatted (with commas) expression.
     */
    fun mapRawOffsetToFormatted(rawExpr: String, rawOffset: Int): Int {
        val clampedRaw = rawOffset.coerceIn(0, rawExpr.length)
        val prefix = rawExpr.substring(0, clampedRaw)
        return formatExpressionWithIndianCommas(prefix).length
    }

    /**
     * Maps an offset in the formatted expression back to the raw unformatted expression offset.
     */
    fun mapFormattedOffsetToRaw(rawExpr: String, formattedOffset: Int): Int {
        val formattedFull = formatExpressionWithIndianCommas(rawExpr)
        val clampedFormatted = formattedOffset.coerceIn(0, formattedFull.length)

        var bestRaw = 0
        for (r in 0..rawExpr.length) {
            val fLen = formatExpressionWithIndianCommas(rawExpr.substring(0, r)).length
            if (fLen <= clampedFormatted) {
                bestRaw = r
            } else {
                break
            }
        }
        return bestRaw
    }
}
