/**
 * Modified from <https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/test/kotlin/com/pinterest/ktlint/ruleset/standard/rules/CommentSpacingRuleTest.kt>
 *
 * comment-spacing rule modified to add additional exceptions for comments starting with any of the following:
 *
 * - `///`
 * - `//#`
 * - `//$`
 */

package de.rubixdev.ktlint.mc.preprocessor

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import org.junit.jupiter.api.Test

class CommentSpacingRuleTest {
    private val commentSpacingRuleAssertThat = assertThatRule { CommentSpacingRule() }

    @Test
    fun testLintValidCommentSpacing() {
        val code =
            """
            //
            //noinspection AndroidLintRecycle
            //region
            //endregion
            //language=SQL
            ////// Title //////
            //#if MC < 12004
            //$$ isOld = true
            //#endif
            // comment
            var debugging = false // comment
            var debugging = false // comment//word
                // comment
            """.trimIndent()
        commentSpacingRuleAssertThat(code).hasNoLintViolations()
    }

    @Test
    fun testFormatInvalidCommentSpacing() {
        val code =
            """
            //comment
            var debugging = false// comment
            var debugging = false //comment
            var debugging = false//comment
            fun main() {
                System.out.println(//123
                    "test"
                )
            }
                //comment
            """.trimIndent()
        val formattedCode =
            """
            // comment
            var debugging = false // comment
            var debugging = false // comment
            var debugging = false // comment
            fun main() {
                System.out.println( // 123
                    "test"
                )
            }
                // comment
            """.trimIndent()
        commentSpacingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(1, 1, "Missing space after //"),
                LintViolation(2, 22, "Missing space before //"),
                LintViolation(3, 23, "Missing space after //"),
                LintViolation(4, 22, "Missing space before //"),
                LintViolation(4, 22, "Missing space after //"),
                LintViolation(6, 24, "Missing space before //"),
                LintViolation(6, 24, "Missing space after //"),
                LintViolation(10, 5, "Missing space after //"),
            ).isFormattedAs(formattedCode)
    }
}
