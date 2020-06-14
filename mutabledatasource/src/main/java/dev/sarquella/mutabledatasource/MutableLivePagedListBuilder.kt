package dev.sarquella.mutabledatasource

import androidx.lifecycle.LiveData
import androidx.paging.LivePagedListBuilder
import androidx.paging.PagedList
import java.util.concurrent.Executor


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

/**
 * Wrapper around [LivePagedListBuilder] to be able to generate a [LiveData<PagedList>] given
 * a [MutableDataSource.Factory].
 *
 * @property mutableDataSourceFactory DataSource factory providing DataSource generations.
 * @property config Paging configuration.
 */
class MutableLivePagedListBuilder<Key, Value>(
        private val mutableDataSourceFactory: MutableDataSource.Factory<Key, Value>,
        private val config: PagedList.Config) {

    private var initialLoadKey: Key? = null
    private var boundaryCallback: PagedList.BoundaryCallback<Value>? = null
    private var fetchExecutor: Executor? = null

    /**
     * Creates a MutableLivePagedListBuilder with required parameters.
     *
     * @param mutableDataSourceFactory DataSource.Factory providing DataSource generations.
     * @param pageSize Size of pages to load.
     */
    constructor(mutableDataSourceFactory: MutableDataSource.Factory<Key, Value>,
                pageSize: Int) : this(mutableDataSourceFactory,
                                      PagedList.Config.Builder().setPageSize(pageSize).build())

    /**
     * First loading key passed to the first PagedList/DataSource.
     *
     * @param initialLoadKey Initial load key passed to the first PagedList/DataSource.
     * @return this
     */
    fun setInitialLoadKey(initialLoadKey: Key?): MutableLivePagedListBuilder<Key, Value> {
        this.initialLoadKey = initialLoadKey
        return this
    }

    /**
     * Sets a [PagedList.BoundaryCallback] on each PagedList created, typically used to load
     * additional data from network when paging from local storage.
     *
     * @param boundaryCallback The boundary callback for listening to PagedList load state.
     * @return this
     */
    fun setBoundaryCallback(boundaryCallback: PagedList.BoundaryCallback<Value>?):
            MutableLivePagedListBuilder<Key, Value> {
        this.boundaryCallback = boundaryCallback
        return this
    }

    /**
     * Sets executor used for background fetching of PagedLists, and the pages within.
     *
     * @param fetchExecutor Executor for fetching data from DataSources.
     * @return this
     */
    fun setFetchExecutor(fetchExecutor: Executor): MutableLivePagedListBuilder<Key, Value> {
        this.fetchExecutor = fetchExecutor
        return this
    }

    /**
     * Constructs the [LiveData<PagedList>].
     *
     * @return The LiveData of PagedLists
     */
    fun build(): LiveData<PagedList<Value>> =
        LivePagedListBuilder(
                mutableDataSourceFactory.build(MutableDataSource.Config(initialLoadKey)),
                config).apply {
            setInitialLoadKey(initialLoadKey)
            setBoundaryCallback(boundaryCallback)
            fetchExecutor?.let { setFetchExecutor(it) }
        }.build()

}