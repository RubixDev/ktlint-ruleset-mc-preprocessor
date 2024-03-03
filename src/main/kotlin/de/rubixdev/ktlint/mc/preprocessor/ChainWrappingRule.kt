/**
 * Modified from <https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/rules/ChainWrappingRule.kt>
 *
 * chain-wrapping rule modified to place supported operators (`*`, `/`, `%`, `&&`, `||`) on start of next line instead
 * of end of previous line. `+` and `-` are kept at end of line, see <https://github.com/pinterest/ktlint/issues/163#issuecomment-369418775>
 * for why.
 */

package de.rubixdev.ktlint.mc.preprocessor

import com.pinterest.ktlint.rule.engine.core.api.*
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ANDAND
import com.pinterest.ktlint.rule.engine.core.api.ElementType.COMMA
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DIV
import com.pinterest.ktlint.rule.engine.core.api.ElementType.DOT
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELSE_KEYWORD
import com.pinterest.ktlint.rule.engine.core.api.ElementType.ELVIS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.IMPORT_DIRECTIVE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LBRACE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.LPAR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MINUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.MUL
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OPERATION_REFERENCE
import com.pinterest.ktlint.rule.engine.core.api.ElementType.OROR
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PERC
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PLUS
import com.pinterest.ktlint.rule.engine.core.api.ElementType.PREFIX_EXPRESSION
import com.pinterest.ktlint.rule.engine.core.api.ElementType.SAFE_ACCESS
import com.pinterest.ktlint.rule.engine.core.api.IndentConfig.Companion.DEFAULT_INDENT_CONFIG
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.EditorConfig
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_SIZE_PROPERTY
import com.pinterest.ktlint.rule.engine.core.api.editorconfig.INDENT_STYLE_PROPERTY
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafElement
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.psiUtil.leaves

class ChainWrappingRule :
    Rule(
        ruleId = RuleId("$CUSTOM_RULE_SET_ID:chain-wrapping"),
        about = ABOUT,
        usesEditorConfigProperties =
            setOf(
                INDENT_SIZE_PROPERTY,
                INDENT_STYLE_PROPERTY,
            ),
    ) {
    private val sameLineTokens = TokenSet.create()
    private val prefixTokens = TokenSet.create(PLUS, MINUS)
    private val nextLineTokens = TokenSet.create(DOT, SAFE_ACCESS, ELVIS, MUL, DIV, PERC, ANDAND, OROR)
    private val spaceAroundTokens = TokenSet.create(ELVIS, MUL, DIV, PERC, ANDAND, OROR)
    private var indentConfig = DEFAULT_INDENT_CONFIG

    override fun beforeFirstNode(editorConfig: EditorConfig) {
        indentConfig =
            IndentConfig(
                indentStyle = editorConfig[INDENT_STYLE_PROPERTY],
                tabWidth = editorConfig[INDENT_SIZE_PROPERTY],
            )
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        /*
           org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.LeafPsiElement (DOT) | "."
           org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl (WHITE_SPACE) | "\n        "
           org.jetbrains.kotlin.psi.KtCallExpression (CALL_EXPRESSION)
         */
        val elementType = node.elementType
        if (nextLineTokens.contains(elementType)) {
            if (node.isPartOfComment()) {
                return
            }
            val nextLeaf = node.nextCodeLeaf()?.prevLeaf()
            if (nextLeaf.isWhiteSpaceWithNewline() && !node.isElvisOperatorAndComment() && !node.isWildcardImport()) {
                emit(node.startOffset, "Line must not end with \"${node.text}\"", true)
                if (autoCorrect) {
                    // rewriting
                    // <prevLeaf><node="."><nextLeaf="\n"> to
                    // <prevLeaf><delete space if any><nextLeaf="\n"><node="."><space if needed>
                    // (or)
                    // <prevLeaf><node="."><spaceBeforeComment><comment><nextLeaf="\n"> to
                    // <prevLeaf><delete space if any><spaceBeforeComment><comment><nextLeaf="\n"><node="."><space if needed>
                    if (node.elementType == ELVIS) {
                        node.upsertWhitespaceBeforeMe(indentConfig.childIndentOf(node))
                        node.upsertWhitespaceAfterMe(" ")
                    } else {
                        val prevLeaf = node.prevLeaf()
                        if (spaceAroundTokens.contains(node.elementType) && prevLeaf?.isWhiteSpace() == true) {
                            prevLeaf.treeParent.removeChild(prevLeaf)
                        }
                        node.treeParent.removeChild(node)
                        (nextLeaf as LeafElement).rawInsertAfterMe(node as LeafElement)
                        if (spaceAroundTokens.contains(node.elementType)) {
                            node.upsertWhitespaceAfterMe(" ")
                        }
                    }
                }
            }
        } else if (sameLineTokens.contains(elementType) || prefixTokens.contains(elementType)) {
            if (node.isPartOfComment()) {
                return
            }
            val prevLeaf = node.prevLeaf()
            if (node.isPartOfSpread()) {
                // Allow:
                //    fn(
                //        *typedArray<...>()
                //    )
                return
            }
            if (prefixTokens.contains(elementType) && node.isInPrefixPosition()) {
                // Allow:
                //    fn(
                //        -42
                //    )
                return
            }

            if (prevLeaf != null && prevLeaf.isWhiteSpaceWithNewline()) {
                emit(node.startOffset, "Line must not begin with \"${node.text}\"", true)
                if (autoCorrect) {
                    // rewriting
                    // <insertionPoint><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                    // <insertionPoint><prevLeaf=" "><node="&&"><nextLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                    // (or)
                    // <insertionPoint><spaceBeforeComment><comment><prevLeaf="\n"><node="&&"><nextLeaf=" "> to
                    // <insertionPoint><space if needed><node="&&"><spaceBeforeComment><comment><prevLeaf="\n"><delete node="&&"><delete nextLeaf=" ">
                    val nextLeaf = node.nextLeaf()
                    val whiteSpaceToBeDeleted =
                        when {
                            nextLeaf.isWhiteSpaceWithNewline() -> {
                                // Node is preceded and followed by whitespace. Prefer to remove the whitespace before the node as this will
                                // change the indent of the next line
                                prevLeaf
                            }

                            nextLeaf.isWhiteSpaceWithoutNewline() -> nextLeaf

                            else -> null
                        }

                    if (node.treeParent.elementType == OPERATION_REFERENCE) {
                        val operationReference = node.treeParent
                        val insertBeforeSibling =
                            operationReference
                                .prevCodeSibling()
                                ?.nextSibling()
                        operationReference.treeParent.removeChild(operationReference)
                        insertBeforeSibling?.treeParent?.addChild(operationReference, insertBeforeSibling)
                        node.treeParent.upsertWhitespaceBeforeMe(" ")
                    } else {
                        val insertionPoint = prevLeaf.prevCodeLeaf() as LeafPsiElement
                        (node as LeafPsiElement).treeParent.removeChild(node)
                        insertionPoint.rawInsertAfterMe(node)
                        (insertionPoint as ASTNode).upsertWhitespaceAfterMe(" ")
                    }
                    whiteSpaceToBeDeleted
                        ?.treeParent
                        ?.removeChild(whiteSpaceToBeDeleted)
                }
            }
        }
    }

    private fun ASTNode.isPartOfSpread() =
        elementType == MUL
            && prevCodeLeaf()
                ?.let { leaf ->
                    val type = leaf.elementType
                    type == LPAR
                        || type == COMMA
                        || type == LBRACE
                        || type == ELSE_KEYWORD
                        || KtTokens.OPERATIONS.contains(type)
                } == true

    private fun ASTNode.isInPrefixPosition() = treeParent?.treeParent?.elementType == PREFIX_EXPRESSION

    private fun ASTNode.isElvisOperatorAndComment(): Boolean =
        elementType == ELVIS
            && leaves().takeWhile { it.isWhiteSpaceWithoutNewline() || it.isPartOfComment() }.any()

    private fun ASTNode.isWildcardImport(): Boolean = elementType == MUL && treeParent.elementType == IMPORT_DIRECTIVE
}
