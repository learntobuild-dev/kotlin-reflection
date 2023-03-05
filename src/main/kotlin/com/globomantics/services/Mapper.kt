package com.example.services

import kotlin.reflect.*
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

class Mapper {
    companion object {
        inline fun <reified TSource : Any, reified TDestination : Any> map(source: TSource): TDestination {
            val destinationType = TDestination::class
            val sourceType = TSource::class
            return map(source, sourceType, destinationType) as TDestination
        }

        fun map(
            source: Any?,
            sourceType: KClass<*>,
            destinationType: KClass<*>) : Any {
            val arguments: MutableList<Argument> = mutableListOf()
            for (member in sourceType.memberProperties) {
                val propertyType = member.returnType
                val propertyValue = member.call(source)
                arguments.add(Argument(member.name, propertyType, propertyValue))
            }
            val ctors = findConstructor(destinationType, arguments);
            if (ctors.isNotEmpty()) {
                val sorted =
                    ctors
                        .filter { it.parameters.isNotEmpty() }
                        .sortedByDescending { it.parameters.size }
                for (ctor in sorted) {
                    val finalArguments: MutableList<Any?> = mutableListOf()
                    var hasAllArguments = true
                    for (parameter in ctor.parameters) {
                        val descriptor = arguments.firstOrNull { d -> d.name == parameter.name }
                        if (descriptor == null) {
                            hasAllArguments = false
                            break
                        }
                        if (parameter.type == descriptor.type) {
                            finalArguments.add(descriptor.value)
                        } else {
                            val argumentClass = descriptor.type.classifier as KClass<*>
                            val parameterClass = parameter.type.classifier as KClass<*>
                            val argument = map(descriptor.value, argumentClass, parameterClass)
                            finalArguments.add(argument)
                        }
                    }
                    if (hasAllArguments) {
                        var argumentsArray = finalArguments.toTypedArray()
                        return ctor.call(*argumentsArray)
                    }
                }
            }
            val parameterlessCtor =
                ctors.firstOrNull { it.parameters.isEmpty() }
                    ?: throw Exception("Could not find a parameterless constructor")
            val newInstance = parameterlessCtor.call()
            for (argument in arguments) {
                var valueSet = false
                for (property in destinationType.declaredMemberProperties) {
                    if (property.name == argument.name) {
                        if (property is KMutableProperty<*>) {
                            property.setter.call(newInstance, argument.value)
                            valueSet = true
                        }
                        if (property is KMutableProperty0<*>) {
                            property.setter.call(newInstance, argument.value)
                            valueSet = true
                        }
                    }
                }
                if (!valueSet) {
                    throw Exception(
                        "Argument " + argument.name +
                                " does not have a matching property in destination type")
                }
            }
            return newInstance
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