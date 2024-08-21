/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2024 Evan Debenham
 *
 * Sandbox Pixel Dungeon
 * Copyright (C) 2023-2024 AlphaDraxonis
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package com.shatteredpixel.shatteredpixeldungeon.editor.lua.luaeditor;

import com.badlogic.gdx.files.FileHandle;
import com.shatteredpixel.shatteredpixeldungeon.Chrome;
import com.shatteredpixel.shatteredpixeldungeon.GameObject;
import com.shatteredpixel.shatteredpixeldungeon.editor.EditorScene;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.WndEditorInv;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.categories.*;
import com.shatteredpixel.shatteredpixeldungeon.editor.inv.items.EditorItem;
import com.shatteredpixel.shatteredpixeldungeon.editor.lua.DungeonScript;
import com.shatteredpixel.shatteredpixeldungeon.editor.lua.LuaManager;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.PopupMenu;
import com.shatteredpixel.shatteredpixeldungeon.editor.ui.SimpleWindow;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.CustomDungeonSaves;
import com.shatteredpixel.shatteredpixeldungeon.editor.util.EditorUtilities;
import com.shatteredpixel.shatteredpixeldungeon.items.Item;
import com.shatteredpixel.shatteredpixeldungeon.items.bags.Bag;
import com.shatteredpixel.shatteredpixeldungeon.messages.Messages;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.DungeonScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene;
import com.shatteredpixel.shatteredpixeldungeon.ui.*;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.CustomObject;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.LuaCustomObject;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.ResourcePath;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.interfaces.CustomObjectClass;
import com.shatteredpixel.shatteredpixeldungeon.usercontent.ui.CustomObjSelector;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndBag;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndError;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndOptions;
import com.shatteredpixel.shatteredpixeldungeon.windows.WndTitledMessage;
import com.watabou.NotAllowedInLua;
import com.watabou.idewindowactions.CodeInputPanelInterface;
import com.watabou.idewindowactions.LuaScript;
import com.watabou.input.PointerEvent;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextInput;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.BiConsumer;
import com.watabou.utils.Consumer;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@NotAllowedInLua
public class IDEWindow extends Component {

	private String scriptPath;//initial value
	private final LuaCustomObject customObject;

	private CodeInputPanel[] codeInputPanels;
	private CodeInputPanel inputDesc, inputLocalVars, inputScriptVars;
	private AdditionalCodePanel additionalCode;
	private RenderedTextBlock pathLabel;
	private TextInput pathInput;
	private IconButton changeScript;

	private Component outsideSp;

	private final Class<?> clazz;

	private boolean unsavedChanges = false;

	public IDEWindow(LuaCustomObject customObject, Class<?> clazz, Runnable layoutParent) {
		this.customObject = customObject;
		this.scriptPath = customObject.getLuaScriptPath();
		this.clazz = clazz;

		pathLabel = PixelScene.renderTextBlock(Messages.get(IDEWindow.class, "path"),9);
		add(pathLabel);

		//tzz Anzeige des Wertes 'scriptPath'; Button zur Erstellung einer neuen THEORETISCHEN Instanz von scriptPath, erst beim SPEICHERN wird dieser auch eine ID zugewiesen
		pathInput = new TextInput(Chrome.get(Chrome.Type.TOAST_WHITE), false, PixelScene.uiCamera.zoom) {
			@Override
			protected void looseFocus() {
				super.looseFocus();
				String text = getText();
				if (text != null && !text.isEmpty() && !text.endsWith(".lua")) {
					setText(text + ".lua");
				}
			}

			@Override
			public void setText(String text) {
				super.setText(text);
				unsavedChanges = true;
			}
		};
		pathInput.setTextFieldFilter((textField, c) -> TextInput.FILE_NAME_INPUT.acceptChar(textField, c) || c == '/' || c == '\\');
		add(pathInput);

		changeScript = new IconButton(Icons.CHANGES.get()) {
			@Override
			protected void onClick() {
				//Show loaded LuaScript value of all existing and applicable RawLuaScripts tzz
				showSelectScriptWindow(clazz, script -> {
					if (script != null) {
						selectScript(script.getPath(), script, true);
						unsavedChanges = true;
					}
				});
			}
		};
		add(changeScript);

		outsideSp = new OutsideSp();

		int i = 0;

		List<LuaMethodManager> methods = LuaMethodManager.getAllMethodsInOrder(clazz);
		codeInputPanels = new CodeInputPanel[methods.size() + 4];

		codeInputPanels[i++] = inputDesc = new CodeInputPanel() {
			{
				title.text(Messages.get(IDEWindow.class, "desc_title"));
			}

			@Override
			protected String createDescription() {
				return Messages.get(IDEWindow.class, "desc_info");
			}

			@Override
			public String convertToLuaCode() {
				String comment = textInput == null ? textInputText == null ? "" : textInputText : textInput.getText();
				return "--" + comment.replace('\n', (char) 29);
			}

			@Override
			public void applyScript(boolean forceChange, LuaScript fullScript, String cleanedCode) {
				if (forceChange || textInput == null || textInput.getText().isEmpty()) {
					if (textInput == null) onAddClick();
					textInput.setText(fullScript.desc);
				}
			}

			@Override
			protected void onTextChange() {
				unsavedChanges = true;
			}

			@Override
			protected void layoutParent() {
				layoutParent.run();
			}

			@Override
			public void expand() {
				onAdd(null, true);
				remover.setVisible(false);

				layoutParent();
			}

			@Override
			public void fold() {
				textInputText = textInput.getText();

				if (body != null) {
					body.destroy();
					remove(body);
					body = null;
				}
				textInput = null;

				adder.setVisible(false);
				remover.setVisible(false);

				fold.setVisible(false);
				expand.setVisible(true);

				layoutParent();
			}
		};
		add(inputDesc);

		codeInputPanels[i++] = inputLocalVars = new VariablesPanel(Messages.get(IDEWindow.class, (clazz == DungeonScript.class ? "global_vars" : "vars") + "_title"), "vars") {
			@Override
			protected void layoutParent() {
				layoutParent.run();
			}

			@Override
			protected String createDescription() {
				return Messages.get(IDEWindow.class, (clazz == DungeonScript.class ? "global_vars" : "vars") + "_info");
			}

			@Override
			protected void onTextChange() {
				unsavedChanges = true;
			}
		};
		add(inputLocalVars);

		if (clazz != DungeonScript.class) {
			codeInputPanels[i++] = inputScriptVars = new VariablesPanel(Messages.get(IDEWindow.class, "static_title"), "static") {
				@Override
				protected void layoutParent() {
					layoutParent.run();
				}

				@Override
				protected String createDescription() {
					return Messages.get(IDEWindow.class, "static_info");
				}

				@Override
				protected void onTextChange() {
					unsavedChanges = true;
				}
			};
			add(inputScriptVars);
		} else {
			codeInputPanels[i++] = inputScriptVars = null;
		}

		codeInputPanels[codeInputPanels.length-1] = additionalCode = new AdditionalCodePanel() {
			@Override
			protected void layoutParent() {
				layoutParent.run();
			}

			@Override
			protected void onTextChange() {
				unsavedChanges = true;
			}
		};
		add(additionalCode);



		for (LuaMethodManager methodInfo : methods) {
			codeInputPanels[i] = new MethodPanel(methodInfo.method, methodInfo.paramNames) {
				@Override
				protected void layoutParent() {
					layoutParent.run();
				}

				@Override
				protected void onTextChange() {
					unsavedChanges = true;
				}
			};
			add(codeInputPanels[i]);
			i++;
		}

		selectScript(scriptPath, scriptPath == null ? null : CustomObject.loadScriptFromFile(scriptPath), true);
		inputDesc.textInput.gainFocus();

		unsavedChanges = false;
	}

	@Override
	protected void layout() {

		float h = 18;
		changeScript.setRect(x + width - h, y, h, h);
		pathLabel.maxWidth((int) ((changeScript.left() - 1)*2/3f));
		float w = (changeScript.left() - x - pathLabel.width() - 3 - 2);
		pathInput.setRect(x + changeScript.left() - 2 - w, y, w, h);

		pathLabel.maxWidth((int) (pathInput.left() - 2));
		pathLabel.setPos(x, y + (h - pathLabel.height()) * 0.5f);

		height = Math.max(h, pathLabel.height()) + 3;
		height = EditorUtilities.layoutCompsLinear(2, this, codeInputPanels);
	}

	public Component getOutsideSp() {
		return outsideSp;
	}

	private String createFullScript() {
		StringBuilder b = new StringBuilder();

		LuaScript script = new LuaScript(clazz, null);

		script.desc = inputDesc.convertToLuaCode().substring(2);//uncomment, removes --
		b.append('\n');

		StringBuilder functions = new StringBuilder();

		for (int i = 1; i < codeInputPanels.length; i++) {
			if (codeInputPanels[i] == null) continue;
			String code = codeInputPanels[i].convertToLuaCode();
			if (code != null) {
				functions.append(code);
				functions.append("\n\n");
			}
		}

		String fullFunctions = functions.toString();
		b.append(fullFunctions);

		String cleanedFunctions = LuaScript.cleanLuaCode(fullFunctions);

		b.append(LuaScript.SCRIPT_RETURN_START);
		for (String functionName : LuaScript.allFunctionNames(cleanedFunctions)) {
			b.append(functionName).append(" = ").append(functionName).append("; ");
		}
		b.append("\n}");

		script.code  = b.toString();

		return script.getAsFileContent();
	}

	private void compile() {
		String result = CodeInputPanelInterface.compileResult(codeInputPanels);
		if (result != null) {
			EditorScene.show(new WndError(result));
		} else {
			RenderedTextBlock title = PixelScene.renderTextBlock(Messages.get(IDEWindow.class, "compile_no_error_title"), 9);
			title.hardlight(Window.TITLE_COLOR);
			EditorScene.show(new WndTitledMessage(title, Messages.get(IDEWindow.class, "compile_no_error_body")) {{setHighlightingEnabled(false);}});
		}
	}

	private boolean save() {

		String newPath;
		if (pathInput.getText().isEmpty() || pathInput.getText().equals(".lua")) {
			newPath = null;
		} else {
			//script could be stored where the original customObject is saved
			String pathPrefix;
//			pathPrefix = customObject.saveDirPath.substring(0, customObject.saveDirPath.length() - CustomDungeonSaves.fileName(customObject).length())
//					+ "scripts/";
			pathPrefix = "";


			newPath = pathPrefix + pathInput.getText();

			//make sure the path has the correct extension
			if (!newPath.endsWith(".lua")) newPath += ".lua";

			newPath = ResourcePath.removeSpacesInPath(newPath);
		}

		if (scriptPath == null) {
			if (newPath == null) {
				//TODO tzzz cannot create file: invalid name
				return false;
			}

			FileHandle saveTo = CustomObject.getResourceFile(newPath, false);
			if (saveTo.exists()) {
				//TODO tzzz cannot override!
				return false;
			}

			try {
				CustomDungeonSaves.writeClearText(saveTo, createFullScript());
				unsavedChanges = false;
				customObject.setLuaScriptPath(newPath);
			} catch (IOException e) {
				EditorScene.show(new WndError(Messages.get(IDEWindow.class, "write_file_exception", e.getClass().getSimpleName(), e.getMessage())) {{
					setHighlightingEnabled(false);}});
				return false;
			}
			return true;
		}

		if (newPath != null && !newPath.equals(scriptPath)) {
			//save location has changed!
			//fragen, ob alter pfad gelöscht werden soll, warnung, dass alle anderen die denselben pfad verwenden neu konfiguriert werden müssen
			return false;
		} else {
			FileHandle saveTo = CustomObject.getResourceFile(scriptPath, false);
			try {
				CustomDungeonSaves.writeClearText(saveTo, createFullScript());
				unsavedChanges = false;
				customObject.setLuaScriptPath(newPath);
			} catch (IOException e) {
				EditorScene.show(new WndError(Messages.get(IDEWindow.class, "write_file_exception", e.getClass().getSimpleName(), e.getMessage())) {{
					setHighlightingEnabled(false);}});
				return false;
			}
			return true;
		}

//		if (saveTo.exists()) {
//			if (script == null || !script.pathFromRoot.equals(luaCodeHolder.pathToScript)){
//				EditorScene.show(new WndError(Messages.get(IDEWindow.class, "script_in_use_body")));
//				return false;
//			}
//		}

//		if (script != null) {
//			for (LuaScript ls : CustomDungeonSaves.findScripts(null)) {
//				if (luaCodeHolder.pathToScript.equals(ls.pathFromRoot)) {
//					Class<?> luca = script.type;
//					while (!luca.isAssignableFrom(clazz)) {
//						luca = script.type.getSuperclass();
//					}
//					if (luca == GameObject.class || luca == Object.class) {
//						EditorScene.show(new WndError(Messages.get(IDEWindow.class, "save_duplicate_name_error", luaCodeHolder.pathToScript)) {{
//							setHighlightingEnabled(false);
//						}});
//						return false;
//					}
//				}
//			}
//		}
	}

	public static SimpleWindow showWindow(LuaCustomObject customObject, CustomObjSelector<String> customObjSelector, Class<?> clazz) {

		if (Game.platform.openNativeIDEWindow(customObject, customObjSelector, clazz)) return null;

		SimpleWindow w = new SimpleWindow((int) (PixelScene.uiCamera.width * 0.8f),  (int) (PixelScene.uiCamera.height * 0.9f)) {
			IDEWindow ideWindow = new IDEWindow(customObject, clazz, this::layout);
			{
				initComponents(null, ideWindow, ideWindow.getOutsideSp(), 0f, 0f, new ScrollPaneWithScrollbar(ideWindow));
			}
			@Override
			public void hide() {
				if (ideWindow.unsavedChanges || true) {
					if (!ideWindow.save()) {
						Runnable superHide = super::hide;
						GameScene.show(new WndOptions(Icons.WARNING.get(),
								Messages.get(IDEWindow.class, "close_unsaved_title"),
								Messages.get(IDEWindow.class, "close_unsaved_body"),
								Messages.get(IDEWindow.class, "close_unsaved_close_and_lose"),
								Messages.get(IDEWindow.class, "close_unsaved_cancel")) {
							@Override
							protected void onSelect(int index) {
								if (index == 0) {
									superHide.run();
									customObjSelector.setValue(customObject.getLuaScriptPath());
								}
							}
						});
						return;
					}
				}
				super.hide();
				customObjSelector.setValue(customObject.getLuaScriptPath());
			}
		};

		DungeonScene.show(w);

		return w;
	}

	public void selectScript(String scriptPath, LuaScript script, boolean force) {
		if (force) this.scriptPath = scriptPath;
		String cleanedCode;
		LuaScript currentScript;
		if (force) {
			if (script == null) {
				currentScript = new LuaScript(Object.class, "");
				currentScript.code = "";
				cleanedCode = "";
			} else {
				currentScript = script;
				cleanedCode = LuaScript.cleanLuaCode(script.code);
				pathInput.setText(scriptPath);
			}
		} else {
			currentScript = script;
			cleanedCode = LuaScript.cleanLuaCode(script == null ? "" : script.code);
		}

		List<String> functions = LuaScript.allFunctionNames(cleanedCode);
		for (CodeInputPanel inputPanel : codeInputPanels) {
			if (inputPanel == null) continue;
			inputPanel.applyScript(force, currentScript, cleanedCode);
			if (inputPanel instanceof MethodPanel) functions.remove(((MethodPanel) inputPanel).getMethodName());
		}
		additionalCode.actualApplyScript(force, currentScript, cleanedCode, functions);
		IDEWindow.this.layout();
	}

	private class OutsideSp extends Component {
		private RedButton btnCompile, btnSave, btnOpenMenu;
		private StyledButton btnCopy, btnPaste;

		@Override
		protected void createChildren() {
			btnCompile = new RedButton(Messages.get(IDEWindow.class, "compile"), PixelScene.landscape() ? 8 : 6) {
				@Override
				protected void onClick() {
					compile();
				}

				@Override
				protected String hoverText() {
					return Messages.get(IDEWindow.class, "compile");
				}
			};
			btnCompile.multiline = true;
			add(btnCompile);

//			btnSave = new RedButton(Messages.get(IDEWindow.class, "save")){
//				@Override
//				protected void onClick() {
//					save();
//				}
//
//				@Override
//				protected String hoverText() {
//					return Messages.get(IDEWindow.class, "save");
//				}
//			};
//			add(btnSave);

			btnCopy = new RedButton(""){
				@Override
				protected void onPointerDown() {
					super.onPointerDown();
					PointerEvent.clearKeyboardThisPress = false;
				}

				@Override
				protected void onPointerUp() {
					super.onPointerUp();
					PointerEvent.clearKeyboardThisPress = false;
				}

				@Override
				protected void onClick() {
					super.onClick();
					TextInput textInput = TextInput.getWithFocus();
					if (textInput != null) textInput.copyToClipboard();
				}
			};
			btnCopy.icon(Icons.COPY.get());
			add(btnCopy);

			btnPaste = new RedButton(""){
				@Override
				protected void onPointerDown() {
					super.onPointerDown();
					PointerEvent.clearKeyboardThisPress = false;
				}

				@Override
				protected void onPointerUp() {
					super.onPointerUp();
					PointerEvent.clearKeyboardThisPress = false;
				}

				@Override
				protected void onClick() {
					super.onClick();
					TextInput textInput = TextInput.getWithFocus();
					if (textInput != null) textInput.pasteFromClipboard();
				}

			};
			btnPaste.icon(Icons.PASTE.get());
			add(btnPaste);

			btnOpenMenu = new RedButton("") {

				@Override
				protected void onClick() {
					DungeonScene.show(new OutsideSpMenuPopup(
							(int) ((x + btnOpenMenu.width() + 2 - camera().width / 2f)),
					(int) (y - camera().height / 2f) - 3) {

					});
				}

				@Override
				protected String hoverText() {
					return Messages.get(IDEWindow.class, "more");
				}
			};
			btnOpenMenu.icon(Icons.MENU.get());
			add(btnOpenMenu);
		}

		@Override
		protected void layout() {
			float h = 18;
			float gap = 1;
			float posX = x + width;

			btnOpenMenu.setRect(posX - gap - h, y, h, h);
			posX = btnOpenMenu.left();

			btnPaste.setRect(posX - gap - h, y, h, h);
			posX = btnPaste.left();

			btnCopy.setRect(posX - gap - h, y, h, h);
			posX = btnCopy.left();

			float w = (posX - x - gap*4) / 1f;
//			btnSave.setRect(x + gap*2, y, w, h);
			btnCompile.setRect(x + gap*2, y, w, h);

			PixelScene.align(btnOpenMenu);
			PixelScene.align(btnPaste);
			PixelScene.align(btnCopy);
//			PixelScene.align(btnSave);
			PixelScene.align(btnCompile);

			height = h;
		}
	}

	private class OutsideSpMenuPopup extends PopupMenu {

		public OutsideSpMenuPopup(int posX, int posY) {

			finishInstantiation(new RedButton[] {
					new NewInstanceButton(this) {
						@Override
						protected void onSelect(String insertText) {
							TextInput textInput = TextInput.getWithFocus();
							if (textInput != null && textInput != pathInput) textInput.insert(insertText);
						}
					},
					new RedButton(Messages.get(IDEWindow.class, "insert_full")) {
						@Override
						protected void onClick() {
							LuaTemplates.show(script -> {
								if (script != null) {
									selectScript(null, script, false);
									OutsideSpMenuPopup.this.hideImmediately();
								}
							}, clazz);
						}
					},
					new RedButton(Messages.get(IDEWindow.class, "view_documentation")) {
						@Override
						protected void onClick() {
							CodeInputPanelInterface.viewDocumentation();
						}
					},
//					new RedButton(Messages.get(IDEWindow.class, "insert_line")) {
//						@Override
//						protected void onClick() {
//							//TODO way to add class("xxx");
//							//and some other examples...
//						}
//					},

			}, posX, posY, 200);
		}
	}

	public static void chooseClassName(BiConsumer<String, Object> whatToDo) {
		WndEditorInv.chooseClass = true;
		EditorScene.selectItem(new WndBag.ItemSelectorInterface() {
			@Override
			public String textPrompt() {
				return null;
			}

			@Override
			public Class<? extends Bag> preferredBag() {
				return null;
			}

			@Override
			public List<Bag> getBags() {
				return Arrays.asList(Mobs.bag(), Items.bag(), Traps.bag(), Plants.bag(), Buffs.bag());
			}

			@Override
			public boolean itemSelectable(Item item) {
				return true;
			}

			@Override
			public void onSelect(Item item) {
				WndEditorInv.chooseClass = false;
				Object obj;
				if (item instanceof EditorItem) obj = ((EditorItem<?>) item).getObject();
				else obj = item;
				if (obj == null) {
					whatToDo.accept(null, null);
					return;
				}
				Class<?> clazz = obj.getClass();
				while (CustomObjectClass.class.isAssignableFrom(clazz)) clazz = clazz.getSuperclass();
				String clName = clazz.getSimpleName();
				if (clName.equals("Barrier")) clName = clazz.getName();
				if (obj instanceof Plant.Seed) clName = obj.getClass().getEnclosingClass().getSimpleName() + "$" + clName;
				whatToDo.accept(clName, obj);
			}

			@Override
			public boolean acceptsNull() {
				return false;
			}
		});
	}

	public static void showSelectScriptWindow(Class<?> clazz, Consumer<LuaScript> onSelect) {
		List<LuaScript> scripts = CustomDungeonSaves.findScripts(script -> {
			Class<?> luca = script.type;
			while (!luca.isAssignableFrom(clazz)) {
				luca = luca.getSuperclass();
			}
			return luca != GameObject.class && luca != Object.class;
		});

		String[] options = new String[scripts.size()];
		String[] descs = new String[options.length];
		int i = 0;
		for (LuaScript s : scripts) {
			options[i] = s.toString();
			descs[i++] = s.desc;
		}

		if (options.length == 0) {
			EditorScene.show(new WndError(Messages.get(IDEWindow.class, "no_scripts_available")));
			return;
		}

		EditorScene.show(new WndOptions(
				Messages.get(IDEWindow.class, "choose_script_title"),
				Messages.get(IDEWindow.class, "choose_script_body", CustomDungeonSaves.getAdditionalFilesDir().file().getAbsolutePath()),
				options
		) {
			{
				tfMessage.setHighlighting(false);
			}

			@Override
			protected Image getIcon(int index) {
				return LuaManager.scriptSprite(scripts.get(index));
			}

			@Override
			protected boolean hasInfo(int index) {
				return descs[index] != null && !descs[index].isEmpty();
			}

			@Override
			protected void onInfo(int index) {
				EditorScene.show(new WndTitledMessage(
						Icons.get(Icons.INFO),
						Messages.titleCase(options[index]),
						descs[index]));
			}

			@Override
			protected void onSelect(int index) {
				onSelect.accept(scripts.get(index));
			}

			@Override
			public void hide() {
				super.hide();
				onSelect.accept(null);
			}
		});
	}


}