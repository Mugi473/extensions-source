package eu.kanade.tachiyomi.extension.fr.crunchyscan

import eu.kanade.tachiyomi.multisrc.madara.Madara
import java.text.SimpleDateFormat
import java.util.Locale

class Crunchyscan :
    Madara(
        "Crunchyscan",
        "https://crunchyscan.fr",
        "fr",
        SimpleDateFormat("MMMM d, yyyy", Locale("fr")),
    ) {
    override val useNewChapterEndpoint = true
    override val mangaDetailsSelectorStatus =
        "div.summary-heading:contains(statut) + div.summary-content"
}
