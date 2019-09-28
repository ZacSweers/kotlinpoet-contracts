/*
 * Copyright (c) 2019 Zac Sweers
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.zacsweers.kotlinpoet.contracts

import com.google.common.truth.Truth.assertThat
import com.squareup.kotlinpoet.ANY
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.UNIT
import dev.zacsweers.kotlinpoet.contracts.ContractInvocationKind.AT_MOST_ONCE
import dev.zacsweers.kotlinpoet.contracts.ContractInvocationKind.EXACTLY_ONCE
import org.junit.Assert.fail
import org.junit.Test

class ContractSpecTest {

  @Test
  fun calls() {
    val param = ParameterSpec.builder(
        "body",
        LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
    ).build()
    val contractSpec = ContractSpec.builder()
        .addEffect(ContractEffectSpec.calls(param, EXACTLY_ONCE))
        .build()
    val function = FunSpec.builder("test")
        .addModifiers(KModifier.INLINE)
        .addParameter(param)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      inline fun test(body: (kotlin.String) -> kotlin.String) {
        kotlin.contracts.contract {
          callsInPlace(body, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
        }
      }
    """.trimIndent())
  }

  @Test
  fun callsTestMissingInline() {
    val param = ParameterSpec.builder(
        "body",
        LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
    ).build()
    val contractSpec = ContractSpec.builder()
        .addEffect(ContractEffectSpec.calls(param, EXACTLY_ONCE))
        .build()
    try {
      FunSpec.builder("test")
          .addParameter(param)
          .build()
          .withContract(contractSpec)
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessageThat().contains("functions with callsInPlace effects must be inline")
    }
  }

  @Test
  fun callsTestParamNotFunction() {
    val param = ParameterSpec.builder("body", STRING).build()
    try {
      ContractEffectSpec.calls(param, EXACTLY_ONCE)
      fail()
    } catch (e: IllegalArgumentException) {
      assertThat(e).hasMessageThat().contains(
          "callsInPlace is only applicable to function parameters")
    }
  }

  @Test
  fun returnsNotNullTest() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returnsNotNull(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(UNIT.copy(nullable = true))
        .addParameter(param)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Unit? {
        kotlin.contracts.contract {
          returnsNotNull() implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun returnsNotNullWithReceiver() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returnsNotNull(
                conclusion = ContractEffectExpressionSpec.nullCheck(0, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .receiver(STRING.copy(nullable = true))
        .addParameter(param)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun kotlin.String?.test(param: kotlin.String?) {
        kotlin.contracts.contract {
          returnsNotNull() implies (this@test != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun returnsTest() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(UNIT.copy(nullable = true))
        .addParameter(param)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Unit? {
        kotlin.contracts.contract {
          returns() implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun instanceTest() {
    val param = ParameterSpec.builder("param", ANY)
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.isInstance(STRING, 1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.Any) {
        kotlin.contracts.contract {
          returns() implies (param !is kotlin.String)
        }
      }
    """.trimIndent())
  }

  @Test
  fun returnsValueTest() {
    val param = ParameterSpec.builder("param", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returnsValue(
                value = CodeBlock.of("1"),
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .returns(INT)
        .addParameter(param)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param: kotlin.String?): kotlin.Int {
        kotlin.contracts.contract {
          returns(1) implies (param != null)
        }
      }
    """.trimIndent())
  }

  @Test
  fun booleanConclusion() {
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.constantValue(true)
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .receiver(BOOLEAN)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun kotlin.Boolean.test() {
        kotlin.contracts.contract {
          returns() implies (true)
        }
      }
    """.trimIndent())
  }

  @Test
  fun multipleEffects() {
    val lambda = LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
    val param1 = ParameterSpec.builder(
        "body1",
        lambda
    ).build()
    val param2 = ParameterSpec.builder(
        "body2",
        lambda
    ).build()

    val effect1 = ContractEffectSpec.calls(param1, EXACTLY_ONCE)
    val effect2 = ContractEffectSpec.calls(param2, AT_MOST_ONCE)

    val contractSpec = ContractSpec.builder()
        .addEffect(effect1)
        .addEffect(effect2)
        .build()
    val function = FunSpec.builder("test")
        .addModifiers(KModifier.INLINE)
        .addParameter(param1)
        .addParameter(param2)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
        inline fun test(body1: (kotlin.String) -> kotlin.String, body2: (kotlin.String) -> kotlin.String) {
          kotlin.contracts.contract {
            callsInPlace(body1, kotlin.contracts.InvocationKind.EXACTLY_ONCE)
            callsInPlace(body2, kotlin.contracts.InvocationKind.AT_MOST_ONCE)
          }
        }
    """.trimIndent())
  }

  @Test
  fun andArguments() {
    val param1 = ParameterSpec.builder("param1", STRING.copy(nullable = true))
        .build()
    val param2 = ParameterSpec.builder("param2", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
                    .and(ContractEffectExpressionSpec.nullCheck(2, true))
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param1)
        .addParameter(param2)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param1: kotlin.String?, param2: kotlin.String?) {
        kotlin.contracts.contract {
          returns() implies (param1 != null && (param2 != null))
        }
      }
    """.trimIndent())
  }

  @Test
  fun orArguments() {
    val param1 = ParameterSpec.builder("param1", STRING.copy(nullable = true))
        .build()
    val param2 = ParameterSpec.builder("param2", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
                    .or(ContractEffectExpressionSpec.nullCheck(2, true))
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param1)
        .addParameter(param2)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(param1: kotlin.String?, param2: kotlin.String?) {
        kotlin.contracts.contract {
          returns() implies (param1 != null || (param2 != null))
        }
      }
    """.trimIndent())
  }

  @Test
  fun andOrArguments() {
    val param1 = ParameterSpec.builder("param1", STRING.copy(nullable = true))
        .build()
    val param2 = ParameterSpec.builder("param2", STRING.copy(nullable = true))
        .build()
    val param3 = ParameterSpec.builder("param3", STRING.copy(nullable = true))
        .build()
    val contractSpec = ContractSpec.builder()
        .addEffect(
            ContractEffectSpec.returns(
                conclusion = ContractEffectExpressionSpec.nullCheck(1, true)
                    .or(ContractEffectExpressionSpec.nullCheck(2, true))
                    .and(ContractEffectExpressionSpec.nullCheck(3, true))
            )
        )
        .build()
    val function = FunSpec.builder("test")
        .addParameter(param1)
        .addParameter(param2)
        .addParameter(param3)
        .build()
        .withContract(contractSpec)

    //language=kotlin
    assertThat(function.toString().trim()).isEqualTo("""
      fun test(
        param1: kotlin.String?,
        param2: kotlin.String?,
        param3: kotlin.String?
      ) {
        kotlin.contracts.contract {
          returns() implies (param1 != null && (param3 != null) || (param2 != null))
        }
      }
    """.trimIndent())
  }

  // TODO more error cases
  // TODO complex
}
