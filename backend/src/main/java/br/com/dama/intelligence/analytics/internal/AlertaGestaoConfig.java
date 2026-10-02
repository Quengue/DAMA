package br.com.dama.intelligence.analytics.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Escolhe o motor dos alertas de gestão. Com {@code dama.alertas.url} (variável ALERTAS_URL) o backend usa o
 * alert-service Node.js, com as regras locais como fallback; sem ela, usa só as regras locais (testes e dev sem Docker).
 */
@Configuration(proxyBeanMethods = false)
class AlertaGestaoConfig {

    private static final Logger log = LoggerFactory.getLogger(AlertaGestaoConfig.class);

    @Bean
    AlertaGestaoClient alertaGestaoClient(@Value("${dama.alertas.url:}") String url,
                                          @Value("${dama.alertas.timeout-ms:2000}") long timeoutMs) {
        AlertaGestaoClient local = new AlertaGestaoLocal();
        if (url == null || url.isBlank()) {
            log.info("Alertas de gestão: regras locais (dama.alertas.url não configurada).");
            return local;
        }
        log.info("Alertas de gestão: alert-service em {} (timeout {} ms, fallback local).", url, timeoutMs);
        return new AlertaGestaoNode(url.trim(), Duration.ofMillis(timeoutMs), local);
    }
}
