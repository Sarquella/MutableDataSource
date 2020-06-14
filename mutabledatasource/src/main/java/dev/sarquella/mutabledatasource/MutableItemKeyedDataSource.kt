package dev.sarquella.mutabledatasource

import androidx.paging.ItemKeyedDataSource
import java.lang.IllegalStateException
import kotlin.math.max


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

typealias OnInitialItemsLoaded<T> = (List<T>, Int?) -> Unit
typealias OnItemsLoaded<T> = (List<T>) -> Unit

/**
 * Wrapper around [ItemKeyedDataSource] to allow the mutation of its resulting items.
 */
class MutableItemKeyedDataSource<Key, Original, Mutated> internal constructor(
        private val originalDataSource: ItemKeyedDataSource<Key, Original>,
        private val mutate: MutateFunction<Original, Mutated>,
        private val config: MutableDataSource.Config<Key>) :
    ItemKeyedDataSource<Key, Mutated>() {

    private var startKey: Key? = config.initialKey
    private var endKey: Key? = config.initialKey

    init {
        originalDataSource.addInvalidatedCallback {
            super.invalidate()
        }
    }

    override fun invalidate() {
        config.alreadyLoaded = 0
        super.invalidate()
    }

    // Returning a dummy key. The original DataSource's start and end key will be used.
    override fun getKey(item: Mutated): Key = startKey ?: throw IllegalStateException()

    override fun loadInitial(params: LoadInitialParams<Key>,
                             callback: LoadInitialCallback<Mutated>) {

        val initialParams = LoadInitialParams(
                config.initialKey,
                max(params.requestedLoadSize, config.alreadyLoaded),
                params.placeholdersEnabled)

        loadOriginalInitial(initialParams) { data, position ->
            if (config.totalCount != null && position != null)
                callback.onResult(data, position, config.totalCount!!)
            else
                callback.onResult(data)
        }
    }

    override fun loadAfter(params: LoadParams<Key>, callback: LoadCallback<Mutated>) {
        endKey?.let { key ->
            val afterParams = LoadParams(key, params.requestedLoadSize)
            loadOriginalAfter(afterParams) { data ->
                callback.onResult(data)
            }
        } ?: callback.onResult(listOf())

    }

    override fun loadBefore(params: LoadParams<Key>, callback: LoadCallback<Mutated>) {
        startKey?.let { key ->
            val beforeParams = LoadParams(key, params.requestedLoadSize)
            loadOriginalBefore(beforeParams) { data ->
                callback.onResult(data)
            }
        } ?: callback.onResult(listOf())

    }

    private fun loadOriginalInitial(params: LoadInitialParams<Key>,
                                    onLoaded: OnInitialItemsLoaded<Mutated>) {

        originalDataSource.loadInitial(
                params,
                object : LoadInitialCallback<Original>() {
                    override fun onResult(data: MutableList<Original>,
                                          position: Int,
                                          totalCount: Int) {

                        config.alreadyLoaded = data.size

                        updateStartKey(data)
                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() -> {
                                updateEndKey(data)
                                onLoaded(mutatedData, position)
                            }

                            data.isEmpty() -> onLoaded(listOf(), position)

                            else -> {
                                val afterParams = LoadParams(originalDataSource.getKey(data.last()),
                                                             params.requestedLoadSize)
                                loadOriginalAfter(afterParams) { afterData ->
                                    onLoaded(afterData, position)
                                }
                            }
                        }

                    }

                    override fun onResult(data: MutableList<Original>) {

                        config.alreadyLoaded = data.size

                        updateStartKey(data)
                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() -> {
                                updateEndKey(data)
                                onLoaded(mutatedData, null)
                            }

                            data.isEmpty() -> onLoaded(listOf(), null)

                            else -> {
                                val afterParams = LoadParams(originalDataSource.getKey(data.last()),
                                                             params.requestedLoadSize)
                                loadOriginalAfter(afterParams) { afterData ->
                                    onLoaded(afterData, null)
                                }
                            }
                        }
                    }

                })

    }

    private fun loadOriginalAfter(params: LoadParams<Key>, onLoaded: OnItemsLoaded<Mutated>) {

        originalDataSource.loadAfter(
                params,
                object : LoadCallback<Original>() {
                    override fun onResult(data: MutableList<Original>) {

                        config.alreadyLoaded += data.size

                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() -> {
                                updateEndKey(data)
                                onLoaded(mutatedData)
                            }

                            data.isEmpty() -> onLoaded(listOf())

                            else -> {
                                val afterParams = LoadParams(originalDataSource.getKey(data.last()),
                                                             params.requestedLoadSize)
                                loadOriginalAfter(afterParams, onLoaded)
                            }
                        }
                    }
                })
    }

    private fun loadOriginalBefore(params: LoadParams<Key>, onLoaded: OnItemsLoaded<Mutated>) {

        originalDataSource.loadBefore(
                params,
                object : LoadCallback<Original>() {
                    override fun onResult(data: MutableList<Original>) {

                        val mutatedData = mutate(data)

                        when {
                            mutatedData.isNotEmpty() -> {
                                updateStartKey(data)
                                onLoaded(mutatedData)
                            }

                            data.isEmpty() -> onLoaded(listOf())

                            else -> {
                                val beforeParams =
                                    LoadParams(originalDataSource.getKey(data.first()),
                                               params.requestedLoadSize)
                                loadOriginalBefore(beforeParams, onLoaded)
                            }
                        }
                    }
                })
    }

    private fun updateStartKey(data: List<Original>) {
        data.firstOrNull()?.let { item ->
            startKey = originalDataSource.getKey(item)
        }
    }

    private fun updateEndKey(data: List<Original>) {
        data.lastOrNull()?.let { item ->
            endKey = originalDataSource.getKey(item)
        }
    }

}