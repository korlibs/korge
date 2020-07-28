package com.soywiz.korge.intellij.module.wizard

import com.intellij.ide.util.projectWizard.*
import com.intellij.openapi.ui.*
import com.intellij.ui.*
import com.intellij.util.ui.*
import com.soywiz.korge.intellij.*
import com.soywiz.korge.intellij.module.*
import com.soywiz.korge.intellij.util.*
import java.awt.*
import java.net.*
import javax.swing.*
import javax.swing.tree.*

typealias Feature = KorgeProjectTemplate.Features.Feature
typealias FeatureSet = KorgeProjectTemplate.Features.FeatureSet

class KorgeModuleWizardStep(val korgeProjectTemplateProvider: KorgeProjectTemplate.Provider, val config: KorgeModuleConfig) : ModuleWizardStep() {
	override fun updateDataModel() {
		config.projectType = projectTypeCB.selectedItem as ProjectType
		config.featuresToInstall = featuresToCheckbox.keys.filter { it.selected }
		config.korgeVersion = versionCB.selected
		println("KorgeModuleWizardStep.updateDataModel: projectType:${config.projectType}, korgeVersion:${config.korgeVersion}, featuresToInstall:${config.featuresToInstall.size}")
	}

	lateinit var projectTypeCB: JComboBox<ProjectType>

	lateinit var versionCB: JComboBox<KorgeProjectTemplate.Versions.Version>

	lateinit var refreshButton: JButton

	lateinit var wrapperCheckBox: JCheckBox

	lateinit var featureList: FeatureCheckboxList

	init {
		println("Created KorgeModuleWizardStep")
	}

	val panel by lazy {
		println("Created KorgeModuleWizardStep.panel")
		JPanel().apply {
			val description = JPanel().apply {
				layout = BoxLayout(this, BoxLayout.Y_AXIS)
				border = IdeBorderFactory.createBorder()
			}

			fun showFeatureDocumentation(feature: Feature?) {
				description.removeAll()
				if (feature != null) {
					description.add(JLabel(feature.description, SwingConstants.LEFT))
					//for (artifact in feature.artifacts) description.add(JLabel(artifact))
					val doc = feature.documentation
					description.add(Link(doc, URL(doc)))
				}
				description.doLayout()
				description.repaint()
			}

			featureList = object : FeatureCheckboxList(listOf()) {
				override fun onSelected(feature: Feature?, node: ThreeStateCheckedTreeNode) = showFeatureDocumentation(feature)
				override fun onChanged(feature: Feature, node: ThreeStateCheckedTreeNode) = updateTransitive()
			}.also {
				it.isEnabled = false
			}

			this.layout = BorderLayout(0, 0)

			add(table {
				tr(
					policy = TdSize.FIXED,
					fill = TdFill.NONE,
					align = TdAlign.CENTER_LEFT
				) {
					td(JLabel("Project:"))
					projectTypeCB = td(JComboBox(ProjectType.values()))
					wrapperCheckBox = td(JCheckBox("Wrapper", true))
					td(JLabel("Korge Version:"))
					versionCB = td(JComboBox<KorgeProjectTemplate.Versions.Version>(arrayOf()))
					refreshButton = td(JButton("Refresh").onClick {
						refresh(invalidate = true)
					})
				}
			}, BorderLayout.NORTH)

			add(Splitter(true, 0.8f, 0.2f, 0.8f).apply {
				this.firstComponent = table {
					tr(
						policy = TdSize.FIXED,
						minHeight = 24,
						maxHeight = 24,
						fill = TdFill.NONE,
						align = TdAlign.CENTER_LEFT
					) {
						td(JLabel("Features:"))
					}
					tr {
						td(featureList.scrollVertical())
					}
				}
				this.secondComponent = description
			}, BorderLayout.CENTER)
		}.also {
			refresh()
		}
	}

	fun refresh(invalidate: Boolean = false) {
		fun toggleEnabled(enabled: Boolean) {
			projectTypeCB.isEnabled = enabled
			wrapperCheckBox.isEnabled = enabled
			versionCB.isEnabled = enabled
			featureList.isEnabled = enabled
			refreshButton.isEnabled = enabled
			for (checkbox in featureList.featuresToCheckbox.values) {
				checkbox.isEnabled = enabled
			}
		}

		toggleEnabled(false)

		runBackgroundTaskGlobal {
			// Required since this is blocking
			if (invalidate) {
				korgeProjectTemplateProvider.invalidate()
			}
			val korgeProjectTemplate = korgeProjectTemplateProvider.template

			runInUiThread {
				versionCB.model = DefaultComboBoxModel(korgeProjectTemplate.versions.versions.toTypedArray())
				featureList.features = korgeProjectTemplateProvider.template.features.features.toList()
				toggleEnabled(true)
			}
		}
	}

	val featuresToCheckbox get() = featureList.featuresToCheckbox


		var Feature.selected: Boolean
		get() = featuresToCheckbox[this]?.isChecked ?: false
		set(value) = run { featuresToCheckbox[this]?.isChecked = value }

	var Feature.indeterminate: Boolean
		get() = featuresToCheckbox[this]?.indeterminate ?: false
		set(value) = run { featuresToCheckbox[this]?.indeterminate = value }

	//var Feature.indeterminate : Boolean
	//    get() = featuresToCheckbox[this]?. ?: false
	//    set(value) {
	//        featuresToCheckbox[this]?.isSelected = value
	//    }

	fun updateTransitive() {
		val features = korgeProjectTemplateProvider.template.features
		val allFeatures = features.features.toList()
		val featureSet = FeatureSet(allFeatures.filter { it.selected }, features.allFeatures)

		for (feature in allFeatures) {
			feature.indeterminate = (feature in featureSet.transitive)
		}

		featureList.repaint()
	}

	override fun getComponent() = panel
}

open class ThreeStateCheckedTreeNode : CheckedTreeNode {
	constructor() : super()
	constructor(userObject: Any?) : super(userObject)

	var indeterminate = false
}

abstract class FeatureCheckboxList(private val initialFeatures: List<Feature>) : CheckboxTree(
	object : CheckboxTree.CheckboxTreeCellRenderer() {
		override fun customizeRenderer(
			tree: JTree?,
			value: Any?,
			selected: Boolean,
			expanded: Boolean,
			leaf: Boolean,
			row: Int,
			hasFocus: Boolean
		) {
			if (value is ThreeStateCheckedTreeNode) {
				val feature = value.userObject
				val tscheckbox = checkbox as ThreeStateCheckBox
				if (feature is Feature) {
					val style: SimpleTextAttributes = when {
						value.indeterminate -> SimpleTextAttributes.REGULAR_ITALIC_ATTRIBUTES
						else -> SimpleTextAttributes.REGULAR_ATTRIBUTES
					}
					textRenderer.append(feature.name, style)
					textRenderer.isEnabled = true
					tscheckbox.isVisible = true
					tscheckbox.state = when {
						value.indeterminate -> ThreeStateCheckBox.State.DONT_CARE
						value.isChecked -> ThreeStateCheckBox.State.SELECTED
						else -> ThreeStateCheckBox.State.NOT_SELECTED
					}
					textRenderer.foreground = UIUtil.getTreeForeground()
				} else if (feature is String) {
					textRenderer.append(feature)
					textRenderer.isEnabled = false
					isEnabled = false
					tscheckbox.isVisible = false
				}
			}
		}
	},
	ThreeStateCheckedTreeNode()
) {
	val CheckedTreeNode?.feature: Feature? get() = this?.userObject as? Feature?

	val featuresToCheckbox = LinkedHashMap<Feature, ThreeStateCheckedTreeNode>()
	val root = (this.model as DefaultTreeModel).root as ThreeStateCheckedTreeNode

	init {
		this.model = object : DefaultTreeModel(root) {
			override fun valueForPathChanged(path: TreePath, newValue: Any) {
				super.valueForPathChanged(path, newValue)
				val node = path.lastPathComponent as ThreeStateCheckedTreeNode
				val feature = node.feature
				if (feature != null) {
					onChanged(feature, node)
				}
			}
		}
	}

	private  var _features: List<Feature> = initialFeatures
	var features: List<Feature>
		get() = _features
		set(value) {
			_features = value
			root.removeAllChildren()
			featuresToCheckbox.clear()
			for ((group, gfeatures) in _features.groupBy { it.group }) {
				root.add(ThreeStateCheckedTreeNode(group).apply { isChecked = false })
				for (feature in gfeatures) {
					root.add(ThreeStateCheckedTreeNode(feature).apply { isChecked = false; featuresToCheckbox[feature] = this })
				}
			}
			(this.model as DefaultTreeModel).reload(root)
		}



	init {
		features = initialFeatures

		addTreeSelectionListener { e ->
			val node = (e.newLeadSelectionPath.lastPathComponent as? ThreeStateCheckedTreeNode)
			val feature = node?.userObject as? Feature?

			if (node != null) {
				onSelected(feature, node)
			}
		}
	}

	abstract fun onSelected(feature: Feature?, node: ThreeStateCheckedTreeNode)
	open fun onChanged(feature: Feature, node: ThreeStateCheckedTreeNode) {
	}
}
