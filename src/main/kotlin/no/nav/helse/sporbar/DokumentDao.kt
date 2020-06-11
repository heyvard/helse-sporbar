package no.nav.helse.sporbar

import kotliquery.queryOf
import kotliquery.sessionOf
import org.intellij.lang.annotations.Language
import java.util.*
import javax.sql.DataSource

class DokumentDao(val datasource: DataSource) {
    fun opprett(hendelse: Hendelse) {
        @Language("PostgreSQL")
        val query = "INSERT INTO hendelse(hendelse_id, dokument_id, type) VALUES(?,?,?) ON CONFLICT DO NOTHING"
        sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    query,
                    hendelse.hendelseId, hendelse.dokumentId, hendelse.type.name
                ).asUpdate
            )
        }
    }

    fun finn(hendelseIder: List<UUID>) = sessionOf(datasource).use { session ->
        @Language("PostgreSQL")
        val query = """SELECT * FROM hendelse h
            INNER JOIN hendelse_dokument hd ON h.hendelse_id = hd.hendelse_id
             INNER JOIN dokument d ON hd.dokument_id = d.id
            WHERE hendelse_id = ANY((?)::uuid[])""".trimMargin()
        session.run(
            queryOf(query, hendelseIder.joinToString(prefix = "{", postfix = "}", separator = ",") { it.toString() })
                .map { row ->
                    Hendelse(
                        dokumentId = UUID.fromString(row.string("dokument_id")),
                        hendelseId = UUID.fromString(row.string("hendelse_id")),
                        type = enumValueOf(row.string("type"))
                    )
                }.asList
        )
    }

    fun lagre(vedtaksperiodeId: UUID, fagsystemId: String) {
        @Language("PostgreSQL")
        val query = "INSERT INTO vedtak_utbetalingsref(vedtaksperiode_id, utbetalingsref) VALUES(?,?) ON CONFLICT DO NOTHING"
        sessionOf(datasource).use { session ->
            session.run(
                queryOf(
                    query,
                    vedtaksperiodeId,
                    fagsystemId
                ).asUpdate
            )
        }
    }
}

data class Hendelse(
    val dokumentId: UUID,
    val hendelseId: UUID,
    val type: Dokument
)
