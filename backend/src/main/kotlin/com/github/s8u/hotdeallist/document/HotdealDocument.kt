package com.github.s8u.hotdeallist.document

import com.github.s8u.hotdeallist.enums.PlatformType
import org.springframework.data.annotation.Id
import org.springframework.data.elasticsearch.annotations.DateFormat
import org.springframework.data.elasticsearch.annotations.Document
import org.springframework.data.elasticsearch.annotations.Field
import org.springframework.data.elasticsearch.annotations.FieldType
import org.springframework.data.elasticsearch.annotations.InnerField
import org.springframework.data.elasticsearch.annotations.MultiField
import org.springframework.data.elasticsearch.annotations.Setting
import org.springframework.data.elasticsearch.annotations.CompletionField
import org.springframework.data.elasticsearch.core.suggest.Completion
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

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer"),
        otherFields = [InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "ngram_analyzer")]
    )
    val title: String,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard"),
        otherFields = [InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "ngram_analyzer")]
    )
    val titleEn: String? = null,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "nori_analyzer", searchAnalyzer = "nori_analyzer"),
        otherFields = [InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "ngram_analyzer")]
    )
    val productName: String? = null,

    @MultiField(
        mainField = Field(type = FieldType.Text, analyzer = "standard", searchAnalyzer = "standard"),
        otherFields = [InnerField(suffix = "ngram", type = FieldType.Text, analyzer = "ngram_analyzer", searchAnalyzer = "ngram_analyzer")]
    )
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
    val shoppingPlatform: String? = null,

    @CompletionField(analyzer = "suggest_analyzer", searchAnalyzer = "suggest_analyzer")
    val suggest: Completion? = null
)
