package com.example.services

import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.typeOf

class ServiceProvider {
    private val mappings: HashMap<KType, Any> = HashMap()

    fun add(type: KType, instance: Any) {
        mappings[type] = instance
    }

    inline fun <reified T : Any> getService(): T {
        return getService(typeOf<T>().classifier as KClass<*>) as T
    }

    fun getService(targetType: KClass<*>) : Any? {
        fun canConstruct(parameters: List<KParameter>) : List<Any?>? {
            val result = mutableListOf<Any?>()
            for (parameter in parameters) {
                val argument = mappings.getOrDefault(parameter.type, null)
                result.add(argument)
            }
            return result
        }
        fun createInstance() : Any {
            for (ctor in targetType.constructors) {
                val arguments = canConstruct(ctor.parameters)
                if (arguments != null) {
                    return ctor.call(*arguments.toTypedArray())
                }
            }
            throw Exception("Could find constructor with available arguments")
        }
        return createInstance()
    }
}