package com.mecqrsafe.api

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource
import org.springframework.web.filter.CorsFilter

/**
 * CORS para o editor servido fora da API (docs/DEMO.md): na demo, o
 * editor é um site estático, que não dorme, e chama a API em outra origem.
 * DESLIGADO por padrão — localmente o Vite faz proxy e a imagem Docker
 * serve o editor na mesma origem — e o render.yaml liga com a origem do
 * site.
 *
 * É um filtro com a maior precedência, antes do [RateLimitFilter] e do
 * [RequestSizeLimitFilter], e não a configuração de CORS do Spring MVC:
 * o 429 e o 413 que eles respondem também precisam do cabeçalho, senão o
 * browser esconde a resposta e o editor só vê "a API não respondeu".
 */
@Configuration
@ConditionalOnExpression("'\${mecqrsafe.cors.allowed-origins:}' != ''")
class CorsConfig(
    @param:Value("\${mecqrsafe.cors.allowed-origins}") private val allowedOrigins: List<String>,
) {
    @Bean
    fun corsFilter(): FilterRegistrationBean<CorsFilter> {
        val config = CorsConfiguration().apply {
            allowedOrigins = this@CorsConfig.allowedOrigins.map { it.trim() }.filter { it.isNotEmpty() }
            allowedMethods = listOf("GET", "POST")
            allowedHeaders = listOf("Content-Type")
            maxAge = 3600
        }
        val source = UrlBasedCorsConfigurationSource().apply { registerCorsConfiguration("/api/**", config) }
        return FilterRegistrationBean(CorsFilter(source)).apply { order = Ordered.HIGHEST_PRECEDENCE }
    }
}
