# Third-party software notices

The 24Seven.FM Player is built with the open-source components below. This
inventory reflects the resolved `releaseRuntimeClasspath` used for the M23.1
release-candidate audit on July 15, 2026. Artifact-level versions remain
recorded by Gradle and in the release bundle dependency metadata.

## Apache License 2.0 components

- AndroidX, including Activity 1.11.0, Browser 1.10.0, Compose UI 1.11.3,
  Material icons 1.7.8, Material 3 1.4.0, Core 1.16.0, Lifecycle 2.9.4,
  Media3 1.10.1, Window 1.5.0, and their AndroidX transitive modules
- Accompanist Drawable Painter 0.37.3
- Coil 3.4.0
- Guava 33.3.1-android and FailureAccess 1.0.2
- JetBrains Compose runtime transitive modules 1.9.3 and JetBrains AndroidX
  transitive modules 1.3.6/2.9.6
- Kotlin standard library 2.3.10, kotlinx.coroutines 1.10.2, and
  kotlinx.serialization 1.7.3
- OkHttp 4.12.0 and Okio 3.16.4
- JetBrains Annotations 23.0.0 and jspecify 1.0.0

These components are provided under the Apache License, Version 2.0. A copy
of that license is included in [LICENSE](LICENSE).

Project sources:

- <https://android.googlesource.com/platform/frameworks/support/>
- <https://github.com/androidx/media>
- <https://github.com/google/accompanist>
- <https://github.com/coil-kt/coil>
- <https://github.com/google/guava>
- <https://github.com/JetBrains/compose-multiplatform>
- <https://github.com/JetBrains/kotlin>
- <https://github.com/Kotlin/kotlinx.coroutines>
- <https://github.com/Kotlin/kotlinx.serialization>
- <https://github.com/square/okhttp>
- <https://github.com/square/okio>
- <https://github.com/JetBrains/java-annotations>
- <https://github.com/jspecify/jspecify>

## jsoup 1.22.2 — MIT License

Copyright (c) 2009-2026 Jonathan Hedley <https://jsoup.org/>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.

Source: <https://github.com/jhy/jsoup>

## desugar_jdk_libs 2.1.5 — GPLv2 with the Classpath Exception

Android's Java API desugaring library declares the GNU General Public License,
version 2, with the Classpath Exception. The exception permits linking the
library with independent modules and distributing the resulting executable
under terms of choice, subject to the license terms of each independent
module. The complete license and exception are available in the upstream
source repository:

- License and Classpath Exception: <https://github.com/google/desugar_jdk_libs/blob/master/LICENSE>
- Source: <https://github.com/google/desugar_jdk_libs>
- Version 2.1.5 release entry: <https://github.com/google/desugar_jdk_libs/blob/master/CHANGELOG.md#version-215-2025-02-14>

No local modifications are made to this library. It is processed by the
Android Gradle Plugin as part of core library desugaring.

## Public Suffix List data — Mozilla Public License 2.0

OkHttp contains `publicsuffixes.gz`, compiled from the Public Suffix List at
<https://publicsuffix.org/list/public_suffix_list.dat>. The data is subject to
the Mozilla Public License, Version 2.0:
<https://www.mozilla.org/MPL/2.0/>.

The upstream notice is retained in the release artifact at
`okhttp3/internal/publicsuffix/NOTICE`.
