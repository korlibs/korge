package com.soywiz.kmem.dyn

import kotlin.native.internal.*
import kotlin.native.internal.kmem.*

@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <R> KPointerTT<KFunctionTT<() -> R>>.invoke(): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, R> KPointerTT<KFunctionTT<(P1) -> R>>.invoke(p1: P1): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, P2, R> KPointerTT<KFunctionTT<(P1, P2) -> R>>.invoke(p1: P1, p2: P2): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, P2, P3, R> KPointerTT<KFunctionTT<(P1, P2, P3) -> R>>.invoke(p1: P1, p2: P2, p3: P3): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, P2, P3, P4, R> KPointerTT<KFunctionTT<(P1, P2, P3, P4) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, P2, P3, P4, P5, R> KPointerTT<KFunctionTT<(P1, P2, P3, P4, P5) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, P2, P3, P4, P5, P6, R> KPointerTT<KFunctionTT<(P1, P2, P3, P4, P5, P6) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6): R
@TypedIntrinsic(IntrinsicType.INTEROP_FUNPTR_INVOKE) actual external operator fun <P1, P2, P3, P4, P5, P6, P7, R> KPointerTT<KFunctionTT<(P1, P2, P3, P4, P5, P6, P7) -> R>>.invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7): R
