package com.appprobe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.appprobe.testing.TargetApp
import com.appprobe.ui.AppDiscoveryScreen
import com.appprobe.ui.AppDiscoveryViewModel
import com.appprobe.ui.AppInspectionScreen
import com.appprobe.ui.AppInspectionViewModel
import com.appprobe.ui.ScenarioBuilderScreen
import com.appprobe.ui.ScenarioBuilderViewModel
import com.appprobe.ui.TargetAppState
import com.appprobe.ui.TargetAppViewModel
import com.appprobe.ui.TargetReadyScreen

class MainActivity : ComponentActivity() {

    private val discoveryViewModel: AppDiscoveryViewModel by viewModels {
        AppDiscoveryViewModel.Factory(application)
    }

    private val targetViewModel: TargetAppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                val targetState by targetViewModel.targetState.collectAsState()
                val currentTarget = (targetState as? TargetAppState.Ready)?.target

                var currentScreen by rememberSaveable { mutableStateOf<String>("discovery") }
                var inspectionPackage by rememberSaveable { mutableStateOf<String?>(null) }

                when (currentScreen) {
                    "discovery" -> {
                        AppDiscoveryScreen(
                            viewModel = discoveryViewModel,
                            currentTarget = currentTarget,
                            onTargetClick = {
                                currentScreen = "target_ready"
                            },
                            onAppClick = { packageName ->
                                inspectionPackage = packageName
                                currentScreen = "inspection"
                            }
                        )
                    }

                    "inspection" -> {
                        val pkg = inspectionPackage
                        if (pkg != null) {
                            val inspectionViewModel: AppInspectionViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel(
                                    key = pkg,
                                    factory = AppInspectionViewModel.Factory(application, pkg)
                                )
                            AppInspectionScreen(
                                viewModel = inspectionViewModel,
                                onBack = {
                                    currentScreen = "discovery"
                                },
                                onSelectTarget = { target ->
                                    targetViewModel.confirmTarget(target)
                                    currentScreen = "target_ready"
                                }
                            )
                        } else {
                            currentScreen = "discovery"
                        }
                    }

                    "target_ready" -> {
                        if (currentTarget != null) {
                            TargetReadyScreen(
                                target = currentTarget,
                                onBack = {
                                    currentScreen = "discovery"
                                },
                                onInspect = {
                                    inspectionPackage = currentTarget.packageName
                                    currentScreen = "inspection"
                                },
                                onConfigureScenario = {
                                    currentScreen = "scenario_builder"
                                },
                                onChangeTarget = {
                                    currentScreen = "discovery"
                                }
                            )
                        } else {
                            currentScreen = "discovery"
                        }
                    }

                    "scenario_builder" -> {
                        if (currentTarget != null) {
                            val scenarioViewModel: ScenarioBuilderViewModel =
                                androidx.lifecycle.viewmodel.compose.viewModel(
                                    key = "scenario_${currentTarget.packageName}",
                                    factory = ScenarioBuilderViewModel.Factory(currentTarget)
                                )
                            ScenarioBuilderScreen(
                                viewModel = scenarioViewModel,
                                onBack = {
                                    currentScreen = "target_ready"
                                }
                            )
                        } else {
                            currentScreen = "discovery"
                        }
                    }
                }
            }
        }
    }
}
