package com.example.services

import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties

class Mapper {
    companion object {
        inline fun <reified TSource : Any, reified TDestination : Any> map(source: TSource): TDestination {
            val destinationType = TDestination::class
            val sourceType = TSource::class
            return map(source, sourceType, destinationType) as TDestination
        }

        fun map(source: Any?, sourceType: KClass<*>, destinationType: KClass<*>) : Any {
            val arguments: MutableList<Argument> = mutableListOf()
            for (member in sourceType.memberProperties) {
                val propertyType = member.returnType
                val propertyValue = member.call(source)
                arguments.add(Argument(member.name, propertyType, propertyValue))
            }
            val ctors = findConstructor(destinationType, arguments);
            if (ctors.isEmpty()) {
                throw Exception("No compatible constructor found")
            }
            val sorted = ctors.sortedByDescending { it.parameters.size }
            for (ctor in sorted) {
                val finalArguments: MutableList<Any?> = mutableListOf()
                var hasAllArguments = true
                for (parameter in ctor.parameters) {
                    val argument = arguments.firstOrNull { d -> d.name == parameter.name }
                    if (argument == null) {
                        hasAllArguments = false
                        break
                    }
                    if (parameter.type == argument.type) {
                        finalArguments.add(argument.value)
                    } else {
                        val argumentClass = argument.type.classifier as KClass<*>
                        val parameterClass = parameter.type.classifier as KClass<*>
                        val argument = map(argument.value, argumentClass, parameterClass)
                        finalArguments.add(argument)
                    }
                }
                if (hasAllArguments) {
                    var argumentsArray = arguments.toTypedArray()
                    return ctor.call(*argumentsArray)
                }
            }
            throw Exception("Should not get here")
        }

        fun <T : Any> findConstructor(
            typeClass: KClass<T>,
            arguments: List<Argument>): Array<KFunction<T>> {
            val result = mutableListOf<KFunction<T>>()
            for (constructor in typeClass.constructors) {
                if (compatible(constructor.parameters, arguments)) {
                    result.add(constructor)
                }
            }
            return result.toTypedArray()
        }

        private fun compatible(
            parameters: List<KParameter>,
            arguments: List<Argument>): Boolean {
            for (parameter in parameters) {
                var hasMatchingArgument: Boolean = false
                for (argument in arguments) {
                    if (parameter.name == argument.name) {
                        hasMatchingArgument = true
                        break
                    }
                }
                if (!hasMatchingArgument) {
                    return false
                }
            }
            return true
        }
    }

    class Argument(
        val name: String, val type: KType, val value: Any?
    )
}