package {
	import flash.events.TextEvent;
	import flash.text.TextField;
	import flash.text.TextFieldType;
	import flash.text.TextFormat;

	import flash.display.Sprite;
	import flash.display.StageAlign;
	import flash.display.StageScaleMode;
	import flash.events.Event;
	import flash.events.FocusEvent;

	[SWF(frameRate=60, width=300, height=320, backgroundColor="#454545")]
	public class Main extends Sprite {
		private var propertiesTextField: TextField;

		public function Main() {
			initialize();
		}
		
		private function initialize():void {
			stage.scaleMode = StageScaleMode.NO_SCALE;
			stage.align = StageAlign.TOP_LEFT;

			var propertiesLabel: TextField = new TextField();
			propertiesLabel.x = 0;
			propertiesLabel.y = 0;
			propertiesLabel.textColor = 0xcbcbcb;
			propertiesLabel.defaultTextFormat = new TextFormat("Arial", 12);
			propertiesLabel.selectable = false;
			propertiesLabel.type = TextFieldType.DYNAMIC;
			propertiesLabel.text = 'Properties:';
			this.addChild(propertiesLabel);
			
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
			
			stage.addEventListener(Event.RESIZE, function(e:*):void {
				propertiesTextField.width = stage.stageWidth;
				propertiesTextField.height = stage.stageHeight - propertiesTextField.y;	
			});

			propertiesTextField.addEventListener(Event.CHANGE, function (e:*):void {
				Logger.log('CHANGE');
				if (selectedItem) {
					var props:* = Props.parse(propertiesTextField.text);
					setPropertiesToSelectedJson(JSON.stringify(props))
				}
			});

			setSelectedItem(false);

			JSFLExt.exec(
				<js><![CDATA[
					fl.addEventListener("selectionChanged", callback);
				]]></js>.toString(),
				{
					callback: function (args:Array):void {
						var json: String = getPropertiesFromSelectedItemJson();
						Logger.log('SELECTION_CHANGED: ' + json);
						setProperties((json != null) ? JSON.parse(json) : null);
					}
				}
			);
		}
		
		static private function getPropertiesFromSelectedItemJson(): * {
			return JSFLExt.exec(
				<js><![CDATA[
					var doc = fl.getDocumentDOM(); if (!doc) return null;
					var item = doc.selection[0]; if (!item) return null;
					if (doc.selection.length != 1) return null;

					return item.hasPersistentData('props') ? item.getPersistentData('props') : "{}";
				]]></js>.toString()
			);
		}
		
		static private function setPropertiesToSelectedJson(propsAsJson: String): void {
			var result:String = JSFLExt.exec(
				<js><![CDATA[
					var doc = fl.getDocumentDOM(); if (!doc) return;
					var item = doc.selection[0]; if (!item) return;
					if (doc.selection.length != 1) return null;

					item.setPersistentData('props', 'string', propsAsJson);
					item.setPublishPersistentData('props', "_EMBED_SWF_", true);
					doc.setPublishDocumentData("_EMBED_SWF_", true);

					return 'success';
				]]></js>.toString(),
				{ propsAsJson: propsAsJson }
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
		
		private function setProperties(props: *):void {
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

import adobe.utils.MMExecute;
import flash.utils.ByteArray;

import flash.external.ExternalInterface;
import flash.utils.Dictionary;
import flash.utils.setTimeout;

class JSFLExt {
	static private var initialized:Boolean = false;

	static public const panelName:String = 'KorgeEXT';

	static private function initOnce():void {
		if (initialized) return;
		initialized = true;
		if (ExternalInterface.available) ExternalInterface.addCallback("JSFLCallback", JSFLCallback);
		MMExecute("var panelName = " + JSON.stringify(panelName) + ";" + <js><![CDATA[
			JSON = {
				stringify: function(obj) {
					if (obj === null) return 'null';
					if (typeof obj === 'undefined') return 'undefined';
					if ((typeof obj === 'number') || (typeof obj === 'boolean')) return '' + obj;
					if (typeof obj === 'string') {
						var str = obj;
						return '"' + str.replace(/["\\\x00-\x1f\x7f-\x9f]/g, function(a) {
							switch (a) {
								case '\b': return '\\b';
								case '\t': return '\\t';
								case '\n': return '\\n';
								case '\f': return '\\f';
								case '\r': return '\\r';
								case '"': return '\\"';
								case '\\': return '\\\\';
							}
							var c = a.charCodeAt();
							return '\\u00' + Math.floor(c / 16).toString(16) + (c % 16).toString(16);
						}) + '"';
					}
					if (obj instanceof Array) {
						return '[' + obj.map(function(item) { return JSON.stringify(item); }).join(',') + ']';
					} else {
						var items = [];
						for (var key in obj) {
							if (!Object.prototype.hasOwnProperty.call(obj, key)) continue;
							items.push(JSON.stringify(key) + ':' + JSON.stringify(obj[key]));
						}
						return '{' + items.join(',') + '}';
					}
					return obj;
				},
				parse: function(str) {
					return eval('(' + str + ')');
				}
			};

			function stringtostr2(str) {
				var items = [];
				for (var n = 0; n < str.length; n++) items.push(str.charCodeAt(n));
				return items.join(',')
			}

			function executeCallback(id, args) {
				fl.getSwfPanel(panelName, false).call('JSFLCallback', stringtostr2(JSON.stringify({ id: id, args: args })));
			}
		]]></js>.toString());
	}

	static private var callbacks:Dictionary = new Dictionary();
	static private var callbacksId:int = 0;

	static public function exec(code:String, args:Object = null):* {
		if (!ExternalInterface.available) return undefined;

		initOnce();
		if (!args) args = {};
		var realCode:String = '';
		for (var key:String in args) {
			var item:* = args[key];
			if (item is Function) {
				var callbackId:String = 'callback_' + callbacksId++;
				callbacks[callbackId] = item;
				realCode += 'var ' + key + ' = function() { executeCallback(' + JSON.stringify(callbackId) + ', Array.prototype.slice.call(arguments, 0)); };';
			} else {
				realCode += 'var ' + key + ' = ' + JSON.stringify(item) + ';';
			}
		}
		realCode += code;
		var result:* = MMExecute('JSON.stringify((function() { ' + realCode + ' })())');
		try {
			return JSON.parse(result);
		} catch (e:*) {
			trace('ERROR: ' + e);
			return undefined;
		}
	}


	static public function log(text:String):void {
		exec('fl.trace(value);', { value: text });
	}

	static public function JSFLCallback(infos2:String):void {
		try {
			var infos:String = infos2.split(',').map(function (s:String, ...args):String { return String.fromCharCode(parseInt(s)); }).join('');
			var info:* = JSON.parse(infos);
			var func:Function = (callbacks[info.id] as Function)
			setTimeout(function ():void { func(info.args); }, 0);
		} catch (e:*) {
			log('ERROR:' + e);
		}
	}
}

class Logger {
	static public function log(msg: String):void {
		//trace(msg);
		//JSFLExt.log(msg);
	}
}

class Props {
	static private function getKeys(v:*):Array {
		var out: Array = [];
		for (var key: String in v) out.push(key);
		return out;
	}
	
	static public function stringify(obj: *): String {
		var lines: Array = [];
		for each (var key: String in getKeys(obj).sort()) {
			var value: * = obj[key];
			if (value == "") {
				lines.push(key);
			} else {
				lines.push(key + " = " + value);
			}
		}
		return lines.join("\n");
	}
	
	static private function trim(str: String): String {
		return str.replace(/\s+$/, '').replace(/^\s+/, '');
	}
	
	static public function parse(str: String): * {
		var out:* = {};
		var lines: Array = str.split(/[\r\n]+/)
		for each (var line: String in lines) {
			for each (var part: String in line.split(',')) {
				var match: Array = part.match(/^(.*?)([:=](.*?))?$/);
				var key: String = trim(match[1]);
				var value: String = trim((match[3] != null) ? match[3] : "");
				if (key.length > 0) {
					out[key] = value;
				}
			}
		}
		return out;
	}
}
