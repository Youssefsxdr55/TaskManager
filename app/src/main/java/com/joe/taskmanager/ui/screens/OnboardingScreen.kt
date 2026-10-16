package com.joe.taskmanager.ui.screens
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch
/** PRD 7.15: three intro screens followed by a plain-language permission step. */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenReliability: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { IntroPages.size })
    val scope = rememberCoroutineScope()
    var showPermissionStep by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxSize()) {
        if (!showPermissionStep) {
            HorizontalPager(state = pagerState, modifier = Modifier.weight(1f).fillMaxWidth()) { page ->
                val intro = IntroPages[page]
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(stringResource(intro.titleRes), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
                    Text(
                        stringResource(intro.bodyRes), style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
            Text("${pagerState.currentPage + 1} / ${IntroPages.size}",
                style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.CenterHorizontally))
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { viewModel.finish(onFinished) }) { Text(stringResource(R.string.onboarding_skip)) }
                Button(onClick = {
                    if (pagerState.currentPage == IntroPages.lastIndex) showPermissionStep = true
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }) { Text(stringResource(R.string.onboarding_next)) }
            }
        } else {
            Column(
                modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(stringResource(R.string.onboarding_permissions_title), style = MaterialTheme.typography.headlineSmall)
                Text(stringResource(R.string.onboarding_permissions_body), style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                PermissionReason(R.string.perm_notifications_title, R.string.perm_notifications_body)
                PermissionReason(R.string.perm_exact_alarm_title, R.string.perm_exact_alarm_body)
                PermissionReason(R.string.perm_full_screen_title, R.string.perm_full_screen_body)
                PermissionReason(R.string.perm_battery_title, R.string.perm_battery_body)
                Button(onClick = onOpenReliability, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.onboarding_open_settings))
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(onClick = { showPermissionStep = false }) { Text(stringResource(R.string.action_back)) }
                Button(onClick = { viewModel.finish(onFinished) }) { Text(stringResource(R.string.onboarding_start)) }
            }
        }
    }
}
@Composable
private fun PermissionReason(title: Int, body: Int) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        Text(stringResource(body), style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
private data class IntroPage(val titleRes: Int, val bodyRes: Int)
private val IntroPages = listOf(
    IntroPage(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
    IntroPage(R.string.onboarding_reminders_title, R.string.onboarding_reminders_body),
    IntroPage(R.string.onboarding_privacy_title, R.string.onboarding_privacy_body)
)
