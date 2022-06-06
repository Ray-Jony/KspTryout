package com.lovzoe

import com.google.devtools.ksp.processing.*
import com.google.devtools.ksp.symbol.*
import com.google.devtools.ksp.validate
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec

class IntSummableProcessor(
    private val options: Map<String, String>,
    private val codeGenerator: CodeGenerator,
    private val logger: KSPLogger
) : SymbolProcessor {
    private lateinit var intType: KSType

    override fun process(resolver: Resolver): List<KSAnnotated> {
        intType = resolver.builtIns.intType
        val symbols = resolver.getSymbolsWithAnnotation(IntSummable::class.qualifiedName!!)
        val unableToProcess = symbols.filterNot { it.validate() }
        symbols.filter { it is KSClassDeclaration && it.validate() }
            .forEach { it.accept(Visitor(), Unit) }

        return unableToProcess.toList()
    }

    inner class Visitor : KSVisitorVoid() {
        private lateinit var className: String
        private lateinit var packageName: String
        private val summables: MutableList<String> = mutableListOf()

        override fun visitClassDeclaration(classDeclaration: KSClassDeclaration, data: Unit) {
            val qualifiedName = classDeclaration.qualifiedName?.asString()

            if (!classDeclaration.isDataClass()) {
                logger.error("@IntSummable cannot target non-data class $qualifiedName", classDeclaration)
                return
            }

            if (qualifiedName == null) {
                logger.error("@IntSummable must target qualified names", classDeclaration)
                return
            }
            className = qualifiedName
            packageName = classDeclaration.packageName.asString()

            classDeclaration.getAllProperties()
                .forEach { it.accept(this, Unit) }

            if (summables.isEmpty()) return

            val fileSpec = FileSpec.builder(
                packageName = packageName,
                fileName = classDeclaration.simpleName.asString()
            ).apply {
                addFunction(
                    FunSpec.builder("sumInts")
                        .receiver(ClassName.bestGuess(className))
                        .returns(Int::class)
                        .addStatement("val sum = ${summables.joinToString(" + ")}")
                        .addStatement("return sum")
                        .build()
                )
            }.build()

            codeGenerator.createNewFile(
                dependencies = Dependencies(aggregating = false),
                packageName = packageName,
                fileName = classDeclaration.simpleName.asString() + "Ext"
            ).use { outputStream ->
                outputStream.writer().use { fileSpec.writeTo(it) }
            }
        }

        override fun visitPropertyDeclaration(property: KSPropertyDeclaration, data: Unit) {
            if (property.type.resolve().isAssignableFrom(intType)) {
                val name = property.simpleName.asString()
                summables.add(name)
            }
        }

        private fun KSClassDeclaration.isDataClass() = modifiers.contains(Modifier.DATA)


    }
}