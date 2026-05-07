package eu.kanade.tachiyomi.extension.fr.phenixscans

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

@Serializable
class SearchResultsDto(
    val mangas: List<LatestMangaItemDto>,
    val pagination: PaginationFilterDto? = null,
    val genres: List<GenreDto>? = null,
)

@Serializable
class PaginationFilterDto(
    val page: Int,
    val totalPages: Int,
)

@Serializable
class MangaInfoDto(
    val title: String,
    val coverImage: String? = null,
    val slug: String,
    val synopsis: String? = "",
    val status: String? = null,
)

@Serializable
class ChapterInfoDto(
    val number: JsonPrimitive,
    val createdAt: String?,
    val price: Int = 0,
)

@Serializable
class MangaDetailDto(
    val manga: MangaInfoDto,
    val chapters: List<ChapterInfoDto>,
)

@Serializable
class LatestMangaItemDto(
    val title: String,
    val coverImage: String? = null,
    val slug: String,
)

@Serializable
class ChapterImagesDto(
    val images: List<String>,
)

@Serializable
class ChapterContentDto(
    val chapter: ChapterImagesDto,
)

@Serializable
class GenreDto(
    val id: String,
    val name: String,
)
