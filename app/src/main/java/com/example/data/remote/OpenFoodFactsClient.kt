package com.example.data.remote

import com.example.data.local.entity.FoodItemCacheEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class OpenFoodFactsClient(
    private val okHttpClient: OkHttpClient = defaultClient()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    suspend fun searchFoods(query: String): Result<List<FoodItemCacheEntity>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val trimmed = query.trim()
                if (trimmed.isBlank()) return@runCatching emptyList()

                val url = "https://world.openfoodfacts.org/cgi/search.pl"
                    .toHttpUrl()
                    .newBuilder()
                    .addQueryParameter("search_terms", trimmed)
                    .addQueryParameter("search_simple", "1")
                    .addQueryParameter("action", "process")
                    .addQueryParameter("json", "1")
                    .addQueryParameter("page_size", "20")
                    .addQueryParameter("fields", "code,product_name,brands,nutriments,serving_quantity")
                    .build()

                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", "KinetixAndroid/1.0 (offline-first-fitness)")
                    .get()
                    .build()

                val body = okHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        error("Open Food Facts HTTP ${response.code}")
                    }
                    response.body?.string().orEmpty()
                }

                if (body.isBlank()) return@runCatching emptyList()
                val parsed = json.decodeFromString<OffSearchResponse>(body)
                val now = System.currentTimeMillis()

                parsed.products.mapNotNull { product ->
                    val name = product.productName?.trim().orEmpty()
                    if (name.isBlank()) return@mapNotNull null
                    val n = product.nutriments ?: return@mapNotNull null
                    val kcal = n.energyKcal100g ?: return@mapNotNull null
                    val code = product.code?.takeIf { it.isNotBlank() }
                        ?: "off_${name.lowercase().hashCode()}"

                    FoodItemCacheEntity(
                        barcodeOrKey = code,
                        name = name,
                        brand = product.brands?.split(",")?.firstOrNull()?.trim().orEmpty(),
                        kcalPer100g = kcal.coerceAtLeast(0.0),
                        proteinPer100g = (n.proteins100g ?: 0.0).coerceAtLeast(0.0),
                        carbsPer100g = (n.carbohydrates100g ?: 0.0).coerceAtLeast(0.0),
                        fatPer100g = (n.fat100g ?: 0.0).coerceAtLeast(0.0),
                        servingSizeG = (product.servingQuantity ?: 100.0).coerceIn(5.0, 1000.0),
                        cachedAtEpochMs = now,
                        lastUsedAtEpochMs = 0L,
                        isCustom = 0
                    )
                }
            }
        }

    companion object {
        fun defaultClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .callTimeout(10, TimeUnit.SECONDS)
            .build()
    }
}

@Serializable
private data class OffSearchResponse(
    val products: List<OffProduct> = emptyList()
)

@Serializable
private data class OffProduct(
    val code: String? = null,
    @SerialName("product_name") val productName: String? = null,
    val brands: String? = null,
    @SerialName("serving_quantity") val servingQuantity: Double? = null,
    val nutriments: OffNutriments? = null
)

@Serializable
private data class OffNutriments(
    @SerialName("energy-kcal_100g") val energyKcal100g: Double? = null,
    @SerialName("proteins_100g") val proteins100g: Double? = null,
    @SerialName("carbohydrates_100g") val carbohydrates100g: Double? = null,
    @SerialName("fat_100g") val fat100g: Double? = null
)
