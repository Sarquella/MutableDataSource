package dev.sarquella.mutabledatasource


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

class MutableDataSource private constructor() {

    class Config<Key> internal constructor(internal val initialKey: Key?) {
        internal var alreadyLoaded: Int = 0
        internal var totalCount: Int? = null
    }

}