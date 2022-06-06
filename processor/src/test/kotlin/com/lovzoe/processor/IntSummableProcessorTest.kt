package com.lovzoe.processor

import com.lovzoe.IntSummableProcessor
import com.lovzoe.IntSummableProcessorProvider
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.symbolProcessorProviders
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import kotlin.test.assertEquals


class IntSummableProcessorTest {

    @TempDir
    lateinit var tempDir: File

    @Test
    fun testDataClass() {
        val sourceFile = SourceFile.kotlin(
            "Foo.kt",
            """
            package com.example
             import com.lovzoe.IntSummable
            
            @IntSummable
            data class Foo {
                val x: Int
                val y: Int
            }
            """.trimIndent(), false
        )

        val compilationResult = compile(sourceFile)

        assertSourceEquals(
            """
            package com.example
            fun Foo.sumInts() = x + y
            """.trimIndent(), compilationResult.sourceFor("FooExt.kt")
        )
    }

    private fun compile(vararg source: SourceFile) = KotlinCompilation().apply {
        sources = source.toList()
        symbolProcessorProviders = listOf(IntSummableProcessorProvider())
        workingDir = tempDir
        inheritClassPath = true
        verbose = false
    }.compile()

    private fun assertSourceEquals(@Language("kotlin") expected: String, actual: String) {
        assertEquals(expected, actual)
    }

    private fun KotlinCompilation.Result.sourceFor(fileName: String): String {
        return kspGeneratedSources().find { it.name == fileName }?.readText()
            ?: throw IllegalArgumentException("Could not find file $fileName in ${kspGeneratedSources()}")
    }

    private fun KotlinCompilation.Result.kspGeneratedSources(): List<File> {
        with(workingDir.resolve("ksp").resolve("sources")) {
            val kotlinGeneratedDir = resolve("kotlin")
            val javaGeneratedDir = resolve("java")
            return kotlinGeneratedDir.walk().toList() + javaGeneratedDir.walk().toList()
        }
    }

    private val KotlinCompilation.Result.workingDir: File
        get() = checkNotNull(outputDirectory.parentFile)

}