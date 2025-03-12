/*-
 * #%L
 * This file is part of QuPath.
 * %%
 * Copyright (C) 2018 - 2022 QuPath developers, The University of Edinburgh
 * %%
 * QuPath is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * QuPath is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License 
 * along with QuPath.  If not, see <https://www.gnu.org/licenses/>.
 * #L%
 */

package qupath.lib.gui.tools;

import java.util.ArrayList;
import java.util.function.Function;

import javafx.beans.binding.Bindings;
import org.kordamp.ikonli.javafx.StackedFontIcon;

import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ContentDisplay;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.Tooltip;
import javafx.scene.control.TreeCell;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.TilePane;
import qupath.lib.gui.QuPathGUI;
import qupath.lib.gui.prefs.PathPrefs;
import qupath.lib.gui.prefs.PathPrefs.DetectionTreeDisplayModes;
import qupath.lib.objects.PathAnnotationObject;
import qupath.lib.objects.PathObject;
import qupath.lib.objects.TMACoreObject;

/**
 * Create standardized {@link ListCell} and {@link TreeCell} instances for displaying a {@link PathObject}, 
 * or a generic pane to use elsewhere.
 * 
 * @author Pete Bankhead
 * @since v0.4.0
 */
public class PathObjectLabels {

	/**
	 * Create a {@link PathObjectMiniPane} using the default {@link PathObject#toString()} method.
	 * @return
	 */
	public static PathObjectMiniPane createPane() {
		return createPane(PathObject::toString);
	}

	/**
	 * Create a {@link PathObjectMiniPane} using a custom method to create a string representation of the object.
	 * @param stringExtractor
	 * @return
	 */
	public static PathObjectMiniPane createPane(Function<PathObject, String> stringExtractor) {
		var pane = new PathObjectMiniPane(stringExtractor);
		pane.pane.setPadding(new Insets(5.0));
		pane.pane.setPrefHeight(pane.h+16);
		return pane;
	}
	
	/**
	 * Create a {@link ListCell} for displaying a {@link PathObject} using the default {@link PathObject#toString()} method.
	 * @return
	 */
	public static ListCell<PathObject> createListCell() {
		return createListCell(PathObject::toString);
	}
	
	/**
	 * Create a {@link ListCell} for displaying a {@link PathObject} using a custom method to create a string representation of the object.
	 * @param stringExtractor 
	 * @return
	 */
	public static ListCell<PathObject> createListCell(Function<PathObject, String> stringExtractor) {
		return new PathObjectListCell(stringExtractor);
	}
	
	/**
	 * Create a {@link TreeCell} for displaying a {@link PathObject} using the default {@link PathObject#toString()} method.
	 * @return
	 */
	public static TreeCell<PathObject> createTreeCell() {
		return createTreeCell(PathObject::toString);
	}
	
	/**
	 * Create a {@link TreeCell} for displaying a {@link PathObject} using a custom method to create a string representation of the object.
	 * @param stringExtractor 
	 * @return
	 */
	public static TreeCell<PathObject> createTreeCell(Function<PathObject, String> stringExtractor) {
		return new PathObjectTreeCell(stringExtractor);
	}
	
	
	private static class PathObjectTreeCell extends TreeCell<PathObject> {
		
		private final PathObjectMiniPane miniPane;
		
		PathObjectTreeCell(Function<PathObject, String> stringExtractor) {
			super();
			this.miniPane = new PathObjectMiniPane(stringExtractor);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		
		@Override
		protected void updateItem(PathObject value, boolean empty) {
			super.updateItem(value, empty);
			this.miniPane.setPathObject(empty ? null : value);
			if (value == null || empty) {
				setText(null);
				setGraphic(null);
				return;
			}			
			setGraphic(this.miniPane.getNode());
		}
		
	}


	private static class PathObjectListCell extends ListCell<PathObject> {
		
		private final PathObjectMiniPane miniPane;
		
		PathObjectListCell(Function<PathObject, String> stringExtractor) {
			super();
			this.miniPane = new PathObjectMiniPane(stringExtractor);
			setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
		}
		
		@Override
		protected void updateItem(PathObject value, boolean empty) {
			super.updateItem(value, empty);
			this.miniPane.setPathObject(empty ? null : value);
			if (value == null || empty) {
				setText(null);
				setGraphic(null);
				return;
			}
			setGraphic(this.miniPane.getNode());
		}
		
	}
	
	
	/**
	 * Manage a small pane that can be used to display a {@link PathObject}.
	 * Intended for use creating standardized list and tree cells.
	 */
	public static class PathObjectMiniPane {

		private final Tooltip tooltip = new Tooltip();
		private Function<PathObject, String> fun;
		
		private final int w = 16, h = 16;
		
		private final Node lockIcon;
		private final Node unlockIcon;
		private final Node descriptionIcon;
		private final Pane iconPane;

		private final Tooltip descriptionTooltip = new Tooltip();

		private final Label label;
		private final BorderPane pane;

		// TODO - Decide if unlocked icons should be shown by default
		private static boolean showUnlocked = false;

		/**
		 * Constructor using a custom string extraction function.
		 * @param stringExtractor function to generate a String representation of the object.
		 */
		public PathObjectMiniPane(Function<PathObject, String> stringExtractor) {
			this.fun = stringExtractor;
			if (this.fun == null)
				this.fun = PathObject::toString;
			
			pane = new BorderPane();
			label = new Label();
			label.setTextOverrun(OverrunStyle.ELLIPSIS);

			var sp = new StackPane(label);
			sp.setPrefWidth(1.0);
			sp.setMinHeight(0.0);
			StackPane.setAlignment(label, Pos.CENTER_LEFT);
			pane.setCenter(sp);
			
			lockIcon = createLockIcon();
			unlockIcon = createUnlockIcon();
			descriptionIcon = createDescriptionIcon();

			var qupath = QuPathGUI.getInstance();
			var actions = qupath == null ? null : qupath.getCommonActions();
			if (actions != null) {
				descriptionIcon.setOnMouseClicked(e -> {
					if (e.getClickCount() == 2) {
						actions.SHOW_OBJECT_DESCRIPTIONS.handle(new ActionEvent());
						e.consume();
					}
				});
			}
			
			Tooltip.install(descriptionIcon, descriptionTooltip);
			
			iconPane = new TilePane();

			var lockedTooltip = new Tooltip("ROI locked");
			Tooltip.install(lockIcon, lockedTooltip);
			
			BorderPane.setAlignment(label, Pos.CENTER_LEFT);
			BorderPane.setAlignment(iconPane, Pos.CENTER_RIGHT);
		}

		private static Node createDescriptionIcon() {
			var descriptionIcon = new StackedFontIcon();
			descriptionIcon.setIconCodeLiterals("ion4-md-list");
			descriptionIcon.setStyle("-fx-background-color: -fx-background; -fx-icon-color: -fx-text-background-color;");
			descriptionIcon.setOpacity(0.6);
			return descriptionIcon;
		}

		private static Node createLockIcon() {
			var lockIcon = new StackedFontIcon();
			lockIcon.setIconCodeLiterals("ion4-md-lock");
			lockIcon.setStyle("-fx-background-color: -fx-background; -fx-icon-color: -fx-text-background-color;");
			lockIcon.setOpacity(0.6);
			return lockIcon;
		}

		// We don't currently use unlocked icons to save space - but they could be added back in if needed
		private static Node createUnlockIcon() {
			var unlockIcon = new StackedFontIcon();
			unlockIcon.setIconCodeLiterals("ion4-md-unlock");
			unlockIcon.setStyle("-fx-background-color: -fx-background; -fx-icon-color: -fx-text-background-color;");
			unlockIcon.opacityProperty().bind(
					Bindings.createDoubleBinding(() -> unlockIcon.isHover() ? 0.4 : 0.1,
							unlockIcon.hoverProperty()));
			return unlockIcon;
		}

		
		/**
		 * Set the {@link PathObject} to display (may be null).
		 * @param value
		 */
		public void setPathObject(PathObject value) {
			if (value == null) {
				label.setText(null);
				label.setGraphic(null);
				pane.setRight(null);
				return;
			}

			label.setText(fun.apply(value));

			String description = getDescription(value);
			updateTooltips(value, description);

			if (showRoiIcon(value)) {
				Node icon = IconFactory.createPathObjectIcon(value, w, h);
				label.setGraphic(icon);
			} else {
				label.setGraphic(null);
			}

			boolean hasDescription = description != null && !description.isBlank();
			if (hasDescription)
				descriptionTooltip.setText(description);
			else
				descriptionTooltip.setText("No description");
			boolean isLocked = value.isLocked();

			var icons = new ArrayList<Node>();
			if (hasDescription) {
				icons.add(descriptionIcon);
			} if (isLocked) {
				icons.add(lockIcon);
			} else if (showUnlocked) {
				icons.add(unlockIcon);
			}

			if (icons.isEmpty())
				pane.setRight(null);
			else {
				iconPane.getChildren().setAll(icons);
				pane.setRight(iconPane);
			}
		}
		
		
		private String getDescription(PathObject pathObject) {
			if (pathObject instanceof PathAnnotationObject)
				return ((PathAnnotationObject)pathObject).getDescription();
			if (pathObject instanceof TMACoreObject core)
				return core.getMetadata().getOrDefault("Note", null); // TODO: Improve naming of TMA note!
			return null;
		}
		
		
		private boolean showRoiIcon(PathObject pathObject) {
			if (pathObject == null || !pathObject.hasROI())
				return false;
			return !pathObject.isDetection() || PathPrefs.detectionTreeDisplayModeProperty().get() == DetectionTreeDisplayModes.WITH_ICONS;
		}
		
		
		/**
		 * Get the node to display.
		 * @return
		 */
		public Pane getNode() {
			return pane;
		}

		private void updateTooltips(PathObject pathObject, String description) {
			if (pathObject == null) { // || !pathObject.isAnnotation()) {
				label.setTooltip(null);
				return;
			}
			tooltip.setText(label.getText());
			label.setTooltip(tooltip);
			if (description == null)
				descriptionTooltip.setText("No description");
			else
				descriptionTooltip.setText(description);
		}

	}

}
