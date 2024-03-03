# Custom ktlint Rule Set for Minecraft Preprocessor Mods

This is a custom rule set for [ktlint](https://ktlint.github.io/) which modifies
some standard rules to better work with Kotlin code that makes use of
[preprocessor](https://github.com/ReplayMod/preprocessor).

## Included Rules

- [`mc-preprocessor:chain-wrapping`](./src/main/kotlin/de/rubixdev/ktlint/mc/preprocessor/ChainWrappingRule.kt)
  - based on
    [`standard:chain-wrapping`](https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/rules/ChainWrappingRule.kt)
  - modified to move the operators `*`, `/`, `%`, `&&`, and `||` on the start of
    the next line instead of at the end of the current line
  - `+` and `-` operators are kept at the end of lines because
    [Kotlin's parsing is a bit annoying there](https://github.com/pinterest/ktlint/issues/163#issuecomment-369418775)
  - this is just my preference and has nothing to do with the preprocessor

- [`mc-preprocessor:comment-spacing`](./src/main/kotlin/de/rubixdev/ktlint/mc/preprocessor/CommentSpacingRule.kt)
  - based on
    [`standard:comment-spacing`](https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/rules/CommentSpacingRule.kt)
  - modified to allow comments starting with `//#`, `//$` or `///` to not have
    spaces after them

## TODO

- `mc-preprocessor:import-ordering`
  - based on
    [`standard:import-ordering`](https://github.com/pinterest/ktlint/blob/c4788a5f581f0d9f85ca47296b6b576fd6b5d594/ktlint-ruleset-standard/src/main/kotlin/com/pinterest/ktlint/ruleset/standard/rules/ImportOrderingRule.kt)
  - modified to sort imports alphabetically, but respects preprocessor comments
    for version-specific imports and puts those separate and at the end but
    before `static` and `java` imports
    [as specified in their docs](https://github.com/ReplayMod/preprocessor#the-preprocessor).
    Also sorts the preprocessor imports based on their minimum Minecraft version
    and alphabetically per preprocessor block.
  - custom import ordering patterns cannot be specified with this version of the
    rule
