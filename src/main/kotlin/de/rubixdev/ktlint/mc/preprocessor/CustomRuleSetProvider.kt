package de.rubixdev.ktlint.mc.preprocessor

import com.pinterest.ktlint.cli.ruleset.core.api.RuleSetProviderV3
import com.pinterest.ktlint.rule.engine.core.api.Rule
import com.pinterest.ktlint.rule.engine.core.api.RuleProvider
import com.pinterest.ktlint.rule.engine.core.api.RuleSetId

internal const val CUSTOM_RULE_SET_ID = "mc-preprocessor"
internal val ABOUT =
    Rule.About(
        maintainer = "RubixDev",
        repositoryUrl = "https://github.com/RubixDev/ktlint-ruleset-mc-preprocessor/",
        issueTrackerUrl = "https://github.com/RubixDev/ktlint-ruleset-mc-preprocessor/issues",
    )

class CustomRuleSetProvider : RuleSetProviderV3(RuleSetId(CUSTOM_RULE_SET_ID)) {
    override fun getRuleProviders(): Set<RuleProvider> =
        setOf(
            RuleProvider { ChainWrappingRule() },
            RuleProvider { CommentSpacingRule() },
        )
}
