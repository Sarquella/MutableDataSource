package dev.sarquella.mutabledatasource

import androidx.paging.PageKeyedDataSource
import kotlin.math.max


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

typealias OnInitialPageLoaded<K, T> = (List<T>, Int?, K?, K?) -> Unit
typealias OnPageLoaded<K, T> = (List<T>, K?) -> Unit

/**
 * Wrapper around [PageKeyedDataSource] to allow the mutation of its resulting items.
 */
class MutablePageKeyedDataSource<Key, Original, Mutated> internal constructor(
        private val originalDataSource: PageKeyedDataSource<Key, Original>,
        private val mutate: MutateFunction<Original, Mutated>,
        private val config: MutableDataSource.Config<Key>) :
    PageKeyedDataSource<Key, Mutated>() {

    init {
        originalDataSource.addInvalidatedCallback {
            super.invalidate()
        }
    }

    override fun invalidate() {
        config.alreadyLoaded = 0
        super.invalidate()
    }

    override fun loadInitial(params: LoadInitialParams<Key>,
                             callback: LoadInitialCallback<Key, Mutated>) {

        val initialParams =
            LoadInitialParams<Key>(max(params.requestedLoadSize, config.alreadyLoaded),
                                   params.placeholdersEnabled)

        loadOriginalInitial(initialParams) { data, position, previousKey, nextKey ->
            if (config.totalCount != null && position != null)
                callback.onResult(data, position, config.totalCount!!, previousKey, nextKey)
            else
                callback.onResult(data, previousKey, nextKey)
        }
    }

    override fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Key, Mutated>) {
        loadOriginalAfter(params) { data, nextKey ->
            callback.onResult(data, nextKey)
        }
    }

    override fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Key, Mutated>) {
        loadOriginalBefore(params) { data, previousKey ->
            callback.onResult(data, previousKey)
        }
    }

    private fun loadOriginalInitial(params: LoadInitialParams<Key>,
                                    onLoaded: OnInitialPageLoaded<Key, Mutated>) {

        originalDataSource.loadInitial(
                params,
                object : LoadInitialCallback<Key, Original>() {
                    override fun onResult(data: MutableList<Original>,
                                          position: Int,
                                          totalCount: Int,
                                          previousPageKey: Key?,
                                          nextPageKey: Key?) {

                        config.alreadyLoaded = data.size

                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() ->
                                onLoaded(mutatedData, position, previousPageKey, nextPageKey)

                            data.isEmpty() || nextPageKey == null ->
                                onLoaded(listOf(), position, previousPageKey, nextPageKey)

                            else -> {
                                val afterParams = LoadParams(nextPageKey, params.requestedLoadSize)
                                loadOriginalAfter(afterParams) { loadedData, nextKey ->
                                    onLoaded(loadedData, position, previousPageKey, nextKey)
                                }
                            }
                        }
                    }

                    override fun onResult(data: MutableList<Original>,
                                          previousPageKey: Key?,
                                          nextPageKey: Key?) {

                        config.alreadyLoaded = data.size

                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() ->
                                onLoaded(mutatedData, null, previousPageKey, nextPageKey)

                            data.isEmpty() || nextPageKey == null ->
                                onLoaded(listOf(), null, previousPageKey, nextPageKey)

                            else -> {
                                val afterParams = LoadParams(nextPageKey, params.requestedLoadSize)
                                loadOriginalAfter(afterParams) { loadedData, nextKey ->
                                    onLoaded(loadedData, null, previousPageKey, nextKey)
                                }
                            }
                        }
                    }

                })
    }

    private fun loadOriginalAfter(params: LoadParams<Key>, onLoaded: OnPageLoaded<Key, Mutated>) {
        originalDataSource.loadAfter(
                params,
                object : LoadCallback<Key, Original>() {
                    override fun onResult(data: MutableList<Original>, adjacentPageKey: Key?) {

                        config.alreadyLoaded += data.size

                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() -> onLoaded(mutatedData, adjacentPageKey)

                            data.isEmpty() || adjacentPageKey == null ->
                                onLoaded(listOf(), adjacentPageKey)

                            else -> {
                                val afterParams = LoadParams(adjacentPageKey,
                                                             params.requestedLoadSize)
                                loadOriginalAfter(afterParams, onLoaded)
                            }
                        }
                    }
                })
    }

    private fun loadOriginalBefore(params: LoadParams<Key>, onLoaded: OnPageLoaded<Key, Mutated>) {
        originalDataSource.loadBefore(
                params,
                object : LoadCallback<Key, Original>() {
                    override fun onResult(data: MutableList<Original>, adjacentPageKey: Key?) {

                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() -> onLoaded(mutatedData, adjacentPageKey)

                            data.isEmpty() || adjacentPageKey == null ->
                                onLoaded(listOf(), adjacentPageKey)

                            else -> {
                                val beforeParams = LoadParams(adjacentPageKey,
                                                              params.requestedLoadSize)
                                loadOriginalBefore(beforeParams, onLoaded)
                            }
                        }
                    }
                })
    }

}