package no.nav.helse.sporbar.inntektsmelding

import com.fasterxml.jackson.databind.JsonNode
import java.time.Duration
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.sporbar.Meldingstype
import no.nav.helse.sporbar.inntektsmelding.Producer.Melding
import no.nav.helse.sporbar.objectMapper
import org.slf4j.Logger
import org.slf4j.LoggerFactory

internal class InntektsmeldingStatusMediator(
    private val inntektsmeldingStatusDao: InntektsmeldingStatusDao,
    private val producer: Producer
) {

    internal fun lagre(inntektsmeldingStatus: InntektsmeldingStatus) {
        inntektsmeldingStatusDao.lagre(inntektsmeldingStatus)
    }

    internal fun publiser(statustimeout: Duration) {
        val statuser = inntektsmeldingStatusDao.hent(statustimeout).takeUnless { it.isEmpty() } ?: return
        logg.info("Publiserer ${statuser.size} inntektsmeldingstatuser. Se sikkerlogg for detaljer.")
        statuser.forEach { status ->
            val json = objectMapper.valueToTree<JsonNode>(status)
            producer.send(Melding(
                topic = "tbd.inntektsmeldingstatus",
                meldingstype = Meldingstype.Inntektsmeldingstatus,
                key = status.sykmeldt,
                json = json
            ))
            sikkerLogg.info("Publiserer inntektsmeldingstatus for {} fra ${ISO_OFFSET_DATE_TIME.format(status.tidspunkt)}:\n\t$json", keyValue("vedtaksperiodeId", status.vedtaksperiode.id))
        }
        inntektsmeldingStatusDao.publisert(statuser)
    }

    private companion object {
        private val sikkerLogg: Logger = LoggerFactory.getLogger("tjenestekall")
        private val logg: Logger = LoggerFactory.getLogger("inntektsmeldingstatus")
    }
}