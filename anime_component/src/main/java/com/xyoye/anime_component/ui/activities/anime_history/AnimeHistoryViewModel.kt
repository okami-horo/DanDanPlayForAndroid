package com.xyoye.anime_component.ui.activities.anime_history

import androidx.lifecycle.viewModelScope
import com.xyoye.common_component.base.BaseViewModel
import com.xyoye.common_component.extension.toastError
import com.xyoye.common_component.network.repository.AnimeRepository
import com.xyoye.data_component.data.AnimeData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class AnimeHistoryViewModel : BaseViewModel() {
    private val historiesFlow = MutableStateFlow<List<AnimeData>>(emptyList())
    private val searchWordFlow = MutableStateFlow("")

    val displayHistoriesFlow =
        combine(historiesFlow, searchWordFlow) { histories, searchWord ->
            filterAnimeHistory(histories, searchWord)
        }

    fun getCloudHistory() {
        viewModelScope.launch {
            showLoading()
            val result = AnimeRepository.getPlayHistory()
            hideLoading()

            if (result.isFailure) {
                result.exceptionOrNull()?.message?.toastError()
                return@launch
            }

            historiesFlow.emit(result.getOrNull()?.playHistoryAnimes ?: emptyList())
        }
    }

    fun searchAnime(keyword: String) {
        searchWordFlow.value = keyword
    }
}

/**
 * Filters [histories] by [searchWord] using case-insensitive title matching.
 * Returns the full list when [searchWord] is empty.
 *
 * Extracted as a package-internal function to allow direct unit testing without
 * requiring a ViewModel instance or Android runtime.
 */
internal fun filterAnimeHistory(
    histories: List<AnimeData>,
    searchWord: String,
): List<AnimeData> {
    if (searchWord.isEmpty()) {
        return histories
    }
    return histories.filter { it.animeTitle?.contains(searchWord, ignoreCase = true) == true }
}

