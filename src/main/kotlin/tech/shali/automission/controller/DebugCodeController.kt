package tech.shali.automission.controller

import org.springframework.web.bind.annotation.*
import tech.shali.automission.pojo.DebugCodeVo
import tech.shali.automission.pojo.DebugResult
import tech.shali.automission.service.DebugService
import javax.validation.Valid

@RestController
@RequestMapping("debug")
class DebugCodeController (private val debugService: DebugService){

    @PostMapping
    fun create(@RequestBody @Valid code: DebugCodeVo): DebugResult {
        return debugService.debugCode(code)
    }
    @GetMapping("{id}")
    fun view(@PathVariable id: String): DebugResult?{
        return debugService.viewResult(id)
    }
    @DeleteMapping("{id}")
    fun stop(@PathVariable id: String): DebugResult?{
        return debugService.stopDebug(id)
    }

}