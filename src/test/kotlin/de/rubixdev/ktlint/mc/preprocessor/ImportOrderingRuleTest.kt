package de.rubixdev.ktlint.mc.preprocessor

import com.pinterest.ktlint.test.KtLintAssertThat.Companion.assertThatRule
import com.pinterest.ktlint.test.LintViolation
import de.rubixdev.ktlint.mc.preprocessor.ImportOrderingRule.Companion.ERROR_CANNOT_AUTOCORRECT
import de.rubixdev.ktlint.mc.preprocessor.ImportOrderingRule.Companion.ERROR_UNSORTED_IMPORTS
import org.junit.jupiter.api.Test

class ImportOrderingRuleTest {
    private val importOrderingRuleAssertThat = assertThatRule { ImportOrderingRule() }

    @Test
    fun `Given correctly sorted imports`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import de.rubixdev.enchantedshulkers.enchantment.*
            import net.minecraft.registry.Registries
            import net.minecraft.registry.tag.TagKey
            import net.minecraft.util.Identifier
            import org.slf4j.Logger
            import org.slf4j.LoggerFactory

            //#if MC < 12002
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if MC < 12004
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if MC >= 12001
            import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#else
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given incorrectly sorted imports`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            """.trimIndent()
        val formattedCode =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given incorrectly sorted imports with preprocessor comments`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import de.rubixdev.enchantedshulkers.enchantment.*
            import net.minecraft.registry.Registries


            import net.minecraft.registry.tag.TagKey
            //#if MC < 12002
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            import net.minecraft.util.Identifier
            import org.slf4j.Logger
            //#if MC >= 12001
            import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#else
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            import org.slf4j.LoggerFactory

            //#if MC < 12004
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            """.trimIndent()
        val formattedCode =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import de.rubixdev.enchantedshulkers.enchantment.*
            import net.minecraft.registry.Registries
            import net.minecraft.registry.tag.TagKey
            import net.minecraft.util.Identifier
            import org.slf4j.Logger
            import org.slf4j.LoggerFactory

            //#if MC < 12002
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if MC < 12004
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if MC >= 12001
            import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#else
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given too many blank lines`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand

            import de.rubixdev.enchantedshulkers.config.WorldConfig
            """.trimIndent()
        val formattedCode =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given imports with more code after it`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.WorldConfig

            import de.rubixdev.enchantedshulkers.config.ConfigCommand

            class Test {}
            """.trimIndent()
        val formattedCode =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig

            class Test {}
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given incorrectly sorted imports with EOL comments`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.WorldConfig // some comment
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            """.trimIndent()
        val formattedCode =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig // some comment
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given incorrectly sorted imports with other line comments`() {
        val code =
            """
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            // some comment
            import a
            /* some block comment */
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolationWithoutAutoCorrect(1, 1, ERROR_CANNOT_AUTOCORRECT)
    }

    @Test
    fun `Given duplicate imports`() {
        val code =
            """

            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import a
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            """.trimIndent()
        val formattedCode =
            """

            import a
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolations(
                LintViolation(2, 1, ERROR_UNSORTED_IMPORTS),
                LintViolation(5, 1, "Duplicate 'import de.rubixdev.enchantedshulkers.config.ConfigCommand' found"),
            )
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given incorrectly sorted imports with preprocessor comments and custom values`() {
        val code =
            """
            //#if FABRIC < 1
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import de.rubixdev.enchantedshulkers.enchantment.*
            import net.minecraft.registry.Registries


            import net.minecraft.registry.tag.TagKey
            //#if MC < 12002
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            import net.minecraft.util.Identifier
            import org.slf4j.Logger
            //#if FABRIC >= 2
            import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#else
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            import org.slf4j.LoggerFactory

            //#if MC < 12004
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            """.trimIndent()
        val formattedCode =
            """
            import de.rubixdev.enchantedshulkers.config.ConfigCommand
            import de.rubixdev.enchantedshulkers.config.WorldConfig
            import de.rubixdev.enchantedshulkers.enchantment.*
            import net.minecraft.registry.Registries
            import net.minecraft.registry.tag.TagKey
            import net.minecraft.util.Identifier
            import org.slf4j.Logger
            import org.slf4j.LoggerFactory

            //#if FABRIC < 1
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if FABRIC >= 2
            import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#else
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if MC < 12002
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif

            //#if MC < 12004
            //$$ import eu.pb4.polymer.networking.api.PolymerServerNetworking
            //#endif
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given incorrectly sorted preprocessor comments`() {
        val code =
            """
            //#if MC >= 12005
            //#endif
            //#if MC >= 12002
            //#endif
            //#if MC >= 12001
            //#endif
            //#if MC > 12004
            //#endif
            //#if MC >= 12004
            //#endif
            //#if MC <= 12004
            //#endif
            //#if MC <= 12003
            //#endif
            //#if MC < 12004
            //#endif
            //#if FABRIC < 2
            //#endif
            //#if FABRIC >= 2
            //#endif
            import something
            """.trimIndent()
        val formattedCode =
            """
            import something

            //#if FABRIC < 2
            //#endif

            //#if FABRIC >= 2
            //#endif

            //#if MC < 12004
            //#endif

            //#if MC <= 12003
            //#endif

            //#if MC <= 12004
            //#endif

            //#if MC >= 12001
            //#endif

            //#if MC >= 12002
            //#endif

            //#if MC >= 12004
            //#endif

            //#if MC > 12004
            //#endif

            //#if MC >= 12005
            //#endif
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasLintViolation(1, 1, ERROR_UNSORTED_IMPORTS)
            .isFormattedAs(formattedCode)
    }

    @Test
    fun `Given file with just preproc`() {
        val code =
            """
            //#if MC < 12006
            //$$ import stuff;
            //#endif
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasNoLintViolations()
    }

    @Test
    fun `Given non-import preproc comment right after imports`() {
        val code =
            """
            //#if MC >= 12006
            import stuff;
            //#endif

            //#if MC >= 12006
            class Test {
            //#else
            //$$ else Test2 {
            //#endif
            }
            """.trimIndent()
        importOrderingRuleAssertThat(code)
            .hasNoLintViolations()
    }
}
