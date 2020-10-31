/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.soywiz.korau.format.org.gragravarr.flac

import com.soywiz.korio.stream.*


/**
 * Any Flac Metadata Block we don't explicitly handle
 */
class FlacUnhandledMetadataBlock(type: Byte, private val data2: ByteArray) : FlacMetadataBlock(type) {
	protected override fun write(out: SyncOutputStream) {
		out.write(data2)
	}
}
