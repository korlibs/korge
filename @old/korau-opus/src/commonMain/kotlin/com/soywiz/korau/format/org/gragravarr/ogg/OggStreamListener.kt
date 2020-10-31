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
package com.soywiz.korau.format.org.gragravarr.ogg

/**
 * Implement this if doing Event based processing
 * to know when new streams are found.
 */
interface OggStreamListener {
	/**
	 * Called every time a new Stream is encountered.
	 * Should return a (possibly empty) array of
	 * [OggStreamReader] instances which should
	 * be passed all packets for the stream;
	 */
	fun processNewStream(sid: Int, magicData: ByteArray): Array<OggStreamReader>

	/**
	 * Called after the last packet in Stream has been
	 * processed, in case you want to do any tidying up.
	 */
	fun processStreamEnd(sid: Int)
}
