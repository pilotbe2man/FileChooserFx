package com.chainstaysoftware.filechooser;

import com.chainstaysoftware.filechooser.preview.PreviewPane;
import javafx.scene.Cursor;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.Pane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class AbstractFilesView implements FilesView {
   private static Logger logger = Logger.getLogger("com.chainstaysoftware.filechooser.AbstractFilesView");

   private final Stage parent;

   AbstractFilesView(final Stage parent) {
      this.parent = parent;
   }

   /**
    * Create {@link Stage} to display {@link PreviewPane} and show the {@link Stage}
    * @param previewPaneClass {@link Node} to display the file within.
    * @param file {@link File} to preview.
    */
   void showPreview(final Class<? extends PreviewPane> previewPaneClass,
                    final File file) {
      final Optional<PreviewPane> previewPaneOpt = PreviewPaneFactory.create(previewPaneClass);
      if (!previewPaneOpt.isPresent()) {
         logger.log(Level.SEVERE, "No PreviewPane created.");
         return;
      }

      parent.getScene().setCursor(Cursor.WAIT);

      new FilesViewRunnable(previewPaneOpt.orElseThrow(IllegalStateException::new),
         file).run();
   }

   private class FilesViewRunnable implements Runnable {
      private final PreviewPane previewPane;
      private final File file;

      FilesViewRunnable(final PreviewPane previewPane,
                        final File file) {
         this.previewPane = previewPane;
         this.file = file;
      }

      @Override
      public void run() {
         final Stage stage = new Stage();
         final Pane pane = previewPane.getPane();
         pane.prefWidthProperty().bind(stage.widthProperty());
         pane.prefHeightProperty().bind(stage.heightProperty());
         pane.maxWidthProperty().bind(stage.widthProperty());
         pane.maxHeightProperty().bind(stage.heightProperty());
         pane.minWidthProperty().bind(stage.widthProperty());
         pane.minHeightProperty().bind(stage.heightProperty());

         final Scene scene = new Scene(new Pane(pane));
         scene.getStylesheets().add(new FileBrowserCss().getUrl());
         scene.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
               stage.close();
            }
         });

         previewPane.setFile(file);

         stage.setScene(scene);
         stage.initOwner(parent);
         stage.setAlwaysOnTop(true);
         stage.initStyle(StageStyle.UTILITY);
         stage.initModality(Modality.APPLICATION_MODAL);
         stage.setTitle(getTitle(file));
         stage.setWidth(1024);
         stage.setHeight(768);
         stage.setOnShown(windowEvent -> parent.getScene().setCursor(null));
         stage.show();
      }

      /**
       * Build a window title for the passed in file.
       * @param file
       */
      private String getTitle(final File file) {
         final int maxLength = 75;
         return StringUtils.abbreviateMiddle(file.getPath(), "...", maxLength);
      }
   }

   boolean compareFilePaths(final File f1, final File f2) {
      if (f1 == null || f2 == null) {
         return false;
      }

      try {
         return f1.getCanonicalFile().equals(f2.getCanonicalFile());
      } catch (IOException e) {
         logger.log(Level.SEVERE, "Error canonicalizing file", e);
         return f1.equals(f2);
      }
   }
}
