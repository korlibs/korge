package {
import flash.events.MouseEvent;
import flash.events.TextEvent;
import flash.net.URLRequest;
import flash.text.TextField;
import flash.text.TextFieldType;
import flash.text.TextFormat;
import flash.text.TextFieldAutoSize;

import flash.display.Sprite;
import flash.display.StageAlign;
import flash.display.StageScaleMode;
import flash.events.Event;
import flash.events.FocusEvent;

import adobe.utils.MMExecute;

import flash.external.ExternalInterface;

import flash.net.navigateToURL;

[SWF(frameRate=60, width=300, height=320, backgroundColor="#454545")]
public class Main extends Sprite {
	private var propertiesTextField:TextField;
	private var enabled:Boolean = true;

	public function Main() {
		initialize();
	}

	private function initialize():void {
		stage.scaleMode = StageScaleMode.NO_SCALE;
		stage.align = StageAlign.TOP_LEFT;

		var propertiesLabel:TextField = new TextField();
		propertiesLabel.x = 0;
		propertiesLabel.y = 0;
		propertiesLabel.textColor = 0xcbcbcb;
		propertiesLabel.defaultTextFormat = new TextFormat("Arial", 12);
		propertiesLabel.selectable = false;
		propertiesLabel.type = TextFieldType.DYNAMIC;
		propertiesLabel.autoSize = TextFieldAutoSize.LEFT;
		propertiesLabel.text = 'Properties:';
		this.addChild(propertiesLabel);

		var enableLabel:TextField = new TextField();
		enableLabel.x = 80;
		enableLabel.y = 0;
		enableLabel.defaultTextFormat = new TextFormat("Arial", 12);
		enableLabel.selectable = false;
		enableLabel.type = TextFieldType.DYNAMIC;
		enableLabel.autoSize = TextFieldAutoSize.LEFT;
		enableLabel.textColor = 0xcbcbcb;
		enableLabel.text = '[ENABLED]';
		this.addChild(enableLabel);

		var helpLabel:TextField = new TextField();
		helpLabel.x = 160;
		helpLabel.y = 0;
		helpLabel.defaultTextFormat = new TextFormat("Arial", 12);
		helpLabel.selectable = false;
		helpLabel.type = TextFieldType.DYNAMIC;
		helpLabel.autoSize = TextFieldAutoSize.LEFT;
		helpLabel.textColor = 0xcbcbcb;
		helpLabel.text = '[HELP]';
		this.addChild(helpLabel);

		var versionLabel:TextField = new TextField();
		versionLabel.x = 220;
		versionLabel.y = 0;
		versionLabel.defaultTextFormat = new TextFormat("Arial", 12);
		versionLabel.selectable = false;
		versionLabel.type = TextFieldType.DYNAMIC;
		versionLabel.autoSize = TextFieldAutoSize.LEFT;
		versionLabel.textColor = 0xcbcbcb;
		versionLabel.text = 'v0.9.0';
		this.addChild(versionLabel);

		propertiesTextField = new TextField();
		propertiesTextField.x = 0;
		propertiesTextField.y = 20;
		propertiesTextField.width = 300;
		propertiesTextField.height = 300;
		propertiesTextField.backgroundColor = 0xebebeb;
		propertiesTextField.defaultTextFormat = new TextFormat("Lucida Console", 12);
		propertiesTextField.type = TextFieldType.INPUT;
		propertiesTextField.backgroundColor = 0xebebeb;
		propertiesTextField.background = true;
		propertiesTextField.multiline = true;
		propertiesTextField.wordWrap = false;
		this.addChild(propertiesTextField);

		helpLabel.addEventListener(MouseEvent.CLICK, function (e:MouseEvent):void {
			navigateToURL(new URLRequest('http://docs.korge.soywiz.com/animation/swf'));
		});

		enableLabel.addEventListener(MouseEvent.CLICK, function (e:MouseEvent):void {
			enabled = !enabled;
			enableLabel.textColor = enabled ? 0xcbcbcb : 0x777777;
			propertiesLabel.textColor = enabled ? 0xcbcbcb : 0x777777;
			helpLabel.textColor = enabled ? 0xcbcbcb : 0x777777;
			versionLabel.textColor = enabled ? 0xcbcbcb : 0x777777;
			propertiesTextField.backgroundColor = enabled ? 0xebebeb : 0x777777;
			enableLabel.text = enabled ? '[ENABLED]' : '[DISABLED]';
			selectionChanged();
		});

		stage.addEventListener(Event.RESIZE, function (e:*):void {
			propertiesTextField.width = stage.stageWidth;
			propertiesTextField.height = stage.stageHeight - propertiesTextField.y;
		});

		propertiesTextField.addEventListener(Event.CHANGE, function (e:*):void {
			if (selectedItem) {
				var props:* = Props.parse(propertiesTextField.text);
				setPropertiesToSelectedJson(JSON.stringify(props))
			}
		});

		setSelectedItem(false);

		ExternalInterface.addCallback("selectionChanged", selectionChanged);
		MMExecute(
				"fl.addEventListener('selectionChanged', function() {" +
				"	fl.getSwfPanel('KorgeEXT', false).call('selectionChanged');" +
				"});"
		);
	}

	public function selectionChanged():void {
		var json:String = enabled ? getSelection() : null;
		setProperties((json != null) ? JSON.parse(json.split(/,/).map(function (e:String, index:int, arr:Array):String {
			return String.fromCharCode(parseInt(e));
		}).join('')) : null);

	}

	public function getSelection():String {
		return MMExecute(
				"(function() {" +
				"  var doc = fl.getDocumentDOM();" +
				"  var item = (doc && (doc.selection.length == 1)) ? doc.selection[0] : null;" +
				"  var data = item ? (item.hasPersistentData('props') ? item.getPersistentData('props') : '{}') : null;" +
				"  var data2 = data ? (data.split('').map(function(s) { return s.charCodeAt(0); }).join(',')) : null;" +
				"  return data2;" +
				"})()"
		);
	}

	private function setPropertiesToSelectedJson(propsAsJson:String):void {
		MMExecute(
				"(function() {" +
				"	var doc = fl.getDocumentDOM(); if (!doc) return;" +
				"	var item = doc.selection[0]; if (!item) return;" +
				"	if (doc.selection.length != 1) return null;" +
				"	item.setPersistentData('props', 'string', " + JSON.stringify(propsAsJson) + ");" +
				"	item.setPublishPersistentData('props', '_EMBED_SWF_', true);" +
				"	doc.setPublishDocumentData('_EMBED_SWF_', true);" +
				"	return 'success';" +
				"})()"
		);
	}

	private var selectedItem:Boolean = false;

	private function setSelectedItem(value:Boolean):void {
		this.selectedItem = value;
		propertiesTextField.mouseEnabled = value;
		if (value) {
			propertiesTextField.backgroundColor = 0xFFFFFFFF;
		} else {
			propertiesTextField.backgroundColor = 0xebebeb;
		}

	}

	private function setProperties(props:*):void {
		if (props) {
			propertiesTextField.text = Props.stringify(props);
			setSelectedItem(true);
		} else {
			propertiesTextField.text = '';
			setSelectedItem(false);
		}
	}
}
}

class Props {
	static private function getKeys(v:*):Array {
		var out:Array = [];
		for (var key:String in v) out.push(key);
		return out;
	}

	static public function stringify(obj:*):String {
		var lines:Array = [];
		for each (var key:String in getKeys(obj).sort()) {
			var value:* = obj[key];
			if (value == "") {
				lines.push(key);
			} else {
				lines.push(key + " = " + value);
			}
		}
		return lines.join("\n");
	}

	static private function trim(str:String):String {
		return str.replace(/\s+$/, '').replace(/^\s+/, '');
	}

	static public function parse(str:String):* {
		var out:* = {};
		var lines:Array = str.split(/[\r\n]+/)
		for each (var line:String in lines) {
			for each (var part:String in line.split(',')) {
				var match:Array = part.match(/^(.*?)([:=](.*?))?$/);
				var key:String = trim(match[1]);
				var value:String = trim((match[3] != null) ? match[3] : "");
				if (key.length > 0) {
					out[key] = value;
				}
			}
		}
		return out;
	}
}
