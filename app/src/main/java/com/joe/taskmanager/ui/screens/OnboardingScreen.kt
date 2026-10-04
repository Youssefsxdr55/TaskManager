
package com.joe.taskmanager.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.joe.taskmanager.R
import com.joe.taskmanager.ui.screens.onboarding.OnboardingViewModel
import kotlinx.coroutines.launch

/**
 * PRD 7.15: three quick intro screens tailored to what has shipped, then a
 * permission step with a plain-language reason for each, then an empty Today.
 */
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { IntroPages.size })
    val scope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f).fillMaxWidth()
        ) { page ->
            val intro = IntroPages[page]
            Column(
                modifier = Modifier.fillMaxSize().padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = stringResource(intro.titleRes),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = stringResource(intro.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 12.dp)
                )
            }
        }

        Text(
            text = "${pagerState.currentPage + 1} / ${IntroPages.size}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = { viewModel.finish(onFinished) }) {
                Text(stringResource(R.string.onboarding_skip))
            }
            Button(
                onClick = {
                    val last = pagerState.currentPage == IntroPages.size - 1
                    if (last) viewModel.finish(onFinished)
                    else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                }
            ) {
                Text(
                    stringResource(
                        if (pagerState.currentPage == IntroPages.size - 1) R.string.onboarding_start
                        else R.string.onboarding_next
                    )
                )
            }
        }
    }
}

private data class IntroPage(val titleRes: Int, val bodyRes: Int)

/** Three intro screens per PRD 7.15; habits/rewards copy appears when v1.1 ships. */
private val IntroPages = listOf(
    IntroPage(R.string.onboarding_welcome_title, R.string.onboarding_welcome_body),
    IntroPage(R.string.onboarding_reminders_title, R.string.onboarding_reminders_body),
    IntroPage(R.string.onboarding_privacy_title, R.string.onboarding_privacy_body)
)
