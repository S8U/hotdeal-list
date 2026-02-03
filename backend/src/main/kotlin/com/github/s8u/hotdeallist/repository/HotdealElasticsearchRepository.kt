package com.github.s8u.hotdeallist.repository

import com.github.s8u.hotdeallist.document.HotdealDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository

interface HotdealElasticsearchRepository : ElasticsearchRepository<HotdealDocument, Long>
