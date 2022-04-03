package tech.shali.automission.entity.utils

import org.springframework.data.jpa.domain.Specification
import org.springframework.util.ObjectUtils
import java.lang.reflect.Field
import javax.persistence.criteria.CriteriaBuilder
import javax.persistence.criteria.Predicate
import javax.persistence.criteria.Root
import kotlin.reflect.jvm.javaGetter
import kotlin.reflect.jvm.kotlinProperty

/**
 * 查询参数接口,实现了一组依赖注解的查询规范
 * 未进行标记的成员会生成 eq 查询规范
 * 空值成员会进行跳过
 */
interface QueryParam {
    /**
     * 通过当前类的成员以及注解生成查询参数
     */
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

    /**
     * 生成大于目标规范
     */
    private fun <T> greaterSpecification(
        greater: Greater,
        root: Root<T>,
        criteriaBuilder: CriteriaBuilder,
        value: Any
    ): Predicate {
        @Suppress("UNCHECKED_CAST")
        value as Comparable<Any>
        return when (greater.eq) {
            true -> criteriaBuilder.greaterThanOrEqualTo(root.get(greater.fieldName), value)
            false -> criteriaBuilder.greaterThan(root.get(greater.fieldName), value)
        }
    }

    /**
     * 生成小于目标查询规范
     */
    private fun <T> lessSpecification(
        less: Less,
        root: Root<T>,
        criteriaBuilder: CriteriaBuilder,
        value: Any
    ): Predicate {
        @Suppress("UNCHECKED_CAST")
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
        val filedName = eq?.fieldName?.ifEmpty { field.name }
        return if (eq?.igCase == true && value is String) {
            criteriaBuilder.equal(criteriaBuilder.upper(root.get(filedName)), value.uppercase())
        } else {
            criteriaBuilder.equal(root.get<Any>(filedName), value)
        }
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
        val filedName = like.fieldName.ifEmpty { field.name }
        // like must string
        if (value !is String) throw TypeCastException("like must string")
        val predicateSet = mutableSetOf<Predicate>()
        // 若是分隔符存在则分割关键字查询
        if (like.separator.isNotEmpty()) {
            value.split(like.separator).forEach {
                criteriaBuilder.like(root.get(filedName), getLikeValue(like.type, it))
                predicateSet.add(criteriaBuilder.like(root.get(filedName), getLikeValue(like.type, it)))
            }
        } else {
            predicateSet.add(criteriaBuilder.like(root.get(filedName), getLikeValue(like.type, value)))
        }
        return criteriaBuilder.and(*predicateSet.toTypedArray())
    }

    private fun getLikeValue(type: Like.Type, value: String): String {
        return when (type) {
            Like.Type.START -> "$value%"
            Like.Type.END -> "%$value"
            Like.Type.ALL -> "%$value%"
        }
    }

}

/**
 * 忽略标记，该字段不会用于查询
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Ignore

/**
 * 相等
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Eq(val fieldName: String = "", val igCase: Boolean = false)


/**
 * like查询
 * 本注解仅支持String
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Like(
    /**
     * like模式
     */
    val type: Type = Type.START,
    val fieldName: String = "",
    /**
     * 分割符,如果该属性不为空则会对查询参数进行分割 and 匹配
     */
    val separator: String = ""
) {

    enum class Type {
        START, END, ALL
    }
}


/**
 * 指目标值应该小于标注的属性值
 * @param fieldName 必须,对应的查询目标field
 * 本注解只应该使用在实现了 [java.lang.Comparable] 接口的对象上
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Less(val fieldName: String, val eq: Boolean = true)

/**
 * 指目标值应该大于标注的属性值
 *  @param fieldName 必须,对应的查询目标field
 * 本注解只应该使用在实现了 [java.lang.Comparable] 接口的对象上
 */
@Target(AnnotationTarget.FIELD)
@Retention
@MustBeDocumented
annotation class Greater(val fieldName: String, val eq: Boolean = true)
