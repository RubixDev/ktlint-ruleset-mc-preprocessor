/**
 * Modified from <https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/rules/CommentSpacingRule.kt>
 *
 * comment-spacing rule modified to add additional exceptions for comments starting with any of the following:
 *
 * - `///`
 * - `//#`
 * - `//$`
 */

package de.rubixdev.ktlint.mc.preprocessor

import com.pinterest.ktlint.rule.engine.core.api.ElementType.EOL_COMMENT
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.prevLeaf
import com.pinterest.ktlint.rule.engine.core.api.upsertWhitespaceBeforeMe
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement

class CommentSpacingRule : Rule(ruleId = RuleId("$CUSTOM_RULE_SET_ID:comment-spacing"), about = ABOUT) {
    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == EOL_COMMENT) {
            val prevLeaf = node.prevLeaf()
            if (prevLeaf !is PsiWhiteSpace && prevLeaf is LeafPsiElement) {
                emit(node.startOffset, "Missing space before //", true)
                if (autoCorrect) {
                    node.upsertWhitespaceBeforeMe(" ")
                }
            }
            val text = node.text
            if (text.length != 2
                && !text.startsWith("// ")
                && !text.startsWith("//noinspection")
                && !text.startsWith("//region")
                && !text.startsWith("//endregion")
                && !text.startsWith("//language=")
                && !text.startsWith("///")
                && !text.startsWith("//#")
                && !text.startsWith("//$")
            ) {
                emit(node.startOffset, "Missing space after //", true)
                if (autoCorrect) {
                    (node as LeafPsiElement).rawReplaceWithText("// " + text.removePrefix("//"))
                }
            }
        }
    }
}
