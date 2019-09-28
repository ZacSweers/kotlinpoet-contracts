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
package dev.zacsweers.kotlinpoet.contracts.metadata.specs

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.metadata.ImmutableKmContract
import com.squareup.kotlinpoet.metadata.ImmutableKmEffect
import com.squareup.kotlinpoet.metadata.ImmutableKmEffectExpression
import com.squareup.kotlinpoet.metadata.ImmutableKmType
import com.squareup.kotlinpoet.metadata.KotlinPoetMetadataPreview
import dev.zacsweers.kotlinpoet.contracts.ContractEffectExpressionSpec
import dev.zacsweers.kotlinpoet.contracts.ContractEffectSpec
import dev.zacsweers.kotlinpoet.contracts.ContractEffectType
import dev.zacsweers.kotlinpoet.contracts.ContractInvocationKind
import dev.zacsweers.kotlinpoet.contracts.ContractSpec
import kotlinx.metadata.Flag
import kotlinx.metadata.KmClassifier.Class
import kotlinx.metadata.KmClassifier.TypeAlias
import kotlinx.metadata.KmClassifier.TypeParameter
import kotlinx.metadata.KmConstantValue
import kotlinx.metadata.KmEffectInvocationKind
import kotlinx.metadata.KmEffectInvocationKind.AT_LEAST_ONCE
import kotlinx.metadata.KmEffectInvocationKind.AT_MOST_ONCE
import kotlinx.metadata.KmEffectInvocationKind.EXACTLY_ONCE
import kotlinx.metadata.KmEffectType
import kotlinx.metadata.KmEffectType.CALLS
import kotlinx.metadata.KmEffectType.RETURNS_CONSTANT
import kotlinx.metadata.KmEffectType.RETURNS_NOT_NULL
import kotlinx.metadata.isLocal

@KotlinPoetMetadataPreview
fun ImmutableKmContract.toContractSpec(): ContractSpec {
  return ContractSpec.builder()
      .addEffects(effects.map(ImmutableKmEffect::toContractEffectSpec))
      .build()
}

@KotlinPoetMetadataPreview
private fun ImmutableKmEffect.toContractEffectSpec(): ContractEffectSpec {
  return ContractEffectSpec.builder(type.toContractEffectType())
      .invocationKind(invocationKind?.toContractInvocationKind())
      .addConstructorArguments(
          constructorArguments.map(ImmutableKmEffectExpression::toContractEffectExpressionSpec))
      .conclusion(conclusion?.toContractEffectExpressionSpec())
      .build()
}

private fun KmEffectInvocationKind.toContractInvocationKind(): ContractInvocationKind {
  return when (this) {
    AT_MOST_ONCE -> ContractInvocationKind.AT_MOST_ONCE
    EXACTLY_ONCE -> ContractInvocationKind.EXACTLY_ONCE
    AT_LEAST_ONCE -> ContractInvocationKind.AT_LEAST_ONCE
  }
}

private fun KmEffectType.toContractEffectType(): ContractEffectType {
  return when (this) {
    RETURNS_CONSTANT -> ContractEffectType.RETURNS_CONSTANT
    CALLS -> ContractEffectType.CALLS
    RETURNS_NOT_NULL -> ContractEffectType.RETURNS_NOT_NULL
  }
}

@KotlinPoetMetadataPreview
private fun ImmutableKmEffectExpression.toContractEffectExpressionSpec(): ContractEffectExpressionSpec {
  return ContractEffectExpressionSpec.builder()
      .isNegated(Flag.EffectExpression.IS_NEGATED(flags))
      .isNullCheckPredicate(Flag.EffectExpression.IS_NULL_CHECK_PREDICATE(flags))
      .parameterIndex(parameterIndex)
      .constantValue(constantValue?.toCodeBlock())
      .isInstanceType(isInstanceType?.toTypeName())
      .addAndArguments(
          andArguments.map(ImmutableKmEffectExpression::toContractEffectExpressionSpec))
      .addOrArguments(orArguments.map(ImmutableKmEffectExpression::toContractEffectExpressionSpec))
      .build()
}

private fun KmConstantValue.toCodeBlock(): CodeBlock {
  return CodeBlock.of("%L", value)
}

@KotlinPoetMetadataPreview
private fun ImmutableKmType.toTypeName(): TypeName {
  return when (val localClassifier = classifier) {
    is Class -> bestGuessClassName(localClassifier.name)
    is TypeParameter -> TODO("Unsupported type: TypeParameter")
    is TypeAlias -> TODO("Unsupported type: TypeAlias")
  }
}

/**
 * Copied from KotlinPoet's ClassInspectorUtil.
 *
 * Best guesses a [ClassName] as represented in Metadata's [kotlinx.metadata.ClassName], where
 * package names in this name are separated by '/' and class names are separated by '.'.
 *
 * For example: `"org/foo/bar/Baz.Nested"`.
 *
 * Local classes are prefixed with ".", but for KotlinPoetMetadataSpecs' use case we don't deal
 * with those.
 */
private fun bestGuessClassName(kotlinMetadataName: String): ClassName {
  require(!kotlinMetadataName.isLocal) {
    "Local/anonymous classes are not supported!"
  }
  // Top-level: package/of/class/MyClass
  // Nested A:  package/of/class/MyClass.NestedClass
  val simpleName = kotlinMetadataName.substringAfterLast(
      '/', // Drop the package name, e.g. "package/of/class/"
      '.' // Drop any enclosing classes, e.g. "MyClass."
  )
  val packageName = kotlinMetadataName.substringBeforeLast("/", missingDelimiterValue = "")
  val simpleNames = kotlinMetadataName.removeSuffix(simpleName)
      .removeSuffix(".") // Trailing "." if any
      .removePrefix(packageName)
      .removePrefix("/")
      .let {
        if (it.isNotEmpty()) {
          it.split(".")
        } else {
          // Don't split, otherwise we end up with an empty string as the first element!
          emptyList()
        }
      }
      .plus(simpleName)

  return ClassName(
      packageName = packageName.replace("/", "."),
      simpleNames = simpleNames
  )
}

private fun String.substringAfterLast(vararg delimiters: Char): String {
  val index = lastIndexOfAny(delimiters)
  return if (index == -1) this else substring(index + 1, length)
}
