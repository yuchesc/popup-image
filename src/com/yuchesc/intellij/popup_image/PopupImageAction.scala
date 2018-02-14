package com.yuchesc.intellij.popup_image

import java.awt.image.BufferedImage
import java.awt.{Dimension, FlowLayout, Image}
import javax.imageio.ImageIO
import javax.swing.{ImageIcon, JLabel, JPanel}

import com.intellij.openapi.actionSystem.{AnAction, AnActionEvent, CommonDataKeys}
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ui.popup.{JBPopup, JBPopupFactory}
import com.intellij.openapi.vfs.VirtualFile

class PopupImageAction extends AnAction {
  val logger: Logger = Logger.getInstance(classOf[PopupImageAction])
  // support extensions
  val extensions = Array(".jpg", ".jpeg", ".png", ".gif")

  def isSupportedExtension(path: String): Boolean = extensions.exists(path.endsWith)

  def createPopup(name: String, img: Image): JBPopup = {
    val panel = new JPanel(new FlowLayout())
    val label = new JLabel(new ImageIcon(img))
    panel.add(label)
    val title = s"$name [${img.getWidth(null)}x${img.getHeight(null)}]"
    JBPopupFactory.getInstance.createComponentPopupBuilder(panel, null)
      .setTitle(title)
      .setFocusable(false)
      .setRequestFocus(false)
      .setMayBeParent(false)
      .setMovable(true)
      .setMinSize(new Dimension(200, 200))
      .setCancelOnClickOutside(true)
      .setCancelOnOtherWindowOpen(false)
      .setCancelKeyEnabled(true)
      .createPopup
  }

  // enable this action while editing text
  override def update(e: AnActionEvent): Unit = {
    val editor = e.getData(CommonDataKeys.EDITOR)
    val selectionModel = editor.getSelectionModel
    e.getPresentation.setEnabled(
      (e.getData(CommonDataKeys.EDITOR) != null) &&
        selectionModel.hasSelection &&
        isSupportedExtension(selectionModel.getSelectedText))
  }

  var currentPopup: Option[JBPopup] = None

  def showPopup(editor: Editor, file: VirtualFile, selectedWord: String): Unit = {
    val image = file.getParent.findFileByRelativePath(selectedWord)

    if (image != null && image.exists()) {
      logger.info(s"Found file ${image.getPath}")
      try {
        val img: BufferedImage = ImageIO.read(image.getInputStream)
        currentPopup.foreach { p =>
          p.setUiVisible(false)
          p.dispose()
        }

        currentPopup = Option(createPopup(selectedWord, img))
        currentPopup.foreach(_.showInBestPositionFor(editor))
      } catch {
        case e: Exception => logger.warn("cannot load image.", e)
      }
    } else {
      logger.info(s"File not found. $image")
    }
  }

  override def actionPerformed(e: AnActionEvent) {
    val editor = e.getData(CommonDataKeys.EDITOR)
    // Get selected text
    val selectedWord = editor.getSelectionModel.getSelectedText
    val file = e.getData(CommonDataKeys.VIRTUAL_FILE)
    showPopup(editor, file, selectedWord)
  }
}
