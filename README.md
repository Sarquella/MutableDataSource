# MutableDataSource

A wrapper around Android [Paging Library](https://developer.android.com/topic/libraries/architecture/paging)'s DataSources to allow the free mutation of their resulting items.

#### 100% Kotlin ❤️
[![Download](https://api.bintray.com/packages/sarquella/MutableDataSource/dev.sarquella.mutabledatasource/images/download.svg) ](https://bintray.com/sarquella/MutableDataSource/dev.sarquella.mutabledatasource/_latestVersion) [![API](https://img.shields.io/badge/API-16%2B-brightgreen.svg?style=flat)](https://android-arsenal.com/api?level=16) [![License](https://img.shields.io/badge/license-Apache%202.0-lightgrey.svg)](https://opensource.org/licenses/Apache-2.0)

### Follow
<a href='https://ko-fi.com/S6S8RENM' target='_blank'><img height='36' style='border:0px;height:36px;' src='https://az743702.vo.msecnd.net/cdn/kofi1.png?v=2' border='0' alt='Buy Me a Coffee at ko-fi.com' /></a>

[![GitHub Follow](https://img.shields.io/github/followers/Sarquella.svg?label=Follow&style=social)](https://github.com/Sarquella) [![Twitter Follow](https://img.shields.io/twitter/follow/AdriSarquella.svg?label=Follow&style=social)](https://twitter.com/AdriSarquella)

## Download
```groovy
dependencies {
    //...
    implementation 'dev.sarquella.mutabledatasource:mutabledatasource:0.1.0'
}
```

## Usage

**1- Apply the mutating function**

**Java** ([Java 8+](https://developer.android.com/studio/write/java8-support) required)

```java
MutableDataSource.Factory<Key, Value> mutableDataSourceFactory = DataSourceTransformation.mutateByPage(dataSourceFactory, original -> {
	//Mutate
});
```


**Kotlin**

```kotlin
val mutableDataSourceFactory = dataSourceFactory.mutateByPage { original ->
  //Mutate
}
```

> **Important** 
> The mutating function is applied per page. You will need to be careful in cases such as adding items at every page without taking into account that the `original` list has already loaded all items (page is empty), as it could lead to a never ending list. 

**2- Build the LiveData PagedList**

Generating the `LiveData<PagedList>` instance from the `MutableDataSource.Factory` is the same as with the regular `DataSource.Factory`. However, instead of using `LivePagedListBuilder` to do so, `MutableLivePagedListBuilder` should be used:

```kotlin
val liveDataPagedList = MutableLivePagedListBuilder(mutableDataSourceFactory, pageSize)
```

Alternatively, the `toLiveData` extension function can also be used:

```kotlin
val liveDataPagedList = mutableDataSourceFactory.toLiveData(pageSize)
```

---

**Placeholders [Optional]**

In order to be able to use placeholders in the Paging Library, it should be capable of computing the total number of items even before loading all of them. When applying the mutation function, this total count might change due to modifying the number of items per page.

In case the total number of items after mutating is already known, it can be specified when applying the mutating function to continue making use of the placeholders:

**Java**

```Java
... = DataSourceTransformation.mutateByPage(dataSourceFactory, totalCount, original -> {
	//Mutate
});
```

**Kotlin**

```kotlin
... = dataSourceFactory.mutateByPage(totalCount) { original ->
  //Mutate
}

```


## License
[LICENSE](https://github.com/Sarquella/MutableDataSource/blob/master/LICENSE)

```
Copyright 2020 Adrià Sarquella Farrés

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```