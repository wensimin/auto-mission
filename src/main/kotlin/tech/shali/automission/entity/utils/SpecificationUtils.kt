package tech.shali.automission.entity.utils

import org.springframework.data.jpa.domain.Specification
import org.springframework.util.ObjectUtils
import java.lang.reflect.Field
import java.util.*
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinProperty

/**
 * 标记性质接口表示其中的字段均用于查询
 */
interface QueryParam {
    fun <T> toSpecification(): Specification<T> {
        val fields = this.javaClass.declaredFields
        return Specification { root, query, criteriaBuilder ->
            val specs = mutableListOf<Predicate>()
            fields.forEach {
                //忽略
                if (it.isAnnotationPresent(Ignore::class.java)) {
                    return@forEach
                }
                val value = it.kotlinProperty?.javaGetter?.invoke(this)
                //忽略空值项
                if (ObjectUtils.isEmpty(value)) {
                    return@forEach
                }
                // 声明为非空过编译
                value!!
                // 拥有eq注解||无任何注解的field 使用eq处理
                it.getAnnotation(Eq::class.java).also { eq ->
                    if (eq == null && it.annotations.isNotEmpty()) return@also
                    specs.add(eqSpecification(root, criteriaBuilder, it, value, eq))
                }
                it.getAnnotation(Like::class.java)?.let { like ->
                    specs.add(likeSpecification(like, root, criteriaBuilder, it, value))
                }
                it.getAnnotation(Less::class.java)?.let { less ->
                    specs.add(lessSpecification(less, root, criteriaBuilder, value))
                }
                it.getAnnotation(Greater::class.java)?.let { greater ->
                    specs.add(greaterSpecification(greater, root, criteriaBuilder, value))
                }
            }
            query.where(*specs.toTypedArray()).restriction
        }
    }

    private fun <T> greaterSpecification(
        greater: Greater,
        root: Root<T>,
        criteriaBuilder: CriteriaBuilder,
        value: Any
    ): Predicate {
        value as Comparable<Any>
        return when (greater.eq) {
            true -> criteriaBuilder.greaterThanOrEqualTo(root.get(greater.fieldName), value)
            false -> criteriaBuilder.greaterThan(root.get(greater.fieldName), value)
        }
    }

    private fun <T> lessSpecification(
        less: Less,
        root: Root<T>,
        criteriaBuilder: CriteriaBuilder,
        value: Any
    ): Predicate {
        value as Comparable<Any>
        return when (less.eq) {
            true -> criteriaBuilder.lessThanOrEqualTo(root.get(less.fieldName), value)
            false -> criteriaBuilder.lessThan(root.get(less.fieldName), value)
        }
    }

    /**
     * 生成eq查询规范
     */
    private fun <T> eqSpecification(
        root: Root<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any,
        eq: Eq? = null
    ): Predicate {
        val filedName = if (ObjectUtils.isEmpty(eq?.fieldName)) field.name else eq?.fieldName
        return criteriaBuilder.equal(root.get<Any>(filedName), value)
    }


    /**
     * 生成like查询方式
     */
    private fun <T> likeSpecification(
        like: Like,
        root: Root<T>,
        criteriaBuilder: CriteriaBuilder,
        field: Field,
        value: Any
    ): Predicate {
        val filedName = if (like.fieldName.isEmpty()) field.name else like.fieldName
        value as String
        val queryValue = when (like.type) {
            Like.Type.START -> "$value%"
            Like.Type.END -> "%$value"
            Like.Type.ALL -> "%$value%"
        }
        return criteriaBuilder.like(root.get(filedName), queryValue)
    }

}

/**
 * 字段标记，该字段不会用于查询
 */
@kotlin.annotation.Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Ignore

/**
 * 相等
 */
@kotlin.annotation.Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Eq(val fieldName: String = "")


/**
 * like查询
 */
@kotlin.annotation.Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Like(val type: Type = Type.START, val fieldName: String = "") {

    enum class Type {
        START, END, ALL
    }
}


/**
 * 指目标值应该小于标注的属性值
 */
@kotlin.annotation.Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Less(val fieldName: String = "", val eq: Boolean = true)

/**
 * 指目标值应该大于标注的属性值
 */
@kotlin.annotation.Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Greater(val fieldName: String = "", val eq: Boolean = true)
