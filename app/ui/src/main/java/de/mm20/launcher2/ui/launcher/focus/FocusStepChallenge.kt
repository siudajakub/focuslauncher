package de.mm20.launcher2.ui.launcher.focus

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import de.mm20.launcher2.services.focus.FocusStepChallengeState
import de.mm20.launcher2.ui.R
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun FocusStepChallenge(
    targetSteps: Int,
    onComplete: () -> Unit,
    onUseDelay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val sensorManager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val stepSensor = remember(sensorManager) {
        sensorManager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
    }
    var permissionGranted by remember {
        mutableStateOf(
            Build.VERSION.SDK_INT < Build.VERSION_CODES.Q ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.ACTIVITY_RECOGNITION) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { permissionGranted = it }
    var completedSteps by rememberSaveable(targetSteps) { mutableIntStateOf(0) }
    var lastStepAtMillis by rememberSaveable(targetSteps) { mutableLongStateOf(0L) }
    var lastCounterValue by remember { mutableFloatStateOf(Float.NaN) }
    var nowMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val state = FocusStepChallengeState(
        completedSteps = completedSteps,
        targetSteps = targetSteps,
        lastStepAtMillis = lastStepAtMillis.takeIf { it > 0L },
    )

    DisposableEffect(permissionGranted, stepSensor, targetSteps) {
        if (!permissionGranted || stepSensor == null || state.isComplete) {
            onDispose { }
        } else {
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    val addedSteps = if (event.sensor.type == Sensor.TYPE_STEP_DETECTOR) {
                        event.values.firstOrNull()?.roundToInt()?.coerceAtLeast(1) ?: 1
                    } else {
                        val currentValue = event.values.firstOrNull() ?: return
                        if (lastCounterValue.isNaN()) {
                            lastCounterValue = currentValue
                            return
                        }
                        val delta = (currentValue - lastCounterValue).roundToInt().coerceAtLeast(0)
                        lastCounterValue = currentValue
                        delta
                    }
                    if (addedSteps > 0) {
                        completedSteps = (completedSteps + addedSteps).coerceAtMost(targetSteps)
                        lastStepAtMillis = System.currentTimeMillis()
                        nowMillis = lastStepAtMillis
                    }
                }

                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
            }
            sensorManager.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_NORMAL)
            onDispose { sensorManager.unregisterListener(listener) }
        }
    }

    LaunchedEffect(lastStepAtMillis, completedSteps) {
        while (completedSteps in 1 until targetSteps) {
            nowMillis = System.currentTimeMillis()
            delay(1_000L)
        }
    }
    LaunchedEffect(state.isComplete) {
        if (state.isComplete) onComplete()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(
            text = stringResource(R.string.focus_gate_challenge_title),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = stringResource(R.string.focus_gate_steps_description, targetSteps),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!permissionGranted) {
            Text(
                text = stringResource(R.string.focus_gate_steps_permission),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        permissionLauncher.launch(Manifest.permission.ACTIVITY_RECOGNITION)
                    } else {
                        permissionGranted = true
                    }
                },
            ) {
                Text(stringResource(R.string.focus_gate_steps_allow))
            }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = onUseDelay) {
                Text(stringResource(R.string.focus_gate_steps_use_delay))
            }
        } else if (stepSensor == null) {
            Text(
                text = stringResource(R.string.focus_gate_steps_sensor_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FilledTonalButton(modifier = Modifier.fillMaxWidth(), onClick = onUseDelay) {
                Text(stringResource(R.string.focus_gate_steps_use_delay))
            }
        } else {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(156.dp)) {
                CircularProgressIndicator(
                    progress = { state.progress },
                    modifier = Modifier.size(156.dp),
                    strokeWidth = 12.dp,
                )
                Text(
                    text = stringResource(
                        R.string.focus_gate_steps_progress,
                        state.completedSteps,
                        state.targetSteps,
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = stringResource(
                    if (state.isPaused(nowMillis)) {
                        R.string.focus_gate_steps_paused
                    } else {
                        R.string.focus_gate_steps_active
                    }
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
