/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.ide.bookmarks;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.icons.AllIcons;
import com.intellij.ide.IdeBundle;
import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.ide.structureView.StructureViewModel;
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder;
import com.intellij.lang.LanguageStructureViewBuilder;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.NavigationItem;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.editor.colors.CodeInsightColors;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.openapi.editor.ex.MarkupModelEx;
import com.intellij.openapi.editor.ex.RangeHighlighterEx;
import com.intellij.openapi.editor.impl.DocumentMarkupModel;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.editor.markup.HighlighterLayer;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.editor.markup.TextAttributes;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.ui.ColorUtil;
import com.intellij.ui.JBColor;
import com.intellij.ui.RetrievableIcon;
import com.intellij.util.NotNullProducer;
import com.intellij.util.PlatformIcons;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;

public class Bookmark implements Navigatable, Comparable<Bookmark> {
  public static final Icon DEFAULT_ICON = new MyCheckedIcon();

  private final VirtualFile myFile;
  @NotNull private final OpenFileDescriptor myTarget;
  private final Project myProject;

  private String myDescription;
  private char myMnemonic = 0;
  public static final Font MNEMONIC_FONT = new Font("Monospaced", 0, 11);

  public Bookmark(@NotNull Project project, @NotNull VirtualFile file, int line, @NotNull String description) {
    myFile = file;
    myProject = project;
    myDescription = description;

    myTarget = new OpenFileDescriptor(project, file, line, -1, true);

    addHighlighter();
  }

  @Override
  public int compareTo(Bookmark o) {
    int i = myMnemonic != 0 ? o.myMnemonic != 0 ? myMnemonic - o.myMnemonic : -1: o.myMnemonic != 0 ? 1 : 0;
    if (i != 0) return i;
    i = myProject.getName().compareTo(o.myProject.getName());
    if (i != 0) return i;
    i = myFile.getName().compareTo(o.getFile().getName());
    if (i != 0) return i;
    return myTarget.compareTo(o.myTarget);
  }

  public void updateHighlighter() {
    release();
    addHighlighter();
  }

  private void addHighlighter() {
    Document document = FileDocumentManager.getInstance().getCachedDocument(getFile());
    if (document != null) {
      createHighlighter((MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true));
    }
  }

  public RangeHighlighter createHighlighter(@NotNull MarkupModelEx markup) {
    final RangeHighlighterEx myHighlighter;
    int line = getLine();
    if (line >= 0) {
      myHighlighter = markup.addPersistentLineHighlighter(line, HighlighterLayer.ERROR + 1, null);
      if (myHighlighter != null) {
        myHighlighter.setGutterIconRenderer(new MyGutterIconRenderer(this));

        TextAttributes textAttributes =
          EditorColorsManager.getInstance().getGlobalScheme().getAttributes(CodeInsightColors.BOOKMARKS_ATTRIBUTES);

        Color stripeColor = textAttributes.getErrorStripeColor();
        myHighlighter.setErrorStripeMarkColor(stripeColor != null ? stripeColor : Color.black);
        myHighlighter.setErrorStripeTooltip(getBookmarkTooltip());

        TextAttributes attributes = myHighlighter.getTextAttributes();
        if (attributes == null) {
          attributes = new TextAttributes();
        }
        attributes.setBackgroundColor(textAttributes.getBackgroundColor());
        attributes.setForegroundColor(textAttributes.getForegroundColor());
        myHighlighter.setTextAttributes(attributes);
      }
    }
    else {
      myHighlighter = null;
    }
    return myHighlighter;
  }

  @Nullable
  public Document getDocument() {
    return FileDocumentManager.getInstance().getCachedDocument(getFile());
  }

  public void release() {
    int line = getLine();
    if (line < 0) {
      return;
    }
    final Document document = getDocument();
    if (document == null) return;
    MarkupModelEx markup = (MarkupModelEx)DocumentMarkupModel.forDocument(document, myProject, true);
    final Document markupDocument = markup.getDocument();
    if (markupDocument.getLineCount() <= line) return;
    final int startOffset = markupDocument.getLineStartOffset(line);
    final int endOffset = markupDocument.getLineEndOffset(line);

    final Ref<RangeHighlighterEx> found = new Ref<RangeHighlighterEx>();
    markup.processRangeHighlightersOverlappingWith(startOffset, endOffset, new Processor<RangeHighlighterEx>() {
      @Override
      public boolean process(RangeHighlighterEx highlighter) {
        GutterMark renderer = highlighter.getGutterIconRenderer();
        if (renderer instanceof MyGutterIconRenderer && ((MyGutterIconRenderer)renderer).myBookmark == Bookmark.this) {
          found.set(highlighter);
          return false;
        }
        return true;
      }
    });
    if (!found.isNull()) found.get().dispose();
  }

  public Icon getIcon() {
    return myMnemonic == 0 ? DEFAULT_ICON : MnemonicIcon.getIcon(myMnemonic);
  }

  public String getDescription() {
    return myDescription;
  }

  public void setDescription(String description) {
    myDescription = description;
  }

  public char getMnemonic() {
    return myMnemonic;
  }

  public void setMnemonic(char mnemonic) {
    myMnemonic = Character.toUpperCase(mnemonic);
  }

  @NotNull
  public VirtualFile getFile() {
    return myFile;
  }

  @Nullable
  public String getNotEmptyDescription() {
    return StringUtil.isEmpty(myDescription) ? null : myDescription;
  }

  public boolean isValid() {
    if (!getFile().isValid()) {
      return false;
    }

    // There is a possible case that target document line that is referenced by the current bookmark is removed. We assume
    // that corresponding range marker becomes invalid then.
    RangeMarker rangeMarker = myTarget.getRangeMarker();
    return rangeMarker == null || rangeMarker.isValid();
  }

  @Override
  public boolean canNavigate() {
    return myTarget.canNavigate();
  }

  @Override
  public boolean canNavigateToSource() {
    return myTarget.canNavigateToSource();
  }

  @Override
  public void navigate(boolean requestFocus) {
    myTarget.navigate(requestFocus);
  }

  public int getLine() {
    RangeMarker marker = myTarget.getRangeMarker();
    if (marker != null && marker.isValid()) {
      Document document = marker.getDocument();
      return document.getLineNumber(marker.getStartOffset());
    }
    return myTarget.getLine();
  }

  @Override
  public String toString() {
    StringBuilder result = new StringBuilder(getQualifiedName());
    String description = StringUtil.escapeXml(getNotEmptyDescription());
    if (description != null) {
      result.append(": ").append(description);
    }
    return result.toString();
  }

  public String getQualifiedName() {
    String presentableUrl = myFile.getPresentableUrl();
    if (myFile.isDirectory()) return presentableUrl;

    PsiDocumentManager.getInstance(myProject).commitAllDocuments();
    final PsiFile psiFile = PsiManager.getInstance(myProject).findFile(myFile);

    if (psiFile == null) return presentableUrl;

    StructureViewBuilder builder = LanguageStructureViewBuilder.INSTANCE.getStructureViewBuilder(psiFile);
    if (builder instanceof TreeBasedStructureViewBuilder) {
      StructureViewModel model = ((TreeBasedStructureViewBuilder)builder).createStructureViewModel(null);
      Object element;
      try {
        element = model.getCurrentEditorElement();
      }
      finally {
        model.dispose();
      }
      if (element instanceof NavigationItem) {
        ItemPresentation presentation = ((NavigationItem)element).getPresentation();
        if (presentation != null) {
          presentableUrl = ((NavigationItem)element).getName() + " " + presentation.getLocationString();
        }
      }
    }

    return IdeBundle.message("bookmark.file.X.line.Y", presentableUrl, getLine() + 1);
  }

  private String getBookmarkTooltip() {
    StringBuilder result = new StringBuilder("Bookmark");
    if (myMnemonic != 0) {
      result.append(" ").append(myMnemonic);
    }
    String description = StringUtil.escapeXml(getNotEmptyDescription());
    if (description != null) {
      result.append(": ").append(description);
    }
    return result.toString();
  }

  static class MnemonicIcon implements Icon {
    private static final MnemonicIcon[] cache = new MnemonicIcon[36];//0..9  + A..Z
    private final char myMnemonic;

    @NotNull
    static MnemonicIcon getIcon(char mnemonic) {
      int index = mnemonic - 48;
      if (index > 9)
        index -= 7;
      if (index < 0 || index > cache.length-1)
        return new MnemonicIcon(mnemonic);
      if (cache[index] == null)
        cache[index] = new MnemonicIcon(mnemonic);
      return cache[index];
    }

    private MnemonicIcon(char mnemonic) {
      myMnemonic = mnemonic;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      g.setColor(new JBColor(new NotNullProducer<Color>() {
        @NotNull
        @Override
        public Color produce() {
          //noinspection UseJBColor
          return !darkBackground() ? new Color(0xffffcc) : new Color(0x675133);
        }
      }));
      g.fillRect(x, y, getIconWidth(), getIconHeight());

      g.setColor(JBColor.GRAY);
      g.drawRect(x, y, getIconWidth(), getIconHeight());

      g.setColor(EditorColorsManager.getInstance().getGlobalScheme().getDefaultForeground());
      final Font oldFont = g.getFont();
      g.setFont(MNEMONIC_FONT);

      ((Graphics2D)g).drawString(Character.toString(myMnemonic), x + 3, y + getIconHeight() - 1.5F);
      g.setFont(oldFont);
    }

    @Override
    public int getIconWidth() {
      return DEFAULT_ICON.getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return DEFAULT_ICON.getIconHeight();
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      MnemonicIcon that = (MnemonicIcon)o;

      return myMnemonic == that.myMnemonic;
    }

    @Override
    public int hashCode() {
      return (int)myMnemonic;
    }
  }

  private static class MyCheckedIcon implements Icon, RetrievableIcon {
    @Nullable
    @Override
    public Icon retrieve() {
      return PlatformIcons.CHECK_ICON;
    }

    @Override
    public void paintIcon(Component c, Graphics g, int x, int y) {
      (darkBackground() ? AllIcons.Actions.CheckedGrey : AllIcons.Actions.CheckedBlack).paintIcon(c, g, x, y);
    }

    @Override
    public int getIconWidth() {
      return PlatformIcons.CHECK_ICON.getIconWidth();
    }

    @Override
    public int getIconHeight() {
      return PlatformIcons.CHECK_ICON.getIconHeight();
    }
  }

  private static boolean darkBackground() {
    Color gutterBackground = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.GUTTER_BACKGROUND);
    if (gutterBackground == null) {
      gutterBackground = EditorColors.GUTTER_BACKGROUND.getDefaultColor();
    }
    return ColorUtil.isDark(gutterBackground);
  }

  private static class MyGutterIconRenderer extends GutterIconRenderer implements DumbAware {
    private final Bookmark myBookmark;

    public MyGutterIconRenderer(@NotNull Bookmark bookmark) {
      myBookmark = bookmark;
    }

    @Override
    @NotNull
    public Icon getIcon() {
      return myBookmark.getIcon();
    }

    @Override
    public String getTooltipText() {
      return myBookmark.getBookmarkTooltip();
    }

    @Override
    public boolean equals(Object obj) {
      return obj instanceof MyGutterIconRenderer &&
             Comparing.equal(getTooltipText(), ((MyGutterIconRenderer)obj).getTooltipText()) &&
             Comparing.equal(getIcon(), ((MyGutterIconRenderer)obj).getIcon());
    }

    @Override
     public int hashCode() {
      return getIcon().hashCode();
    }
  }
}
