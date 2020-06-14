package dev.sarquella.mutabledatasource

import androidx.paging.PageKeyedDataSource
import androidx.paging.PositionalDataSource
import kotlin.math.max
import kotlin.math.min


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

typealias OnRangeLoaded<T> = (List<T>, Int?) -> Unit

/**
 * Wrapper around [PositionalDataSource] to allow the mutation of its resulting items.
 *
 * Note that is actually is a subclass of [PageKeyedDataSource] acting as a [PositionalDataSource]
 * as the latest does not allow to return different number of items per page than specified in its
 * page size, limiting the mutation options.
 */
class MutablePositionalDataSource<Key, Original, Mutated> internal constructor(
        private val originalDataSource: PositionalDataSource<Original>,
        private val mutate: MutateFunction<Original, Mutated>,
        private val config: MutableDataSource.Config<Key>) :
    PageKeyedDataSource<Int, Mutated>() {

    init {
        originalDataSource.addInvalidatedCallback {
            super.invalidate()
        }
    }

    override fun invalidate() {
        config.alreadyLoaded = 0
        super.invalidate()
    }

    override fun loadInitial(params: LoadInitialParams<Int>,
                             callback: LoadInitialCallback<Int, Mutated>) {

        val initialPosition = (config.initialKey as? Int) ?: 0

        val alreadyLoaded = config.alreadyLoaded
        config.alreadyLoaded = 0

        loadForwardRange(initialPosition,
                         max(params.requestedLoadSize, alreadyLoaded)) { data, nextPosition ->
            config.totalCount?.let { totalCount ->
                callback.onResult(data, initialPosition, totalCount, initialPosition, nextPosition)
            } ?: run {
                callback.onResult(data, initialPosition, nextPosition)
            }
        }
    }

    override fun loadAfter(params: LoadParams<Int>, callback: LoadCallback<Int, Mutated>) {
        loadForwardRange(params.key, params.requestedLoadSize) { data, nextPosition ->
            callback.onResult(data, nextPosition)
        }
    }

    override fun loadBefore(params: LoadParams<Int>, callback: LoadCallback<Int, Mutated>) {
        loadBackwardRange(params.key, params.requestedLoadSize) { data, previousPosition ->
            callback.onResult(data, previousPosition)
        }
    }

    private fun loadForwardRange(position: Int, loadSize: Int, onLoaded: OnRangeLoaded<Mutated>) {

        val rangeParams = PositionalDataSource.LoadRangeParams(position, loadSize)
        loadRange(rangeParams) { originalData ->
            config.alreadyLoaded += originalData.size

            val mutatedData = mutate(originalData)

            when {
                mutatedData.isNotEmpty() -> onLoaded(mutatedData, position + loadSize)
                originalData.isEmpty() -> onLoaded(listOf(), null)
                else -> loadForwardRange(position + loadSize, loadSize, onLoaded)
            }
        }
    }

    private fun loadBackwardRange(position: Int, loadSize: Int, onLoaded: OnRangeLoaded<Mutated>) {

        if (position <= 0) {
            onLoaded(listOf(), null)
            return
        }

        val positionOffset = position - loadSize
        val rangeStart = max(positionOffset, 0)
        val rangeSize = loadSize + min(0, positionOffset)

        val rangeParams = PositionalDataSource.LoadRangeParams(rangeStart, rangeSize)
        loadRange(rangeParams) { originalData ->

            val mutatedData = mutate(originalData)

            when {
                mutatedData.isNotEmpty() -> onLoaded(mutatedData, position - loadSize)
                originalData.isEmpty() -> onLoaded(listOf(), null)
                else -> loadBackwardRange(position - loadSize, loadSize, onLoaded)
            }
        }
    }

    private fun loadRange(params: PositionalDataSource.LoadRangeParams,
                          onLoaded: (List<Original>) -> Unit) {
        originalDataSource.loadRange(
                params,
                object : PositionalDataSource.LoadRangeCallback<Original>() {
                    override fun onResult(data: MutableList<Original>) {
                        onLoaded(data)
                    }
                }
        )
    }

}