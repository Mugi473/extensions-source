package eu.kanade.tachiyomi.extension.fr.phenixscans

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.FilterList
import eu.kanade.tachiyomi.source.model.MangasPage
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.source.model.SChapter
import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import keiyoushi.utils.parseAs
import keiyoushi.utils.tryParse
import kotlinx.serialization.json.float
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.Response
import java.text.SimpleDateFormat
import java.util.Locale

class PhenixScans : HttpSource() {
    override val baseUrl = "https://phenix-scans.co"
    private val cdnBaseUrl = "https://api.phenix-scans.co"
    private val apiBaseUrl = "https://api.phenix-scans.co/api"
    override val lang = "fr"
    override val name = "Phenix Scans"
    override val supportsLatest = true
    override val versionId = 3

    private val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.FRENCH)

    // ============================== Popular ===============================
    override fun popularMangaRequest(page: Int): Request {
        val url = "$apiBaseUrl/front/manga".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", "18")
            .addQueryParameter("sort", "top")
            .build()
        return GET(url, headers)
    }

    override fun popularMangaParse(response: Response): MangasPage = searchMangaParse(response)

    // =============================== Latest ===============================
    override fun latestUpdatesRequest(page: Int): Request {
        val url = "$apiBaseUrl/front/manga".toHttpUrl().newBuilder()
            .addQueryParameter("page", page.toString())
            .addQueryParameter("limit", "18")
            .addQueryParameter("sort", "latest")
            .build()
        return GET(url, headers)
    }

    override fun latestUpdatesParse(response: Response): MangasPage = searchMangaParse(response)

    // =============================== Search ===============================

    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        if (query.isNotEmpty()) {
            val apiUrl = "$apiBaseUrl/front/manga/search".toHttpUrl().newBuilder()
                .addQueryParameter("query", query)
                .build()
            return GET(apiUrl, headers)
        }

        val url = "$apiBaseUrl/front/manga".toHttpUrl().newBuilder()
        filters.forEach { filter ->
            when (filter) {
                is SortFilter -> {
                    url.addQueryParameter("sort", filter.toUriPart())
                }

                is GenreFilter -> {
                    val genres = filter.state
                        .filter { it.state }
                        .map { it.id }

                    url.addQueryParameter("genre", genres.joinToString(","))
                }

                is TypeFilter -> {
                    url.addQueryParameter("type", filter.toUriPart())
                }

                is StatusFilter -> {
                    url.addQueryParameter("status", filter.toUriPart())
                }

                else -> {}
            }
        }
        url.addQueryParameter("limit", "18")
        url.addQueryParameter("page", page.toString())

        return GET(url.build(), headers)
    }

    override fun searchMangaParse(response: Response): MangasPage {
        val data = response.parseAs<SearchResultsDto>()

        val hasNextPage = (data.pagination?.page ?: 0) < (data.pagination?.totalPages ?: 0)

        val mangas = data.mangas.map {
            SManga.create().apply {
                title = it.title
                thumbnail_url = it.coverImage?.let { c -> "$cdnBaseUrl/$c" }
                url = it.slug
            }
        }

        return MangasPage(mangas, hasNextPage)
    }

    override fun getFilterList(): FilterList = getGlobalFilterList(apiBaseUrl, client, headers)

    // =============================== Manga ==================================

    override fun mangaDetailsRequest(manga: SManga): Request {
        val apiUrl = "$apiBaseUrl/front/manga/${manga.url}"

        return GET(apiUrl, headers)
    }

    override fun mangaDetailsParse(response: Response): SManga {
        val data = response.parseAs<MangaDetailDto>()

        return SManga.create().apply {
            title = data.manga.title
            thumbnail_url = data.manga.coverImage?.let { "$cdnBaseUrl/$it" }
            url = data.manga.slug
            description = data.manga.synopsis
            status = when (data.manga.status) {
                "Ongoing" -> SManga.ONGOING
                "Hiatus" -> SManga.ON_HIATUS
                "Completed" -> SManga.COMPLETED
                else -> SManga.UNKNOWN
            }
        }
    }

    override fun getMangaUrl(manga: SManga): String = "$baseUrl/manga/${manga.url}"

    // ============================== Chapters ==============================

    override fun chapterListRequest(manga: SManga): Request = mangaDetailsRequest(manga)

    override fun chapterListParse(response: Response): List<SChapter> {
        val data = response.parseAs<MangaDetailDto>()

        return data.chapters
            .filter { it.price == 0 }
            .map { chapterDto ->
                SChapter.create().apply {
                    chapter_number = chapterDto.number.float
                    date_upload = simpleDateFormat.tryParse(chapterDto.createdAt)
                    name = "Chapitre ${chapterDto.number}"
                    url = "${data.manga.slug}/${chapterDto.number}"
                }
            }
    }

    override fun getChapterUrl(chapter: SChapter): String {
        val slug = chapter.url.substringBeforeLast("/")
        val chapterNumber = chapter.url.substringAfterLast("/")
        return "$baseUrl/manga/$slug/chapitre/$chapterNumber"
    }

    // =============================== Pages ================================

    override fun imageUrlParse(response: Response): String = throw UnsupportedOperationException()

    override fun pageListRequest(chapter: SChapter): Request {
        val slug = chapter.url.substringBeforeLast("/")
        val chapterNumber = chapter.url.substringAfterLast("/")

        val apiUrl = "$apiBaseUrl/front/manga/$slug/chapter/$chapterNumber"

        return GET(apiUrl, headers)
    }

    override fun pageListParse(response: Response): List<Page> {
        val data = response.parseAs<ChapterContentDto>()

        return data.chapter.images.mapIndexed { index, url ->
            Page(index, imageUrl = "$cdnBaseUrl/$url")
        }
    }
}
