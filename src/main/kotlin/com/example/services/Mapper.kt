package com.example.services

import kotlin.reflect.*

class Mapper {
    companion object {
        inline fun <reified TSource : Any, reified TDestination : Any> map(source: TSource): TDestination {
            val destinationType = TDestination::class
            val sourceType = TSource::class
            return map(source, sourceType, destinationType) as TDestination
        }

        fun map(source: Any?, sourceType: KClass<*>, destinationType: KClass<*>) : Any {
            val descriptors: MutableList<ArgumentDescriptor> = mutableListOf()
            for (member in sourceType.members) {
                if (member is KProperty0<*> ||
                    member is KProperty1<*, *> ||
                    member is KProperty2<*, *, *>) {
                    val propertyType = member.returnType
                    val propertyValue = member.call(source)
                    descriptors.add(ArgumentDescriptor(member.name, propertyType, propertyValue))
                }
            }
            val ctors = findConstructor(destinationType, descriptors);
            if (ctors.isEmpty()) {
                throw Exception("No compatible constructor found")
            }
            val sorted = ctors.sortedByDescending { it.parameters.size }
            for (ctor in sorted) {
                val arguments: MutableList<Any?> = mutableListOf();
                for (parameter in ctor.parameters) {
                    val descriptor =
                        descriptors.firstOrNull { d -> d.name == parameter.name }
                            ?: continue
                    if (parameter.type == descriptor.type) {
                        arguments.add(descriptor.value)
                    } else {
                        val argumentClass = descriptor.type.classifier as KClass<*>
                        val parameterClass = parameter.type.classifier as KClass<*>
                        val argument = map(descriptor.value, argumentClass, parameterClass)
                        arguments.add(argument)
                    }
                }
                var argumentsArray = arguments.toTypedArray()
                return ctor.call(*argumentsArray)
            }
            throw Exception("Should not get here")
        }

        fun <T : Any> findConstructor(
            typeClass: KClass<T>,
            arguments: List<ArgumentDescriptor>): Array<KFunction<T>> {
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
            arguments: List<ArgumentDescriptor>): Boolean {
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

    class ArgumentDescriptor(
        val name: String, val type: KType, val value: Any?
    )
}