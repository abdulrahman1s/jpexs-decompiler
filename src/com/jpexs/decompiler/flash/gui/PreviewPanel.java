/*
 *  Copyright (C) 2010-2022 JPEXS
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.jpexs.decompiler.flash.gui;

import com.jpexs.decompiler.flash.SWF;
import com.jpexs.decompiler.flash.SWFHeader;
import com.jpexs.decompiler.flash.action.parser.ActionParseException;
import com.jpexs.decompiler.flash.configuration.Configuration;
import com.jpexs.decompiler.flash.exporters.PreviewExporter;
import com.jpexs.decompiler.flash.gui.controls.JPersistentSplitPane;
import com.jpexs.decompiler.flash.gui.debugger.DebuggerTools;
import com.jpexs.decompiler.flash.gui.editor.LineMarkedEditorPane;
import com.jpexs.decompiler.flash.gui.hexview.HexView;
import com.jpexs.decompiler.flash.gui.player.FlashPlayerPanel;
import com.jpexs.decompiler.flash.gui.player.MediaDisplay;
import com.jpexs.decompiler.flash.gui.player.PlayerControls;
import com.jpexs.decompiler.flash.tags.DefineBinaryDataTag;
import com.jpexs.decompiler.flash.tags.MetadataTag;
import com.jpexs.decompiler.flash.tags.SetBackgroundColorTag;
import com.jpexs.decompiler.flash.tags.Tag;
import com.jpexs.decompiler.flash.tags.UnknownTag;
import com.jpexs.decompiler.flash.tags.base.FontTag;
import com.jpexs.decompiler.flash.tags.base.PlaceObjectTypeTag;
import com.jpexs.decompiler.flash.tags.base.TextTag;
import com.jpexs.decompiler.flash.timeline.Frame;
import com.jpexs.decompiler.flash.timeline.TagScript;
import com.jpexs.decompiler.flash.timeline.Timelined;
import com.jpexs.decompiler.flash.treeitems.TreeItem;
import com.jpexs.decompiler.flash.types.MATRIX;
import com.jpexs.decompiler.flash.types.shaperecords.SHAPERECORD;
import com.jpexs.helpers.SerializableImage;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingConstants;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author JPEXS
 */
public class PreviewPanel extends JPersistentSplitPane implements TagEditorPanel {

    private static final String FLASH_VIEWER_CARD = "FLASHVIEWER";

    private static final String DRAW_PREVIEW_CARD = "DRAWPREVIEW";

    private static final String GENERIC_TAG_CARD = "GENERICTAG";

    private static final String BINARY_TAG_CARD = "BINARYTAG";

    private static final String UNKNOWN_TAG_CARD = "UNKNOWNTAG";

    private static final String METADATA_TAG_CARD = "METADATATAG";

    private static final String EMPTY_CARD = "EMPTY";

    private static final String CARDTEXTPANEL = "Text card";

    private static final String CARDFONTPANEL = "Font card";

    private static final String PLACE_TAG_CARD = "PLACETAG";

    private final MainPanel mainPanel;

    private final JPanel viewerCards;

    private final FlashPlayerPanel flashPanel;

    private File tempFile;

    private ImagePanel imagePanel;

    private PlayerControls imagePlayControls;

    private MediaDisplay media;

    private BinaryPanel binaryPanel;

    private LineMarkedEditorPane metadataEditor;

    private GenericTagPanel genericTagPanel;

    private GenericTagPanel placeGenericPanel;

    private JSplitPane placeSplitPane;

    private JPanel displayWithPreview;

    // Image tag buttons
    private JButton replaceShapeButton;
    
    private JButton replaceShapeUpdateBoundsButton;
    
    private JButton replaceSoundButton;
    
    private JButton replaceImageButton;

    private JButton replaceImageAlphaButton;

    private JButton prevFontsButton;

    private JButton nextFontsButton;

    // Binary tag buttons
    private JButton replaceBinaryButton;

    // Unknown tag buttons
    private JButton replaceUnknownButton;

    // Metadata editor buttons
    private JButton metadataEditButton;

    private JButton metadataSaveButton;

    private JButton metadataCancelButton;

    // Generic tag buttons
    private JButton genericEditButton;

    private JButton genericSaveButton;

    private JButton genericCancelButton;

    private JButton placeFreeTransformButton;

    private JButton placeEditButton;

    private JButton placeSaveButton;

    private JButton placeCancelButton;

    private JPanel parametersPanel;

    private FontPanel fontPanel;

    private int fontPageNum;

    private TextPanel textPanel;

    private MetadataTag metadataTag;

    private boolean readOnly = false;

    private ImagePanel placeImagePanel;

    private final int dividerSize;

    private PlaceObjectTypeTag placeTag;

    private MATRIX oldMatrix;

    private HexView unknownHexView;

    private final int PLACE_EDIT_FREETRANSFORM = 1;
    private final int PLACE_EDIT_RAW = 2;
    private int placeEditMode = 0;

    //used only for flash player
    private TreeItem currentItem;

    public void setReadOnly(boolean readOnly) {
        this.readOnly = readOnly;
        setDividerSize(this.readOnly ? 0 : dividerSize);
        if (readOnly) {
            parametersPanel.setVisible(false);
        }
    }

    public PreviewPanel(MainPanel mainPanel, FlashPlayerPanel flashPanel) {
        super(JSplitPane.HORIZONTAL_SPLIT, Configuration.guiPreviewSplitPaneDividerLocationPercent);
        this.mainPanel = mainPanel;
        this.flashPanel = flashPanel;

        viewerCards = new JPanel();
        viewerCards.setLayout(new CardLayout());

        viewerCards.add(createFlashPlayerPanel(flashPanel), FLASH_VIEWER_CARD);
        viewerCards.add(createImagesCard(), DRAW_PREVIEW_CARD);
        viewerCards.add(createBinaryCard(), BINARY_TAG_CARD);
        viewerCards.add(createUnknownCard(), UNKNOWN_TAG_CARD);
        viewerCards.add(createMetadataCard(), METADATA_TAG_CARD);
        viewerCards.add(createGenericTagCard(), GENERIC_TAG_CARD);
        viewerCards.add(createPlaceTagCard(), PLACE_TAG_CARD);
        viewerCards.add(createEmptyCard(), EMPTY_CARD);
        setLeftComponent(viewerCards);

        createParametersPanel();

        showCardLeft(FLASH_VIEWER_CARD);

        dividerSize = getDividerSize();
    }

    private JPanel createEmptyCard() {
        JPanel ret = new JPanel();
        ret.add(new JLabel("-"));
        return ret;
    }

    private void createParametersPanel() {
        displayWithPreview = new JPanel(new CardLayout());

        textPanel = new TextPanel(mainPanel);
        displayWithPreview.add(textPanel, CARDTEXTPANEL);

        fontPanel = new FontPanel(mainPanel);
        displayWithPreview.add(fontPanel, CARDFONTPANEL);

        JLabel paramsLabel = new HeaderLabel(mainPanel.translate("parameters"));
        paramsLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //paramsLabel.setBorder(new BevelBorder(BevelBorder.RAISED));

        parametersPanel = new JPanel(new BorderLayout());
        parametersPanel.add(paramsLabel, BorderLayout.NORTH);
        parametersPanel.add(displayWithPreview, BorderLayout.CENTER);
        setRightComponent(parametersPanel);
    }
    
    private JPanel createImageButtonsPanel() {
        replaceShapeButton = new JButton(mainPanel.translate("button.replace"), View.getIcon("replaceshape16"));
        replaceShapeButton.setMargin(new Insets(3, 3, 3, 10));
        replaceShapeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.replaceButtonActionPerformed(mainPanel.getCurrentTree().getSelected());
            }
        });
        replaceShapeButton.setVisible(false);
        
        replaceShapeUpdateBoundsButton = new JButton(mainPanel.translate("button.replaceNoFill"), View.getIcon("replaceshape16"));
        replaceShapeUpdateBoundsButton.setMargin(new Insets(3, 3, 3, 10));
        replaceShapeUpdateBoundsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.replaceNoFillButtonActionPerformed(mainPanel.getCurrentTree().getCurrentTreeItem());
            }
        });
        replaceShapeUpdateBoundsButton.setVisible(false);
        
        replaceSoundButton = new JButton(mainPanel.translate("button.replace"), View.getIcon("replacesound16"));
        replaceSoundButton.setMargin(new Insets(3, 3, 3, 10));
        replaceSoundButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.replaceButtonActionPerformed(mainPanel.getCurrentTree().getSelected());
            }
        });
        replaceSoundButton.setVisible(false);
        
        replaceImageButton = new JButton(mainPanel.translate("button.replace"), View.getIcon("replaceimage16"));
        replaceImageButton.setMargin(new Insets(3, 3, 3, 10));
        replaceImageButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.replaceButtonActionPerformed(mainPanel.getCurrentTree().getSelected());
            }
        });
        replaceImageButton.setVisible(false);

        replaceImageAlphaButton = new JButton(mainPanel.translate("button.replaceAlphaChannel"), View.getIcon("replacealpha16"));
        replaceImageAlphaButton.setMargin(new Insets(3, 3, 3, 10));
        replaceImageAlphaButton.addActionListener(mainPanel::replaceAlphaButtonActionPerformed);
        replaceImageAlphaButton.setVisible(false);

        prevFontsButton = new JButton(mainPanel.translate("button.prev"), View.getIcon("prev16"));
        prevFontsButton.setMargin(new Insets(3, 3, 3, 10));
        prevFontsButton.addActionListener(this::prevFontsButtonActionPerformed);
        prevFontsButton.setVisible(false);

        nextFontsButton = new JButton(mainPanel.translate("button.next"), View.getIcon("next16"));
        nextFontsButton.setMargin(new Insets(3, 3, 3, 10));
        nextFontsButton.addActionListener(this::nextFontsButtonActionPerformed);
        nextFontsButton.setVisible(false);

        ButtonsPanel imageButtonsPanel = new ButtonsPanel();
        imageButtonsPanel.add(replaceSoundButton);
        imageButtonsPanel.add(replaceShapeButton);
        imageButtonsPanel.add(replaceShapeUpdateBoundsButton);
        imageButtonsPanel.add(replaceImageButton);
        imageButtonsPanel.add(replaceImageAlphaButton);
        imageButtonsPanel.add(prevFontsButton);
        imageButtonsPanel.add(nextFontsButton);
        return imageButtonsPanel;
    }

    private JPanel createBinaryButtonsPanel() {
        replaceBinaryButton = new JButton(mainPanel.translate("button.replace"), View.getIcon("edit16"));
        replaceBinaryButton.setMargin(new Insets(3, 3, 3, 10));
        replaceBinaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.replaceButtonActionPerformed(mainPanel.getCurrentTree().getSelected());
            }
        });

        ButtonsPanel binaryButtonsPanel = new ButtonsPanel();
        binaryButtonsPanel.add(replaceBinaryButton);
        return binaryButtonsPanel;
    }

    private JPanel createUnknownButtonsPanel() {
        replaceUnknownButton = new JButton(mainPanel.translate("button.replace"), View.getIcon("edit16"));
        replaceUnknownButton.setMargin(new Insets(3, 3, 3, 10));
        replaceUnknownButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mainPanel.replaceButtonActionPerformed(mainPanel.getCurrentTree().getSelected());
            }
        });

        ButtonsPanel unknownButtonsPanel = new ButtonsPanel();
        unknownButtonsPanel.add(replaceBinaryButton);
        return unknownButtonsPanel;
    }

    private JPanel createGenericTagButtonsPanel() {
        genericEditButton = new JButton(mainPanel.translate("button.edit"), View.getIcon("edit16"));
        genericEditButton.setMargin(new Insets(3, 3, 3, 10));
        genericEditButton.addActionListener(this::editGenericTagButtonActionPerformed);
        genericSaveButton = new JButton(mainPanel.translate("button.save"), View.getIcon("save16"));
        genericSaveButton.setMargin(new Insets(3, 3, 3, 10));
        genericSaveButton.addActionListener(this::saveGenericTagButtonActionPerformed);
        genericSaveButton.setVisible(false);
        genericCancelButton = new JButton(mainPanel.translate("button.cancel"), View.getIcon("cancel16"));
        genericCancelButton.setMargin(new Insets(3, 3, 3, 10));
        genericCancelButton.addActionListener(this::cancelGenericTagButtonActionPerformed);
        genericCancelButton.setVisible(false);

        ButtonsPanel genericTagButtonsPanel = new ButtonsPanel();
        genericTagButtonsPanel.add(genericEditButton);
        genericTagButtonsPanel.add(genericSaveButton);
        genericTagButtonsPanel.add(genericCancelButton);
        return genericTagButtonsPanel;
    }

    private JPanel createMetadataButtonsPanel() {
        metadataEditButton = new JButton(mainPanel.translate("button.edit"), View.getIcon("edit16"));
        metadataEditButton.setMargin(new Insets(3, 3, 3, 10));
        metadataEditButton.addActionListener(this::editMetadataButtonActionPerformed);
        metadataSaveButton = new JButton(mainPanel.translate("button.save"), View.getIcon("save16"));
        metadataSaveButton.setMargin(new Insets(3, 3, 3, 10));
        metadataSaveButton.addActionListener(this::saveMetadataButtonActionPerformed);
        metadataSaveButton.setVisible(false);
        metadataCancelButton = new JButton(mainPanel.translate("button.cancel"), View.getIcon("cancel16"));
        metadataCancelButton.setMargin(new Insets(3, 3, 3, 10));
        metadataCancelButton.addActionListener(this::cancelMetadataButtonActionPerformed);
        metadataCancelButton.setVisible(false);

        ButtonsPanel metadataTagButtonsPanel = new ButtonsPanel();
        metadataTagButtonsPanel.add(metadataEditButton);
        metadataTagButtonsPanel.add(metadataSaveButton);
        metadataTagButtonsPanel.add(metadataCancelButton);
        return metadataTagButtonsPanel;
    }

    private JPanel createFlashPlayerPanel(FlashPlayerPanel flashPanel) {
        JPanel pan = new JPanel(new BorderLayout());
        JLabel prevLabel = new HeaderLabel(mainPanel.translate("swfpreview"));
        prevLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //prevLabel.setBorder(new BevelBorder(BevelBorder.RAISED));

        pan.add(prevLabel, BorderLayout.NORTH);

        Component leftComponent;
        if (flashPanel != null) {
            JPanel flashPlayPanel = new JPanel(new BorderLayout());
            flashPlayPanel.add(flashPanel, BorderLayout.CENTER);

            JPanel flashPlayPanel2 = new JPanel(new BorderLayout());
            flashPlayPanel2.add(flashPlayPanel, BorderLayout.CENTER);
            flashPlayPanel2.add(new PlayerControls(mainPanel, flashPanel), BorderLayout.SOUTH);
            leftComponent = flashPlayPanel2;
        } else {
            JPanel swtPanel = new JPanel(new GridBagLayout());
            /*String labelStr = "";
            if (!Platform.isWindows()) {
                labelStr = mainPanel.translate("notavailonthisplatform");
            } else {
                if (Configuration.useAdobeFlashPlayerForPreviews.get()) {
                    labelStr = mainPanel.translate("notavailable.activex") + "\n" + mainPanel.translate("notavailable.activex.disable");
                } else {
                    labelStr = mainPanel.translate("notavailable.internalviewer");
                }
            }
            String htmlLabelStr = "<html><center>" + labelStr.replace("\n", "<br>") + "</center></html>";
            swtPanel.add(new JLabel(htmlLabelStr, JLabel.CENTER), BorderLayout.CENTER);
            swtPanel.setBackground(View.getDefaultBackgroundColor());*/

            JPanel buttonsPanel = new JPanel(new FlowLayout());
            JButton flashProjectorButton = new JButton(mainPanel.translate("button.showin.flashprojector"));
            flashProjectorButton.addActionListener(this::flashProjectorActionPerformed);
            buttonsPanel.add(flashProjectorButton);
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.anchor = GridBagConstraints.CENTER;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            swtPanel.add(buttonsPanel, gbc);

            leftComponent = swtPanel;
        }

        pan.add(leftComponent, BorderLayout.CENTER);
        return pan;
    }

    private void flashProjectorActionPerformed(ActionEvent e) {
        createAndRunTempSwf(currentItem);
    }

    private JPanel createImagesCard() {
        JPanel shapesCard = new JPanel(new BorderLayout());
        JPanel previewPanel = new JPanel(new BorderLayout());

        JPanel previewCnt = new JPanel(new BorderLayout());
        imagePanel = new ImagePanel();
        imagePanel.setLoop(Configuration.loopMedia.get());
        previewCnt.add(imagePanel, BorderLayout.CENTER);
        previewCnt.add(imagePlayControls = new PlayerControls(mainPanel, imagePanel), BorderLayout.SOUTH);
        imagePlayControls.setMedia(imagePanel);
        previewPanel.add(previewCnt, BorderLayout.CENTER);
        JLabel prevIntLabel = new HeaderLabel(mainPanel.translate("swfpreview.internal"));
        prevIntLabel.setHorizontalAlignment(SwingConstants.CENTER);
        //prevIntLabel.setBorder(new BevelBorder(BevelBorder.RAISED));
        previewPanel.add(prevIntLabel, BorderLayout.NORTH);

        shapesCard.add(previewPanel, BorderLayout.CENTER);

        shapesCard.add(createImageButtonsPanel(), BorderLayout.SOUTH);
        return shapesCard;
    }

    private JPanel createMetadataCard() {
        JPanel metadataCard = new JPanel(new BorderLayout());
        metadataEditor = new LineMarkedEditorPane();
        metadataCard.add(new FasterScrollPane(metadataEditor), BorderLayout.CENTER);
        //metadataEditor.setContentType("text/xml");
        metadataEditor.setEditable(false);

        metadataEditor.setFont(Configuration.getSourceFont());
        metadataEditor.changeContentType("text/xml");
        metadataEditor.addTextChangedListener(this::metadataTextChanged);

        metadataCard.add(createMetadataButtonsPanel(), BorderLayout.SOUTH);
        return metadataCard;
    }

    private boolean isMetadataModified() {
        return metadataSaveButton.isVisible() && metadataSaveButton.isEnabled();
    }

    private void setMetadataModified(boolean value) {
        metadataSaveButton.setEnabled(value);
        metadataCancelButton.setEnabled(value);
    }

    private void metadataTextChanged() {
        setMetadataModified(true);
    }

    private void updateMetadataButtonsVisibility() {
        boolean edit = metadataEditor.isEditable();
        boolean editorMode = Configuration.editorMode.get();
        metadataEditButton.setVisible(!readOnly && !edit);
        metadataSaveButton.setVisible(!readOnly && edit);
        boolean metadataModified = isMetadataModified();
        metadataCancelButton.setVisible(!readOnly && edit);
        metadataCancelButton.setEnabled(metadataModified || !editorMode);
    }

    private JPanel createBinaryCard() {
        JPanel binaryCard = new JPanel(new BorderLayout());
        binaryPanel = new BinaryPanel(mainPanel);
        binaryCard.add(binaryPanel, BorderLayout.CENTER);
        binaryCard.add(createBinaryButtonsPanel(), BorderLayout.SOUTH);
        return binaryCard;
    }

    private JPanel createUnknownCard() {
        JPanel unknownCard = new JPanel(new BorderLayout());
        unknownHexView = new HexView();
        unknownCard.add(unknownHexView, BorderLayout.CENTER);
        unknownCard.add(createUnknownButtonsPanel(), BorderLayout.SOUTH);
        return unknownCard;
    }

    private JPanel createGenericTagCard() {
        JPanel genericTagCard = new JPanel(new BorderLayout());
        genericTagPanel = new GenericTagTreePanel(mainPanel);
        genericTagCard.add(genericTagPanel, BorderLayout.CENTER);
        genericTagCard.add(createGenericTagButtonsPanel(), BorderLayout.SOUTH);
        return genericTagCard;
    }

    private JPanel createPlaceTagCard() {
        JPanel placeTagCard = new JPanel(new BorderLayout());

        JPanel previewPanel = new JPanel(new BorderLayout());

        JPanel previewCnt = new JPanel(new BorderLayout());
        placeImagePanel = new ImagePanel();
        //imagePanel.setLoop(Configuration.loopMedia.get());
        previewCnt.add(placeImagePanel, BorderLayout.CENTER);
        PlayerControls placeImagePlayControls = new PlayerControls(mainPanel, placeImagePanel);
        previewCnt.add(placeImagePlayControls, BorderLayout.SOUTH);
        placeImagePlayControls.setMedia(placeImagePanel);
        previewPanel.add(previewCnt, BorderLayout.CENTER);
        JLabel prevIntLabel = new HeaderLabel(mainPanel.translate("swfpreview.internal"));
        prevIntLabel.setHorizontalAlignment(SwingConstants.CENTER);
        previewPanel.add(prevIntLabel, BorderLayout.NORTH);

        placeGenericPanel = new GenericTagTreePanel(mainPanel);
        placeSplitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, previewPanel, placeGenericPanel);

        placeTagCard.add(placeSplitPane, BorderLayout.CENTER);
        //placeSplitPane.setDividerLocation(800);
        placeTagCard.add(createPlaceTagButtonsPanel(), BorderLayout.SOUTH);

        return placeTagCard;
    }

    private JPanel createPlaceTagButtonsPanel() {
        placeFreeTransformButton = new JButton(mainPanel.translate("button.freetransform"), View.getIcon("freetransform16"));
        placeFreeTransformButton.setMargin(new Insets(3, 3, 3, 10));
        placeFreeTransformButton.addActionListener(this::freeTransformPlaceTagButtonActionPerformed);
        placeEditButton = new JButton(mainPanel.translate("button.edit"), View.getIcon("edit16"));
        placeEditButton.setMargin(new Insets(3, 3, 3, 10));
        placeEditButton.addActionListener(this::editPlaceTagButtonActionPerformed);
        placeSaveButton = new JButton(mainPanel.translate("button.save"), View.getIcon("save16"));
        placeSaveButton.setMargin(new Insets(3, 3, 3, 10));
        placeSaveButton.addActionListener(this::savePlaceTagButtonActionPerformed);
        placeSaveButton.setVisible(false);
        placeCancelButton = new JButton(mainPanel.translate("button.cancel"), View.getIcon("cancel16"));
        placeCancelButton.setMargin(new Insets(3, 3, 3, 10));
        placeCancelButton.addActionListener(this::cancelPlaceTagButtonActionPerformed);
        placeCancelButton.setVisible(false);

        ButtonsPanel placeTagButtonsPanel = new ButtonsPanel();
        placeTagButtonsPanel.add(placeFreeTransformButton);
        placeTagButtonsPanel.add(placeEditButton);
        placeTagButtonsPanel.add(placeSaveButton);
        placeTagButtonsPanel.add(placeCancelButton);
        return placeTagButtonsPanel;
    }

    private void showCardLeft(String card) {
        CardLayout cl = (CardLayout) (viewerCards.getLayout());
        cl.show(viewerCards, card);

        //stop sounds when switching panels
        imagePanel.stop();
        placeImagePanel.stop();
    }

    private void showCardRight(String card) {
        CardLayout cl = (CardLayout) (displayWithPreview.getLayout());
        cl.show(displayWithPreview, card);
    }

    public TextPanel getTextPanel() {
        return textPanel;
    }

    public void setParametersPanelVisible(boolean show) {
        parametersPanel.setVisible(show);
    }

    public void showFlashViewerPanel() {
        parametersPanel.setVisible(false);
        showCardLeft(FLASH_VIEWER_CARD);
    }

    public void showImagePanel(Timelined timelined, SWF swf, int frame, boolean showObjectsUnderCursor, boolean autoPlay, boolean frozen, boolean alwaysDisplay) {
        showCardLeft(DRAW_PREVIEW_CARD);
        parametersPanel.setVisible(false);
        imagePlayControls.setMedia(imagePanel);
        imagePanel.setTimelined(timelined, swf, frame, showObjectsUnderCursor, autoPlay, frozen, alwaysDisplay);
    }

    public void showImagePanel(SerializableImage image) {
        showCardLeft(DRAW_PREVIEW_CARD);
        parametersPanel.setVisible(false);
        imagePlayControls.setMedia(imagePanel);
        imagePanel.setImage(image);
    }

    public void showTextComparePanel(TextTag textTag, TextTag newTextTag) {
        imagePanel.setText(textTag, newTextTag);
    }

    public void setMedia(MediaDisplay media) {
        this.media = media;
        imagePlayControls.setMedia(media);
    }

    public void showFontPanel(FontTag fontTag) {
        fontPageNum = 0;
        showFontPage(fontTag);

        showCardRight(CARDFONTPANEL);
        if (!readOnly) {
            parametersPanel.setVisible(true);
        }
        fontPanel.showFontTag(fontTag);

        int pageCount = getFontPageCount(fontTag);
        if (pageCount > 1) {
            prevFontsButton.setVisible(true);
            nextFontsButton.setVisible(true);
        }
    }

    private void showFontPage(FontTag fontTag) {
        if (!mainPanel.isAdobeFlashPlayerEnabled() /*|| ft instanceof GFxDefineCompactedFont*/) {
            showImagePanel(MainPanel.makeTimelined(fontTag), fontTag.getSwf(), fontPageNum, true, true, true, true);
        }
    }

    public static int getFontPageCount(FontTag fontTag) {
        int pageCount = (fontTag.getGlyphShapeTable().size() - 1) / SHAPERECORD.MAX_CHARACTERS_IN_FONT_PREVIEW + 1;
        if (pageCount < 1) {
            pageCount = 1;
        }
        return pageCount;
    }

    public void showEmpty() {
        showCardLeft(EMPTY_CARD);
    }

    public void showTextPanel(TextTag textTag) {
        if (!mainPanel.isAdobeFlashPlayerEnabled() /*|| ft instanceof GFxDefineCompactedFont*/) {
            showImagePanel(MainPanel.makeTimelined(textTag), textTag.getSwf(), 0, true, true, true, true);
        }

        showCardRight(CARDTEXTPANEL);
        if (!readOnly) {
            parametersPanel.setVisible(true);
        }
        textPanel.setText(textTag);
    }

    public void focusTextPanel() {
        textPanel.focusTextValue();
    }

    public void clear() {
        imagePanel.clearAll();
        if (media != null) {
            try {
                media.close();
            } catch (IOException ex) {
                // ignore
            }
        }

        binaryPanel.setBinaryData(null);
        genericTagPanel.clear();
        fontPanel.clear();
    }

    public void closeTag() {
        textPanel.closeTag();
    }

    public static String formatMetadata(String input, int indent) {
        input = input.replace("> <", "><");
        try {
            Source xmlInput = new StreamSource(new StringReader(input));
            StringWriter stringWriter = new StringWriter();
            StreamResult xmlOutput = new StreamResult(stringWriter);
            StringWriter sw = new StringWriter();
            xmlOutput.setWriter(sw);
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", indent);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "" + indent);
            transformer.transform(xmlInput, xmlOutput);

            return xmlOutput.getWriter().toString();
        } catch (IllegalArgumentException | TransformerException e) {
            return input;
        }
    }

    public void showMetaDataPanel(MetadataTag metadataTag) {
        showCardLeft(METADATA_TAG_CARD);
        this.metadataTag = metadataTag;
        metadataEditor.setEditable(!readOnly && !metadataTag.isReadOnly() && Configuration.editorMode.get());
        metadataEditor.setText(formatMetadata(metadataTag.xmlMetadata, 4));
        setMetadataModified(false);
        updateMetadataButtonsVisibility();
        parametersPanel.setVisible(false);
    }

    public void showBinaryPanel(DefineBinaryDataTag binaryDataTag) {
        showCardLeft(BINARY_TAG_CARD);
        binaryPanel.setBinaryData(binaryDataTag);
        parametersPanel.setVisible(false);
    }

    public void showUnknownPanel(UnknownTag unknownTag) {
        showCardLeft(UNKNOWN_TAG_CARD);
        if (unknownTag == null) {
            unknownHexView.setData(new byte[0], null, null);
        } else {
            unknownHexView.setData(unknownTag.unknownData.getRangeData(), null, null);
        }
        unknownHexView.repaint();
        parametersPanel.setVisible(false);
    }

    public void showGenericTagPanel(Tag tag) {
        showCardLeft(GENERIC_TAG_CARD);
        genericEditButton.setVisible(!tag.isReadOnly());
        genericEditButton.setEnabled(true);
        genericSaveButton.setVisible(false);
        genericCancelButton.setVisible(false);
        genericTagPanel.setEditMode(false, tag);
        parametersPanel.setVisible(false);
    }

    public void showPlaceTagPanel(PlaceObjectTypeTag tag, int frame) {
        showCardLeft(PLACE_TAG_CARD);
        placeTag = tag;
        oldMatrix = tag.getMatrix();
        placeSplitPane.setDividerLocation((int) (0.6 * this.getWidth()));        
        placeGenericPanel.setVisible(!readOnly);
        placeGenericPanel.setEditMode(false, tag);
        placeImagePanel.selectDepth(-1);
        placeImagePanel.setTimelined(((Tag) tag).getTimelined(), ((Tag) tag).getSwf(), frame, true, Configuration.autoPlayPreviews.get(), !Configuration.animateSubsprites.get(), false);
        placeImagePanel.selectDepth(tag.getDepth());
        parametersPanel.setVisible(false);
        placeEditButton.setVisible(!tag.isReadOnly() && !readOnly);
        placeEditButton.setEnabled(true);
        placeSaveButton.setVisible(false);
        placeCancelButton.setVisible(false);
        placeFreeTransformButton.setVisible(!readOnly);
    }

    public void setImageReplaceButtonVisible(boolean showImage, boolean showAlpha, boolean showShape, boolean showSound) {
        if (readOnly) {
            showImage = false;
            showAlpha = false;
            showShape = false;
            showSound = false;
        }
        replaceImageButton.setVisible(showImage);
        replaceImageAlphaButton.setVisible(showAlpha);
        replaceShapeButton.setVisible(showShape);
        replaceShapeUpdateBoundsButton.setVisible(showShape);
        replaceSoundButton.setVisible(showSound);
        prevFontsButton.setVisible(false);
        nextFontsButton.setVisible(false);
    }
    
    private void createAndRunTempSwf(TreeItem treeItem) {
        try {
            File extTempFile = File.createTempFile("ffdec_viewext_", ".swf");
            extTempFile.deleteOnExit();

            if (treeItem instanceof SWF) {
                SWF swf = (SWF) treeItem;
                try ( FileOutputStream fos = new FileOutputStream(extTempFile)) {
                    swf.saveTo(fos);
                }
            } else {
                Color backgroundColor = View.getSwfBackgroundColor();

                if (treeItem instanceof Tag) {
                    Tag tag = (Tag) treeItem;
                    if (tag instanceof FontTag) { //Fonts are always black on white
                        backgroundColor = View.getDefaultBackgroundColor();
                    }
                } else if (treeItem instanceof Frame) {
                    Frame fn = (Frame) treeItem;
                    SWF sourceSwf = (SWF) fn.getOpenable();
                    if (fn.timeline.timelined == sourceSwf) {
                        SetBackgroundColorTag setBgColorTag = sourceSwf.getBackgroundColor();
                        if (setBgColorTag != null) {
                            backgroundColor = setBgColorTag.backgroundColor.toColor();
                        }
                    }
                }

                SWFHeader header;
                try ( OutputStream fos = new BufferedOutputStream(new FileOutputStream(extTempFile))) {
                    header = new PreviewExporter().exportSwf(fos, treeItem, backgroundColor, fontPageNum, true);
                }
            }
            Main.runAsync(extTempFile);
        } catch (IOException | ActionParseException ex) {
            Logger.getLogger(PreviewPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void createAndShowTempSwf(TreeItem treeItem) {
        try {
            if (tempFile != null) {
                tempFile.delete();
            }

            tempFile = File.createTempFile("ffdec_view_", ".swf");
            tempFile.deleteOnExit();

            Color backgroundColor = View.getSwfBackgroundColor();

            if (treeItem instanceof Tag) {
                Tag tag = (Tag) treeItem;
                if (tag instanceof FontTag) { //Fonts are always black on white
                    backgroundColor = View.getDefaultBackgroundColor();
                }
            } else if (treeItem instanceof Frame) {
                Frame fn = (Frame) treeItem;
                SWF sourceSwf = (SWF) fn.getOpenable();
                if (fn.timeline.timelined == sourceSwf) {
                    SetBackgroundColorTag setBgColorTag = sourceSwf.getBackgroundColor();
                    if (setBgColorTag != null) {
                        backgroundColor = setBgColorTag.backgroundColor.toColor();
                    }
                }
            }

            SWFHeader header;
            try ( OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                header = new PreviewExporter().exportSwf(fos, treeItem, backgroundColor, fontPageNum, false);
            }

            if (flashPanel != null) {
                flashPanel.displaySWF(tempFile.getAbsolutePath(), backgroundColor, header.frameRate);
            }

            this.currentItem = treeItem;

            showFlashViewerPanel();
        } catch (IOException | ActionParseException ex) {
            Logger.getLogger(PreviewPanel.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public void showSwf(SWF swf) {
        currentItem = swf;
        if (flashPanel == null) {
            return;
        }
        Color backgroundColor = View.getDefaultBackgroundColor();
        SetBackgroundColorTag setBgColorTag = swf.getBackgroundColor();
        if (setBgColorTag != null) {
            backgroundColor = setBgColorTag.backgroundColor.toColor();
        }

        if (tempFile != null) {
            tempFile.delete();
        }
        try {
            tempFile = File.createTempFile("ffdec_view_", ".swf");
            try ( OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                swf.saveTo(fos, false);
            }
            //Inject Loader
            if (swf.isAS3() && Configuration.autoOpenLoadedSWFs.get() && Configuration.useAdobeFlashPlayerForPreviews.get() && !DebuggerTools.hasDebugger(swf)) {
                SWF instrSWF;
                try ( InputStream fis = new BufferedInputStream(new FileInputStream(tempFile))) {
                    instrSWF = new SWF(fis, false, false);
                }

                DebuggerTools.switchDebugger(instrSWF);
                DebuggerTools.injectDebugLoader(instrSWF);
                try ( OutputStream fos = new BufferedOutputStream(new FileOutputStream(tempFile))) {
                    instrSWF.saveTo(fos);
                }
            }
            flashPanel.displaySWF(tempFile.getAbsolutePath(), backgroundColor, swf.frameRate);
        } catch (IOException iex) {
            Logger.getLogger(PreviewPanel.class.getName()).log(Level.SEVERE, "Cannot create tempfile", iex);
        } catch (InterruptedException ex) {

        }
    }

    private void editMetadataButtonActionPerformed(ActionEvent evt) {
        TreeItem item = mainPanel.getCurrentTree().getCurrentTreeItem();
        if (item == null) {
            return;
        }

        if (item instanceof MetadataTag) {
            metadataEditor.setEditable(true);
            updateMetadataButtonsVisibility();
        }
    }

    private void saveMetadataButtonActionPerformed(ActionEvent evt) {
        metadataTag.xmlMetadata = metadataEditor.getText().replaceAll(">\r?\n<", "> <");
        metadataTag.setModified(true);
        metadataEditor.setEditable(Configuration.editorMode.get());
        setMetadataModified(false);
        updateMetadataButtonsVisibility();
        mainPanel.repaintTree();
    }

    private void cancelMetadataButtonActionPerformed(ActionEvent evt) {
        metadataEditor.setEditable(false);
        metadataEditor.setText(formatMetadata(metadataTag.xmlMetadata, 4));
        metadataEditor.setEditable(Configuration.editorMode.get());
        setMetadataModified(false);
        updateMetadataButtonsVisibility();
    }

    private void editGenericTagButtonActionPerformed(ActionEvent evt) {
        TreeItem item = mainPanel.getCurrentTree().getCurrentTreeItem();
        if (item == null) {
            return;
        }

        if (item instanceof TagScript) {
            item = ((TagScript) item).getTag();
        }

        if (item instanceof Tag) {
            genericEditButton.setVisible(false);
            genericSaveButton.setVisible(true);
            genericCancelButton.setVisible(true);
            genericTagPanel.setEditMode(true, (Tag) item);
        }
    }

    private void saveGenericTagButtonActionPerformed(ActionEvent evt) {
        if (genericTagPanel.save()) {
            Tag tag = genericTagPanel.getTag();
            SWF swf = tag.getSwf();
            swf.clearImageCache();
            swf.clearShapeCache();
            swf.updateCharacters();
            tag.getTimelined().resetTimeline();
            swf.assignClassesToSymbols();
            swf.assignExportNamesToSymbols();
            mainPanel.refreshTree(swf);
            mainPanel.setTagTreeSelectedNode(mainPanel.getCurrentTree(), tag);
            genericEditButton.setVisible(true);
            genericSaveButton.setVisible(false);
            genericCancelButton.setVisible(false);
            genericTagPanel.setEditMode(false, null);
        }
    }

    private void cancelGenericTagButtonActionPerformed(ActionEvent evt) {
        genericEditButton.setVisible(true);
        genericSaveButton.setVisible(false);
        genericCancelButton.setVisible(false);
        genericTagPanel.setEditMode(false, null);
    }

    private void savePlaceTagButtonActionPerformed(ActionEvent evt) {
        if (placeEditMode == PLACE_EDIT_FREETRANSFORM) {
            MATRIX matrix = placeImagePanel.getNewMatrix();
            placeTag.setPlaceFlagHasMatrix(true);
            placeTag.setMatrix(matrix);
            placeTag.setModified(true);
            placeImagePanel.selectDepth(placeTag.getDepth());
            placeImagePanel.freeTransformDepth(-1);
            placeTag.getTimelined().resetTimeline();
        }
        if (placeEditMode == PLACE_EDIT_RAW) {
            if (placeGenericPanel.save()) {
                Tag tag = placeGenericPanel.getTag();
                SWF swf = tag.getSwf();
                tag.getTimelined().resetTimeline();
                mainPanel.refreshTree(swf);
                mainPanel.setTagTreeSelectedNode(mainPanel.getCurrentTree(), tag);
            }
            placeGenericPanel.setEditMode(false, null);
        }
        placeFreeTransformButton.setVisible(true);
        placeEditButton.setVisible(true);
        placeSaveButton.setVisible(false);
        placeCancelButton.setVisible(false);
        mainPanel.repaintTree();
    }

    private void editPlaceTagButtonActionPerformed(ActionEvent evt) {
        placeEditMode = PLACE_EDIT_RAW;
        placeGenericPanel.setEditMode(true, placeTag);
        placeEditButton.setVisible(false);
        placeFreeTransformButton.setVisible(false);
        placeSaveButton.setVisible(true);
        placeCancelButton.setVisible(true);
    }

    private void freeTransformPlaceTagButtonActionPerformed(ActionEvent evt) {
        TreeItem item = mainPanel.getCurrentTree().getCurrentTreeItem();
        if (item == null) {
            return;
        }
        placeEditMode = PLACE_EDIT_FREETRANSFORM;
        placeImagePanel.selectDepth(-1);
        placeImagePanel.freeTransformDepth(placeTag.getDepth());
        placeEditButton.setVisible(false);
        placeFreeTransformButton.setVisible(false);
        placeSaveButton.setVisible(true);
        placeCancelButton.setVisible(true);
    }

    private void cancelPlaceTagButtonActionPerformed(ActionEvent evt) {
        if (placeEditMode == PLACE_EDIT_FREETRANSFORM) {
            placeImagePanel.selectDepth(placeTag.getDepth());
            placeImagePanel.freeTransformDepth(-1);
            placeTag.setMatrix(oldMatrix);
            placeTag.getTimelined().resetTimeline();
        }
        if (placeEditMode == PLACE_EDIT_RAW) {
            placeGenericPanel.setEditMode(false, null);
        }
        placeEditButton.setVisible(true);
        placeFreeTransformButton.setVisible(true);
        placeSaveButton.setVisible(false);
        placeCancelButton.setVisible(false);
    }

    private void prevFontsButtonActionPerformed(ActionEvent evt) {
        FontTag fontTag = fontPanel.getFontTag();
        int pageCount = getFontPageCount(fontTag);
        fontPageNum = (fontPageNum + pageCount - 1) % pageCount;
        if (!mainPanel.isAdobeFlashPlayerEnabled() /*|| ft instanceof GFxDefineCompactedFont*/) {
            imagePanel.setTimelined(MainPanel.makeTimelined(fontTag, fontPageNum), fontTag.getSwf(), 0, true, true, true, true);
        }
    }

    private void nextFontsButtonActionPerformed(ActionEvent evt) {
        FontTag fontTag = fontPanel.getFontTag();
        int pageCount = getFontPageCount(fontTag);
        fontPageNum = (fontPageNum + 1) % pageCount;
        if (!mainPanel.isAdobeFlashPlayerEnabled() /*|| ft instanceof GFxDefineCompactedFont*/) {
            imagePanel.setTimelined(MainPanel.makeTimelined(fontTag, fontPageNum), fontTag.getSwf(), 0, true, true, true, true);
        }
    }

    @Override
    public boolean tryAutoSave() {
        // todo: implement
        return textPanel.tryAutoSave() && false;
    }

    @Override
    public boolean isEditing() {
        return textPanel.isEditing()
                || (genericSaveButton.isVisible() && genericSaveButton.isEnabled())
                || (metadataSaveButton.isVisible() && metadataSaveButton.isEnabled());
    }

    public void selectImageDepth(int depth) {
        imagePanel.selectDepth(depth);
    }
}
