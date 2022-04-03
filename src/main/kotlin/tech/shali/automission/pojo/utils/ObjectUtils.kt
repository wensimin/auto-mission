package tech.shali.automission.pojo.utils

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.full.memberProperties

/**
 * kotlin版本使用constructor 简易的复制属性方法,用于bean copy
 *
 * 由于save object使用jsr303进行校验参数,声明时使用了可空类型,因此在copy中仅对比命名不进行类型校验
 *
 * @param fromType 简易的使用了first constructor ,使用时应保证只有单个constructor
 */
fun <T : Any, R : Any> T.copyTO(fromType: KClass<R>): R {
    val props = this::class.memberProperties
    val constructor = fromType.constructors.first()
    val params = mutableMapOf<KParameter, Any?>()
    constructor.parameters.forEach { constructorParam ->
        props.find {
            it.name == constructorParam.name
        }?.let {
            params[constructorParam] = it.getter.call(this)
        }
    }
    return constructor.callBy(params)
}