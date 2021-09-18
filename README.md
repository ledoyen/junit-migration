# junit-migration
![Build](https://github.com/ledoyen/junit-migration/workflows/Build/badge.svg)
[![Coverage Status](https://codecov.io/gh/ledoyen/junit-migration/branch/main/graph/badge.svg)](https://codecov.io/gh/ledoyen/junit-migration/)
[![License](https://img.shields.io/github/license/fridujo/spring-automocker.svg)](https://opensource.org/licenses/Apache-2.0)

POC tool to migrate tests from JUnit4 to JUnit5 (Jupiter)

A maintained alternative is [OpenRewrite](https://docs.openrewrite.org) (and more specifically [Migrate to JUnit 5 from JUnit 4](https://docs.openrewrite.org/tutorials/migrate-from-junit-4-to-junit-5)).

## :warning: Disclaimer
Initially I developed a Java project to operate a migration of a lot of JUnit4 test classes for my current client (https://github.com/mirakl).

The code was done following the following guidelines that I inflicted to myself:
* I should spend the less time possible doing it as it will serve once and be garbage collected
* It should work on > 95% of classes to migrate
* The resulting change must be minimal in order for it to be "easily" reviewable

Once the migration was over, I though of talking about it in a JUG meeting

What you have here is a few hours attempt to make the core concept a little more sound by recreating the tool from scratch, but "happens" to work on simple enough test classes.

Having to wrap a lot of elements from [javaparser](https://github.com/javaparser/javaparser) I choose to make the second version in Kotlin for conciseness purpose.

So to sum up, this is a POC, nothing more, not destined to be used on real projects but rather be an inspiration to create something tuned to your needs.

Now that this is clear, the fun part :fire:

## Architecture

As said above, changes produced have to be minimal, however serializing a javaparser-AST will result in breaking the original format leading to unwanted changes and lose of information.

So javaparser will only be used to read code, and the tool will write the changes.

To do this I choose to store `Change` objects (`Deletion`, `Replacement` & `Addition`) with coordinates (line & column) instead of changing the AST.

The tool is composed on two major parts :
* `MinimalDiffParser`: a parser on top of javaparser that exposes a *wrapped-AST* that appears to be manipulable (but in fact creates `Change` objects)
* the transformations: visitors of the *wrapped-AST* with simple purposes, such as replacing the `expected` member of the JUnit4 `@Test` by an [AssertJ](https://github.com/assertj/assertj-core) assertion

The modified code is produced by applying these changes.

In order for them not to interfere with each other, assuming that there is no overlap between them, changes are stored and applied in reversed positional order.

## Perspectives

Do not hesitate to make PRs on this project to implement more behaviors and/or fix bugs at long as you follow the *mimetic rule*.

Who knows, maybe someday it will help a project move from JUnit 4 to 5 !
