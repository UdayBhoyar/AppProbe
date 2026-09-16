package com.appprobe.testing

/**
 * Model representing a configured test scenario for a target application.
 * Contains the scenario name, target package identifier, and ordered actions.
 */
data class TestScenario(
    val name: String,
    val targetPackage: String,
    val actions: List<TestAction> = emptyList()
)

