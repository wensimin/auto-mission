package tech.shali.automission.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.kotlin.core.publisher.toMono


@Configuration(proxyBeanMethods = false)
class ResourceServerConfig {
//
//    @Bean
//    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
//        http
//            .authorizeRequests().apply {
//                // 本项目所有接口目前全部需要admin
//                anyRequest().hasAnyAuthority("ADMIN")
//            }.and()
//            .oauth2ResourceServer()
//            .jwt()
//        return http.build()
//    }

    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity,
        jwtAuthenticationConverter: JwtAuthenticationConverter
    ): SecurityWebFilterChain? {
        http.authorizeExchange().apply {
            anyExchange().hasAnyAuthority("ADMIN")
        }.and()
            .oauth2ResourceServer()
            .jwt().jwtAuthenticationConverter {
                // scope & auth 本项目 混用
                val scope = it.getClaim<Collection<String>?>("scope")
                val auth = it.getClaim<Collection<String>?>("auth")
                JwtAuthenticationToken(it, (scope.orEmpty() + auth.orEmpty()).map { s -> SimpleGrantedAuthority(s) })
                    .toMono()
            }
        return http.build()
    }


}