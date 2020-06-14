package dev.sarquella.mutabledatasource

import androidx.paging.DataSource
import androidx.paging.ItemKeyedDataSource
import androidx.paging.PageKeyedDataSource
import androidx.paging.PositionalDataSource


/*
 * Created by Adrià Sarquella on 15/06/2020.
 * adria@sarquella.dev
 */

/**
 * Applies the given mutation function to each page emitted by DataSources produced by this Factory.
 *
 * @param Original Type of items produced by the original pre-mutation DataSource
 * @param Mutated Type of items produced by the new post-mutation DataSource
 *
 * @param totalCount Total number of items resulting from the new DataSource after
 * the applied mutation
 * @param function Function that runs on each loaded page, returning mutated items.
 * @return
 */
@Suppress("UNCHECKED_CAST")
fun <Key, Original, Mutated> DataSource.Factory<Key, Original>.mutateByPage(
        totalCount: Int? = null,
        function: MutateFunction<Original, Mutated>): MutableDataSource.Factory<Key, Mutated> {

    val originalFactory = this

    return MutableDataSource.Factory { config ->
        object : DataSource.Factory<Key, Mutated>() {
            override fun create(): DataSource<Key, Mutated> {
                val originalDataSource = originalFactory.create()
                config.totalCount = totalCount

                return when (originalDataSource) {
                    is PositionalDataSource<Original> ->
                        MutablePositionalDataSource(originalDataSource, function, config)

                    is ItemKeyedDataSource<Key, Original> ->
                        MutableItemKeyedDataSource(originalDataSource, function, config)

                    is PageKeyedDataSource<Key, Original> ->
                        MutablePageKeyedDataSource(originalDataSource, function, config)

                    else -> originalDataSource
                } as DataSource<Key, Mutated>
            }
        }
    }
}