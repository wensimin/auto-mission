package tech.shali.automission.service

/**
 *  全局使用的kv store
 */
interface KVStore {

    fun get(key: String): String?
    fun set(key: String, value: String?): String?
    fun del(key: String)
}