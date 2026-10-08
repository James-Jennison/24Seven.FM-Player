# Third-party software notices

The 24Seven.FM Player is built with the third-party components below. This
inventory reflects the resolved `releaseRuntimeClasspath` as checked on
October 1, 2026. Artifact-level versions remain recorded by Gradle and in the
release bundle dependency metadata.

## Apache License 2.0 components

- AndroidX, including Activity 1.11.0, Browser 1.10.0, Compose UI 1.11.3,
  Material icons 1.7.8, Material 3 1.4.0, Core 1.16.0, Lifecycle 2.9.4,
  Media 1.7.0, Media3 1.10.1, MediaRouter 1.8.1, Window 1.5.0, and their
  AndroidX transitive modules
- Accompanist Drawable Painter 0.37.3
- Coil 3.4.0
- Firebase Encoders 17.0.0, with its JSON 18.0.0 and Protobuf 16.0.0 encoders
- Google Data Transport API 3.0.0, runtime 3.1.3, and CCT backend 3.1.3
- Guava 33.3.1-android and FailureAccess 1.0.2
- javax.inject 1
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
- <https://github.com/firebase/firebase-android-sdk>
- <https://github.com/google/guava>
- <https://github.com/javax-inject/javax-inject>
- <https://github.com/JetBrains/compose-multiplatform>
- <https://github.com/JetBrains/kotlin>
- <https://github.com/Kotlin/kotlinx.coroutines>
- <https://github.com/Kotlin/kotlinx.serialization>
- <https://github.com/square/okhttp>
- <https://github.com/square/okio>
- <https://github.com/JetBrains/java-annotations>
- <https://github.com/jspecify/jspecify>

## Google Play services — Android Software Development Kit License

The Google Cast sender is built with Google Play services libraries that
Google distributes in binary form under the Android Software Development Kit
License rather than an open-source license:

- Play services Cast 22.3.1 and Cast Framework 22.3.1
- Play services Base 18.7.2, Basement 18.9.0, Tasks 18.3.2, and Flags 18.1.0

License: <https://developer.android.com/studio/terms.html>

These libraries are used unmodified. The Firebase Encoders and Google Data
Transport components listed above are their open-source dependencies.

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

## DejaVu Sans Mono 2.37 — Bitstream Vera Fonts License

The station chat is set in DejaVu Sans Mono (regular and bold), bundled
unmodified.

Copyright (c) 2003 by Bitstream, Inc. All Rights Reserved. Bitstream Vera is a
trademark of Bitstream, Inc. DejaVu changes are in public domain.

Permission is hereby granted, free of charge, to any person obtaining a copy
of the fonts accompanying this license ("Fonts") and associated documentation
files (the "Font Software"), to reproduce and distribute the Font Software,
including without limitation the rights to use, copy, merge, publish,
distribute, and/or sell copies of the Font Software, and to permit persons to
whom the Font Software is furnished to do so, subject to the following
conditions:

The above copyright and trademark notices and this permission notice shall be
included in all copies of one or more of the Font Software typefaces.

The Font Software may be modified, altered, or added to, and in particular the
designs of glyphs or characters in the Fonts may be modified and additional
glyphs or characters may be added to the Fonts, only if the fonts are renamed
to names not containing either the words "Bitstream" or the word "Vera".

This License becomes null and void to the extent applicable to Fonts or Font
Software that has been modified and is distributed under the "Bitstream Vera"
names.

The Font Software may be sold as part of a larger software package but no copy
of one or more of the Font Software typefaces may be sold by itself.

THE FONT SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS
OR IMPLIED, INCLUDING BUT NOT LIMITED TO ANY WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT OF COPYRIGHT, PATENT,
TRADEMARK, OR OTHER RIGHT. IN NO EVENT SHALL BITSTREAM OR THE GNOME FOUNDATION
BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, INCLUDING ANY GENERAL,
SPECIAL, INDIRECT, INCIDENTAL, OR CONSEQUENTIAL DAMAGES, WHETHER IN AN ACTION
OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF THE USE OR INABILITY TO
USE THE FONT SOFTWARE OR FROM OTHER DEALINGS IN THE FONT SOFTWARE.

Except as contained in this notice, the names of Gnome, the Gnome Foundation,
and Bitstream Inc., shall not be used in advertising or otherwise to promote
the sale, use or other dealings in this Font Software without prior written
authorization from the Gnome Foundation or Bitstream Inc., respectively. For
further information, contact: fonts at gnome dot org.

Source: <https://dejavu-fonts.github.io/>

## Public Suffix List data — Mozilla Public License 2.0

OkHttp contains `publicsuffixes.gz`, compiled from the Public Suffix List at
<https://publicsuffix.org/list/public_suffix_list.dat>. The data is subject to
the Mozilla Public License, Version 2.0:
<https://www.mozilla.org/MPL/2.0/>.

The upstream notice is retained in the release artifact at
`okhttp3/internal/publicsuffix/NOTICE`.
