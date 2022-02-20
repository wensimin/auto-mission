package tech.shali.automission.config

import org.springframework.context.annotation.Bean
import org.springframework.core.convert.converter.Converter
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.web.SecurityFilterChain


@EnableWebSecurity
class ResourceServerConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        http
            .authorizeRequests().apply {
                // 本项目所有接口目前全部需要admin
                anyRequest().hasAnyAuthority("ADMIN")
            }.and()
            .oauth2ResourceServer()
            .jwt()
        return http.build()
    }

    /**
     * 自定义转换jwt的权限
     */
    @Bean
    fun jwtAuthenticationConverter(): JwtAuthenticationConverter {
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter {
            // scope & auth 本项目 混用
            val scope = it.getClaim<Collection<String>?>("scope")
            val auth = it.getClaim<Collection<String>?>("auth")
            ((scope ?: emptyList()) + (auth ?: emptyList())).map { s -> SimpleGrantedAuthority(s) }
        }
        return jwtAuthenticationConverter
    }

}