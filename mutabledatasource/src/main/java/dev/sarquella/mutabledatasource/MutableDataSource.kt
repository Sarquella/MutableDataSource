package dev.sarquella.mutabledatasource

import androidx.lifecycle.LiveData
import androidx.paging.DataSource
import androidx.paging.PagedList
import java.util.concurrent.Executor


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

typealias MutateFunction<Original, Mutated> = (List<Original>) -> List<Mutated>

typealias FactoryBuilder<Key, Value> =
        (MutableDataSource.Config<Key>) -> DataSource.Factory<Key, Value>

class MutableDataSource private constructor() {

    class Factory<Key, Value> internal constructor(internal val build: FactoryBuilder<Key, Value>)

    class Config<Key> internal constructor(internal val initialKey: Key?) {
        internal var alreadyLoaded: Int = 0
        internal var totalCount: Int? = null
    }

}

/**
 * Constructs a [LiveData<PagedList>] from this [MutableDataSource.Factory].
 * Convenience for [MutableLivePagedListBuilder].
 *
 * @param config Paging configuration.
 * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @return The LiveData of PagedList
 */
fun <Key, Value> MutableDataSource.Factory<Key, Value>.toLiveData(
        config: PagedList.Config,
        initialLoadKey: Key? = null,
        boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
        fetchExecutor: Executor? = null): LiveData<PagedList<Value>> =
    MutableLivePagedListBuilder(this, config).apply {
        setInitialLoadKey(initialLoadKey)
        setBoundaryCallback(boundaryCallback)
        fetchExecutor?.let { setFetchExecutor(fetchExecutor) }
    }.build()

/**
 * Constructs a [LiveData<PagedList>] from this [MutableDataSource.Factory].
 * Convenience for [MutableLivePagedListBuilder].
 *
 * @param pageSize Page size.
 * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
 * @param boundaryCallback The boundary callback for listening to PagedList load state.
 * @param fetchExecutor Executor for fetching data from DataSources.
 *
 * @return The LiveData of PagedList
 */
fun <Key, Value> MutableDataSource.Factory<Key, Value>.toLiveData(
        pageSize: Int,
        initialLoadKey: Key? = null,
        boundaryCallback: PagedList.BoundaryCallback<Value>? = null,
        fetchExecutor: Executor? = null): LiveData<PagedList<Value>> =
    this.toLiveData(
            androidx.paging.Config(pageSize),
            initialLoadKey,
            boundaryCallback,
            fetchExecutor)