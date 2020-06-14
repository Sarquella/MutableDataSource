package dev.sarquella.mutabledatasource

import androidx.paging.DataSource


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

typealias MutateFunction<Original, Mutated> = (List<Original>) -> List<Mutated>

typealias FactoryBuilder<Key, Value> =
        (MutableDataSource.Config<Key>) -> DataSource.Factory<Key, Value>

class MutableDataSource private constructor() {

    class Factory<Key, Value> internal constructor(private val build: FactoryBuilder<Key, Value>)

    class Config<Key> internal constructor(internal val initialKey: Key?) {
        internal var alreadyLoaded: Int = 0
        internal var totalCount: Int? = null
    }

}