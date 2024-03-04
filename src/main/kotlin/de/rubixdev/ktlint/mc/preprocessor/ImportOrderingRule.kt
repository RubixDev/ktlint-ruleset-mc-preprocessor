/**
 * Modified from <https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/rules/ImportOrderingRule.kt>
 *
 * import-ordering rule modified to support preprocessor comments in the import list and automatically put them at the
 * end, separate from the other imports.
 */

package de.rubixdev.ktlint.mc.preprocessor

import com.pinterest.ktlint.rule.engine.core.api.ElementType
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleId
import com.pinterest.ktlint.rule.engine.core.api.isWhiteSpace
import org.jetbrains.kotlin.com.intellij.lang.ASTNode
import org.jetbrains.kotlin.com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.com.intellij.psi.impl.source.tree.PsiWhiteSpaceImpl
import org.jetbrains.kotlin.psi.psiUtil.leaves

class ImportOrderingRule : Rule(ruleId = RuleId("$CUSTOM_RULE_SET_ID:import-ordering"), about = ABOUT) {
    companion object {
        const val ERROR_UNSORTED_IMPORTS =
            "Imports must be ordered in lexicographic order without empty lines" +
                "in-between with version-specific preprocessor imports in the end and separated from the rest"
        const val ERROR_CANNOT_AUTOCORRECT =
            ERROR_UNSORTED_IMPORTS +
                "-- no autocorrection due to non-preprocessor comments in the import list"
    }

    override fun beforeVisitChildNodes(
        node: ASTNode,
        autoCorrect: Boolean,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ) {
        if (node.elementType == ElementType.IMPORT_LIST) {
            val children = node.allImportNodes()
            if (children.isNotEmpty()) {
                // Get unique imports and blank lines
                val (autoCorrectDuplicateImports: Boolean, importGroups: List<List<ASTNode>>, imports: List<ASTNode>) =
                    getImportGroups(children, emit)

                val hasComments =
                    children.any {
                        it.elementType == ElementType.BLOCK_COMMENT
                            || (it.elementType == ElementType.EOL_COMMENT && !it.isPreprocessorComment())
                    }
                val sortedImports =
                    importGroups
                        .asSequence()
                        // sort groups
                        .sortedWith { a, b ->
                            val aVersion = a.getPreprocMinMcVersion()
                            val bVersion = b.getPreprocMinMcVersion()
                            if (aVersion == null && bVersion == null) {
                                val aIsPreproc = a.firstOrNull()?.isPreprocessorStartComment() == true
                                val bIsPreproc = b.firstOrNull()?.isPreprocessorStartComment() == true
                                if (aIsPreproc && bIsPreproc) {
                                    0
                                } else if (!aIsPreproc) {
                                    -1
                                } else {
                                    1
                                }
                            } else if (aVersion == null) {
                                -1
                            } else if (bVersion == null) {
                                1
                            } else {
                                aVersion.compareTo(bVersion)
                            }
                        }
                        // remove whitespace tokens
                        .map { group -> group.filter { it.elementType == ElementType.IMPORT_DIRECTIVE || it.isPreprocessorComment() } }
                        // sort imports per group
                        .map { group ->
                            // TODO: sort preproc imports (might be difficult with nested ifs)
                            if (group.firstOrNull()?.isPreprocessorStartComment() == true) {
                                group
                            } else {
                                // TODO: more than just alphabetical sorting?
                                group.sortedBy { it.text }
                            }
                        }
                        // add newline after each import
                        .map { group ->
                            group.toList().run {
                                flatMapIndexed { idx, node ->
                                    if (idx != lastIndex) listOf(node, PsiWhiteSpaceImpl("\n")) else listOf(node)
                                }
                            }
                        }
                        // add blank line after each group
                        .toList()
                        .run {
                            flatMapIndexed { idx, group ->
                                group.toMutableList().apply {
                                    if (idx != this@run.lastIndex) add(PsiWhiteSpaceImpl("\n\n"))
                                }
                            }
                        }

                if (hasComments) {
                    emit(children.first().startOffset, ERROR_CANNOT_AUTOCORRECT, false)
                } else {
                    val autoCorrectSortOrder = !importsAreEqual(imports, sortedImports)
                    if (autoCorrectSortOrder) {
                        emit(children.first().startOffset, ERROR_UNSORTED_IMPORTS, true)
                    }
                    if (autoCorrect && (autoCorrectDuplicateImports || autoCorrectSortOrder)) {
                        // leading preprocessor comments
                        children.takeWhile { it.treeParent === node.treeParent }.apply {
                            if (isNotEmpty()) node.treeParent.removeRange(first(), last().treeNext)
                        }
                        // trailing preprocessor comments
                        children.takeLastWhile { it.treeParent === node.treeParent }.apply {
                            if (isNotEmpty()) node.treeParent.removeRange(first(), last().treeNext)
                        }
                        // actual import list
                        node.removeRange(node.firstChildNode, node.lastChildNode.treeNext)
                        sortedImports.forEach { node.addChild(it, null) }
                    }
                }
            }
        }
    }

    private fun ASTNode.allImportNodes(): List<ASTNode> =
        leaves(forward = false).takeWhile { it.isWhiteSpace() || it.isPreprocessorComment() }.toMutableList().apply {
            reverse()
            addAll(getChildren(null))
            addAll(leaves().takeWhile { it.isWhiteSpace() || it.isPreprocessorComment() })
        }.dropLastWhile { it.isWhiteSpace() }.dropWhile { it.isWhiteSpace() }

    private fun getImportGroups(
        nodes: List<ASTNode>,
        emit: (offset: Int, errorMessage: String, canBeAutoCorrected: Boolean) -> Unit,
    ): Triple<Boolean, List<List<ASTNode>>, List<ASTNode>> {
        var autoCorrectDuplicateImports = false
        val imports = mutableListOf<ASTNode>()
        val importGroups = mutableListOf<MutableList<ASTNode>>()
        val mainGroup = mutableListOf<ASTNode>()
        var currentGroup = mainGroup
        val importTextSet = mutableSetOf<String>()
        var nestedIfCount = 0

        for (node in nodes) {
            when {
                node.isWhiteSpace() && node.text.count { it == '\n' } >= 1 -> {
                    imports += node
                    currentGroup += node
                }
                node.isPreprocessorComment() -> {
                    when {
                        node.isPreprocessorStartComment() -> {
                            if (nestedIfCount == 0) currentGroup = mutableListOf()
                            nestedIfCount++
                            imports += node
                            currentGroup += node
                        }
                        node.isPreprocessorEndComment() -> {
                            nestedIfCount--
                            imports += node
                            currentGroup += node
                            if (nestedIfCount == 0) {
                                importGroups += currentGroup
                                currentGroup = mainGroup
                            }
                        }
                        else -> {
                            imports += node
                            currentGroup += node
                        }
                    }
                }
                node.elementType == ElementType.IMPORT_DIRECTIVE -> {
                    if (importTextSet.add(node.text)) {
                        imports += node
                        currentGroup += node
                    } else {
                        emit(node.startOffset, "Duplicate '${node.text}' found", true)
                        autoCorrectDuplicateImports = true
                    }
                }
            }
        }
        importGroups += mainGroup

        return Triple(autoCorrectDuplicateImports, importGroups, imports)
    }

    private fun importsAreEqual(
        actual: List<ASTNode>,
        expected: List<ASTNode>,
    ): Boolean {
        if (actual.size != expected.size) return false

        val combined = actual.zip(expected)
        return combined.all { (first, second) ->
            if (first is PsiWhiteSpace && second is PsiWhiteSpace) {
                return@all (first as PsiWhiteSpace).text == (second as PsiWhiteSpace).text
            }
            return@all first == second
        }
    }

    private fun ASTNode.isPreprocessorStartComment(): Boolean = elementType == ElementType.EOL_COMMENT && text.startsWith("//#if")

    private fun ASTNode.isPreprocessorEndComment(): Boolean = elementType == ElementType.EOL_COMMENT && text.startsWith("//#endif")

    private fun ASTNode.isPreprocessorComment(): Boolean =
        isPreprocessorStartComment()
            || isPreprocessorEndComment()
            || (elementType == ElementType.EOL_COMMENT && (text.startsWith("//#else") || text.startsWith("//$$")))

    // TODO: also compare based on operator (`>=`, `>`, `<=`, `<`)
    private fun List<ASTNode>.getPreprocMinMcVersion(): Int? =
        firstOrNull()?.let { node ->
            if (!node.isPreprocessorStartComment()) return@let null
            node.text.removePrefix("//#if").split(" ").filter { it.isNotBlank() }.let { split ->
                if (split.firstOrNull() == "MC") {
                    split.lastOrNull()?.toIntOrNull()
                } else {
                    null
                }
            }
        }
}
