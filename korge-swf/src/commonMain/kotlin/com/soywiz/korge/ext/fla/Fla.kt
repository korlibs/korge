package com.soywiz.korge.ext.fla

import com.soywiz.korio.file.*
import com.soywiz.korio.file.std.*
import com.soywiz.korio.serialization.xml.*

object Fla {
	suspend fun read(file: VfsFile) {
		val zip = if (file.isDirectory()) file else file.openAsZip()
		val xml = zip["DOMDocument.xml"].readXml()
		for (x in xml.descendants.filter { it.nameLC == "layers" }) {
			println(x)
		}
		//println(xml)
	}

	fun parseTimeline(timelineXml: Xml) {

	}

	fun parseMotionObjectXml(moXml: Xml) {

	}
}

/*
<DOMDocument xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://ns.adobe.com/xfl/2008/" currentTimeline="1" xflVersion="2.95" creatorInfo="Adobe Animate CC" platform="Windows" versionInfo="Saved by Animate Windows 16.5 build 104" majorVersion="16" minorVersion="5" buildNumber="104" nextSceneIdentifier="2" playOptionsPlayLoop="false" playOptionsPlayPages="false" playOptionsPlayFrameActions="false" filetypeGUID="3CE50BB6-55CF-47A6-B591-01286DDDC64C" fileGUID="2F6481179F773E4A913C5ED26633EB76">
     <symbols>
          <Include href="Símbolo 1.xml" loadImmediate="false" itemID="59875270-0000001c" lastModified="1502040688"/>
     </symbols>
     <timelines>
          <DOMTimeline name="Escena 1">
               <layers>
                    <DOMLayer name="Capa 1" color="#4F80FF" current="true" isSelected="true" animationType="motion object">
                         <frames>
                              <DOMFrame index="0" duration="9" tweenType="motion object" motionTweenRotate="none" motionTweenScale="false" isMotionObject="true" visibleAnimationKeyframes="4194303" keyMode="8195">
                                   <motionObjectXML>
										<AnimationCore TimeScale="24000" Version="1" duration="9000">
											<TimeMap strength="0" type="Quadratic"/>
											<metadata>
												<names>
													<name langID="es_ES" value=""/>
												</names>
												<Settings orientToPath="0" xformPtXOffsetPct="0.5" xformPtYOffsetPct="0.5" xformPtZOffsetPixels="0"/>
											</metadata>
											<PropertyContainer id="headContainer">
												<PropertyContainer id="Basic_Motion">
													<Property enabled="1" id="Motion_X" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,0" next="0,0" previous="0,0" roving="0" timevalue="0"/>
														<Keyframe anchor="0,458.5" next="0,458.5" previous="0,458.5" roving="0" timevalue="8000"/>
													</Property>
													<Property enabled="1" id="Motion_Y" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,0" next="0,0" previous="0,0" roving="0" timevalue="0"/>
														<Keyframe anchor="0,309.45" next="0,309.45" previous="0,309.45" roving="0" timevalue="8000"/>
													</Property>
													<Property enabled="1" id="Rotation_Z" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,0" next="0,0" previous="0,0" roving="0" timevalue="0"/>
													</Property>
													<Property enabled="1" id="Depth" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,0" next="0,0" previous="0,0" roving="0" timevalue="0"/>
													</Property>
												</PropertyContainer>
												<PropertyContainer id="Transformation">
													<Property enabled="1" id="Skew_X" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,0" next="0,0" previous="0,0" roving="0" timevalue="0"/>
													</Property>
													<Property enabled="1" id="Skew_Y" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,0" next="0,0" previous="0,0" roving="0" timevalue="0"/>
													</Property>
													<Property enabled="1" id="Scale_X" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,100" next="0,100" previous="0,100" roving="0" timevalue="0"/>
													</Property>
													<Property enabled="1" id="Scale_Y" ignoreTimeMap="0" readonly="0" visible="1">
														<Keyframe anchor="0,100" next="0,100" previous="0,100" roving="0" timevalue="0"/>
													</Property>
												</PropertyContainer>
												<PropertyContainer id="Colors"/>
													<PropertyContainer id="Filters"/>
												</PropertyContainer>
											</AnimationCore>
										</motionObjectXML>
                                   <elements>
                                        <DOMSymbolInstance libraryItemName="Símbolo 1">
                                             <matrix>
                                                  <Matrix tx="1" ty="0.05"/>
                                             </matrix>
                                             <transformationPoint>
                                                  <Point x="45.25" y="45.25"/>
                                             </transformationPoint>
                                        </DOMSymbolInstance>
                                   </elements>
                              </DOMFrame>
                         </frames>
                    </DOMLayer>
               </layers>
          </DOMTimeline>
     </timelines>
     <scripts>
          <GlobalScripts language="Javascript"/>
     </scripts>
     <PrinterSettings/>
     <publishHistory/>
</DOMDocument>

<DOMSymbolItem xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xmlns="http://ns.adobe.com/xfl/2008/" name="Símbolo 1" itemID="59875270-0000001c" lastModified="1502040688">
  <timeline>
    <DOMTimeline name="Símbolo 1">
      <layers>
        <DOMLayer name="Capa 1" color="#4F80FF" current="true" isSelected="true">
          <frames>
            <DOMFrame index="0" keyMode="9728">
              <elements>
                <DOMShape>
                  <fills>
                    <FillStyle index="1">
                      <SolidColor color="#0066CC"/>
                    </FillStyle>
                  </fills>
                  <edges>
                    <Edge fillStyle1="1" edges="!0 0S2|1810 0!1810 0|1810 1810!1810 1810|0 1810!0 1810|0 0"/>
                  </edges>
                </DOMShape>
              </elements>
            </DOMFrame>
          </frames>
        </DOMLayer>
      </layers>
    </DOMTimeline>
  </timeline>
</DOMSymbolItem>
 */
