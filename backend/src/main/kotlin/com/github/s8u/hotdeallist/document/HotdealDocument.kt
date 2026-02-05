package com.github.s8u.hotdeallist.document

import com.github.s8u.hotdeallist.enums.PlatformType
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Document(indexName = "hotdeals")
@Setting(settingPath = "elasticsearch/hotdeals-settings.json")
class HotdealDocument(
    @Id
    val id: Long,

    @Field(type = FieldType.Long)
    val hotdealRawId: Long,

    @Field(type = FieldType.Keyword)
    val platformType: PlatformType,

    @Field(type = FieldType.Keyword)
    val platformPostId: String,

    @Field(type = FieldType.Keyword, index = false)
    val url: String,

    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    val title: String,

    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    val titleEn: String? = null,

    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    val productName: String? = null,

    @Field(type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "standard")
    val productNameEn: String? = null,

    @Field(type = FieldType.Double)
    val price: BigDecimal? = null,

    @Field(type = FieldType.Keyword)
    val currencyUnit: String = "KRW",

    @Field(type = FieldType.Integer)
    val viewCount: Int = 0,

    @Field(type = FieldType.Integer)
    val commentCount: Int = 0,

    @Field(type = FieldType.Integer)
    val likeCount: Int = 0,

    @Field(type = FieldType.Boolean)
    val isEnded: Boolean = false,

    @Field(type = FieldType.Keyword, index = false)
    val sourceUrl: String? = null,

    @Field(type = FieldType.Keyword, index = false)
    val thumbnailUrl: String? = null,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis])
    val wroteAt: LocalDateTime,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis])
    val createdAt: LocalDateTime,

    @Field(type = FieldType.Date, format = [DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis])
    val updatedAt: LocalDateTime,

    @Field(type = FieldType.Keyword)
    val categoryCodes: List<String> = emptyList(),

    @Field(type = FieldType.Keyword)
    val shoppingPlatform: String? = null
)
