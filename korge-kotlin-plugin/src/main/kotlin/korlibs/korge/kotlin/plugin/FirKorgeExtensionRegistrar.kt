@file:OptIn(ExperimentalCompilerApi::class)

package korlibs.korge.kotlin.plugin

import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.cli.common.*
import org.jetbrains.kotlin.cli.common.messages.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.extensions.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.fir.extensions.predicate.*
import org.jetbrains.kotlin.fir.extensions.utils.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.platform.konan.*
import org.jetbrains.kotlin.psi.*

class KorgeCompilerPluginRegistrar : CompilerPluginRegistrar() {
    override val supportsK2: Boolean get() = true

    override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
        //val annotations = configuration.get(ANNOTATION)?.toMutableList() ?: mutableListOf()
        //configuration.get(PRESET)?.forEach { preset ->
        //    SUPPORTED_PRESETS[preset]?.let { annotations += it }
        //}
        //if (annotations.isEmpty()) return

        //DeclarationAttributeAltererExtension.registerExtension(CliAllOpenDeclarationAttributeAltererExtension(annotations))
        //FirExtensionRegistrarAdapter.registerExtension(FirKorgeExtensionRegistrar())

        val logger = configuration.get(CLIConfigurationKeys.MESSAGE_COLLECTOR_KEY, MessageCollector.NONE)
        //val versionSpecificApi = VersionSpecificApi.loadService()
        IrGenerationExtension.registerExtension(KorgeIrExtension(logger))
    }
}

//class CliAllOpenDeclarationAttributeAltererExtension(
//    private val allOpenAnnotationFqNames: List<String>
//) : AbstractAllOpenDeclarationAttributeAltererExtension() {
//    override fun getAnnotationFqNames(modifierListOwner: KtModifierListOwner?) = allOpenAnnotationFqNames
//}

class FirKorgeExtensionRegistrar : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        //+::FirKorgeStatusTransformer
        //+::KorgeIrExtension
        //+FirKorgePredicateMatcher.getFactory(listOf("korlibs.korge.KeepOnReload"))
    }
}

class KorgeIrContext(val pluginContext: IrPluginContext)

internal class KorgeIrExtension(
    private val logger: MessageCollector,
    //private val versionSpecificApi: VersionSpecificApi,
) : IrGenerationExtension {
    override fun generate(
        moduleFragment: IrModuleFragment,
        pluginContext: IrPluginContext,
    ) {
        val context = KorgeIrContext(pluginContext)
        moduleFragment.transform(KorgeIrServiceProcessor(logger), context)
    }
}

internal class KorgeIrServiceProcessor(
    @Suppress("unused")
    private val logger: MessageCollector,
) : IrElementTransformer<KorgeIrContext> {
    //override fun visitClass(declaration: IrClass, data: KorgeIrContext): IrStatement {
    //    IrConstructorCallImpl(
    //        startOffset = declaration.startOffset,
    //        endOffset = declaration.endOffset,
    //        type = data.optInAnnotation.typeWith(),
    //        symbol = data.optInAnnotation.constructors.single(),
    //        typeArgumentsCount = 0,
    //        constructorTypeArgumentsCount = 0,
    //        valueArgumentsCount = 1,
    //    ).putValueArgument()
    //    return super.visitClass(declaration, data)
    //}

    override fun visitConst(expression: IrConst<*>, data: KorgeIrContext): IrExpression {
        if (expression.kind == IrConstKind.String && expression.value == "fir-korge-extension-test") {
            logger.report(CompilerMessageSeverity.STRONG_WARNING, "Detected fir-korge-extension-test!!")
            return IrConstImpl(0, 0, expression.type, IrConstKind.String, "MODIFIED BY KORGE EXTENSION")
        }
        return expression
    }

    //override fun visitStringConcatenation(expression: IrStringConcatenation, data: KorgeIrContext): IrExpression {
    //    expression.arguments
    //    return super.visitStringConcatenation(expression, data)
    //}
}

/*
class FirKorgeStatusTransformer(session: FirSession) : FirStatusTransformerExtension(session) {
    override fun needTransformStatus(declaration: FirDeclaration): Boolean {
        if (declaration.isJavaOrEnhancement) return false
        return when (declaration) {
            is FirRegularClass -> declaration.classKind == ClassKind.CLASS && session.allOpenPredicateMatcher.isAnnotated(declaration.symbol)
            is FirCallableDeclaration -> {
                val parentClassId = declaration.symbol.callableId.classId ?: return false
                if (parentClassId.isLocal) return false
                val parentClassSymbol = session.symbolProvider.getClassLikeSymbolByClassId(parentClassId) as? FirRegularClassSymbol
                    ?: return false
                parentClassSymbol.classKind == ClassKind.CLASS && session.allOpenPredicateMatcher.isAnnotated(parentClassSymbol)
            }
            else -> false
        }
    }

    override fun transformStatus(status: FirDeclarationStatus, declaration: FirDeclaration): FirDeclarationStatus {
        return when (status.modality) {
            null -> status.copyWithNewDefaults(modality = Modality.OPEN, defaultModality = Modality.OPEN)
            else -> status.copyWithNewDefaults(defaultModality = Modality.OPEN)
        }
    }
}

class FirKorgePredicateMatcher(
    session: FirSession,
    allOpenAnnotationFqNames: List<String>
) : AbstractSimpleClassPredicateMatchingService(session) {
    companion object {
        fun getFactory(allOpenAnnotationFqNames: List<String>): Factory {
            return Factory { session -> FirKorgePredicateMatcher(session, allOpenAnnotationFqNames) }
        }
    }

    override val predicate = DeclarationPredicate.create {
        val annotationFqNames = allOpenAnnotationFqNames.map { FqName(it) }
        annotated(annotationFqNames) or metaAnnotated(annotationFqNames, includeItself = true)
    }
}

val FirSession.allOpenPredicateMatcher: FirKorgePredicateMatcher by FirSession.sessionComponentAccessor()
*/
//open class KorgeGradleExtension(objects: ObjectFactory) {
//    val stringProperty: Property<String> = objects.property(String::class.java)
//    val fileProperty: RegularFileProperty = objects.fileProperty()
//}
/*
class FirAllOpenExtensionRegistrar(val allOpenAnnotationFqNames: List<String>) : FirExtensionRegistrar() {
    override fun ExtensionRegistrarContext.configurePlugin() {
        +::FirAllOpenStatusTransformer
        +FirAllOpenPredicateMatcher.getFactory(allOpenAnnotationFqNames)
    }
}
 */
