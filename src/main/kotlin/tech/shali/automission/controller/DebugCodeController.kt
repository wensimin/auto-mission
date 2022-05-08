package tech.shali.automission.controller

import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.web.bind.annotation.*
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.DebugResult
import tech.shali.automission.service.DebugService
import tech.shali.automission.service.DebugWsService
import javax.validation.Valid

@RestController
@RequestMapping("debug")
class DebugCodeController(private val debugService: DebugService, private val debugWsService: DebugWsService) {

    @PostMapping
    suspend fun create(@RequestBody @Valid code: DebugCodeVo): DebugResult {
        return debugService.debugCode(code)
    }

    @GetMapping("{id}")
    suspend fun view(@PathVariable id: String): DebugResult? {
        return debugService.viewResult(id)
    }

    @DeleteMapping("{id}")
    suspend fun stop(@PathVariable id: String): DebugResult? {
        return debugService.stopDebug(id)
    }

    /**
     * 请求ws握手用token
     */
    @GetMapping("ws/token")
    suspend fun wsToken(token: JwtAuthenticationToken) = debugWsService.generateToken(token)

}