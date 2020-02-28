package com.slab.marketcoco

import com.bsidesoft.mvc.bsApiInterceptor
import com.bsidesoft.resource.bsLoader
import com.slab.marketcoco.env.Env
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.builders.WebSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter
import org.springframework.web.servlet.config.annotation.CorsRegistry
import org.springframework.web.servlet.config.annotation.InterceptorRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import javax.annotation.PostConstruct

@Configuration
@EnableWebSecurity
class Setting(e: Environment) : WebSecurityConfigurerAdapter(), WebMvcConfigurer {
    private val env by lazy { Env.env(e) }

    @Bean
    fun env() = env

    @PostConstruct
    fun init(){
        bsLoader(env.settingPathes)
        Scheduler.run()
    }
    override fun configure(http: HttpSecurity) {
        http.csrf().disable()
        http.authorizeRequests().anyRequest().permitAll()
    }

    override fun configure(web: WebSecurity?) {
        web?.ignoring()?.antMatchers(HttpMethod.GET, "/**/*.{html,css,js,map,png,jpg,jpeg,svg,ico,gif}")
    }
    @Profile("!real")
    override fun addCorsMappings(registry: CorsRegistry){
        registry.addMapping("/**")
                .allowedOrigins("*")
                .allowedMethods("*")
                .allowCredentials(false)
                .maxAge(3600)
    }
    override fun addInterceptors(registry: InterceptorRegistry) {
        registry.addInterceptor(bsApiInterceptor()).addPathPatterns("/api/**")
    }
}