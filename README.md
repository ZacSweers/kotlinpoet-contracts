KotlinPoet-Contracts
===================================

## 🚧 WIP 🚧


## Usage

### KotlinPoet

The core component of `kotlinpoet-contracts` is a `ContractSpec`.

```kotlin
val param = ParameterSpec.builder(
    "body",
    LambdaTypeName.get(parameters = *arrayOf(STRING), returnType = STRING)
).build()
val contractSpec = ContractSpec.builder()
    .addEffect(ContractEffectSpec.calls(param, EXACTLY_ONCE))
    .build()
```

You can create a function with it from an existing function via `withContract()` extension.

```kotlin
val function = FunSpec.builder("stillAlive")
    .addModifiers(KModifier.INLINE)
    .addParameter(param)
    .addStatement("val result = body()")
    .addStatement("println(%S)", "This was a triumph: $result")
    .build()
    .withContract(contractSpec)
```

`function`'s output is now:

```kotlin
inline fun stillAlive(body: (String) -> String) {
  contract {
    callsInPlace(body, InvocationKind.EXACTLY_ONCE)
  }
  val result = body()
  println("This was a triumph: $result")
}
```

### KotlinPoet-metadata-specs

Using the `kotlinpoet-contracts-metadata-specs` artifact, you can convert Kotlin metadata representations 
of contracts into a `ContractSpec`.

TODO - KotlinPoet-metadata-specs doesn't support files with only top-level functions yet.

## Installation

Core `ContractSpec` + KotlinPoet extensions

```gradle
implementation "dev.zacsweers.kotlinpoetcontracts:kotlinpoet-contracts:{version}"
```

Metadata extensions

```gradle
implementation "dev.zacsweers.kotlinpoetcontracts:kotlinpoet-contracts-metadata-specs:{version}"
```

License
-------

    Copyright (C) 2019 Zac Sweers

    Licensed under the Apache License, Version 2.0 (the "License");
    you may not use this file except in compliance with the License.
    You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

    Unless required by applicable law or agreed to in writing, software
    distributed under the License is distributed on an "AS IS" BASIS,
    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
    See the License for the specific language governing permissions and
    limitations under the License.
