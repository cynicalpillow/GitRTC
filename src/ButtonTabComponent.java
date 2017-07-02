/*
 * Copyright (c) 1995, 2008, Oracle and/or its affiliates. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *   - Redistributions of source code must retain the above copyright
 *     notice, this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of Oracle or the names of its
 *     contributors may be used to endorse or promote products derived
 *     from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea;
import org.fife.ui.rtextarea.RTextScrollPane;

import javax.swing.*;
import javax.swing.plaf.basic.BasicButtonUI;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Vector;

/**
 * Component to be used as tabComponent;
 * Contains a JLabel to show the text and
 * a JButton to close the tab it belongs to
 */
public class ButtonTabComponent extends JPanel{
    final DnDTabbedPane pane;
    private RTextScrollPane scrollPane;
    private FileInfo file;
    private RSyntaxTextArea textArea;
    private int caretDot = 0;
    private int caretMark = 0;
    private Editor editor;

    public ButtonTabComponent(final DnDTabbedPane pane, Editor editor, RSyntaxTextArea textArea, RTextScrollPane scrollPane, Vector<ButtonTabComponent> tabs) {
        //unset default FlowLayout' gaps
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        if (pane == null) {
            throw new NullPointerException("TabbedPane is null");
        }
        this.pane = pane;
        this.textArea = textArea;
        this.scrollPane = scrollPane;
        this.editor = editor;
        setOpaque(false);

        //make JLabel read titles from JTabbedPane
        JLabel label = new JLabel() {
            public String getText() {
                int i = pane.indexOfTabComponent(ButtonTabComponent.this);
                if (i != -1) {
                    return pane.getTitleAt(i);
                }
                return null;
            }
        };

        add(label);
        //add more space between the label and the button
        label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 5));
        //tab button
        JButton button = new TabButton(editor, this);
        add(button);
        //add more space to the top of the component
        setBorder(BorderFactory.createEmptyBorder(2, 0, 0, 0));
    }

    /*public boolean equals(Object o){
        if(!(o instanceof ButtonTabComponent)) return false;
        ButtonTabComponent b = (ButtonTabComponent) o;
        return b.getFile().getId() == this.file.getId();
    }*/

    public String toString(){
        return file.getTitle();
    }

    public FileInfo getFile() {
        return file;
    }
    public void setFile(FileInfo file) {
        this.file = file;
    }
    public RSyntaxTextArea getTextArea() {
        return textArea;
    }
    public void setTextArea(RSyntaxTextArea textArea) {
        this.textArea = textArea;
    }
    public int getCaretDot() {
        return caretDot;
    }
    public void setCaretDot(int caretDot) {
        this.caretDot = caretDot;
    }
    public int getCaretMark() {
        return caretMark;
    }
    public void setCaretMark(int caretMark) {
        this.caretMark = caretMark;
    }
    public RTextScrollPane getScrollPane() {
        return scrollPane;
    }
    public void setScrollPane(RTextScrollPane scrollPane) {
        this.scrollPane = scrollPane;
    }

    private class TabButton extends JButton implements ActionListener {
        private Editor editor;
        private ButtonTabComponent b;
        public TabButton(Editor editor, ButtonTabComponent b) {
            this.editor = editor;
            this.b = b;
            int size = 17;
            setPreferredSize(new Dimension(size, size));
            setToolTipText("Close this tab");
            //Make the button looks the same for all Laf's
            setUI(new BasicButtonUI());
            //Make it transparent
            setContentAreaFilled(false);
            //No need to be focusable
            setFocusable(false);
            setBorder(BorderFactory.createEtchedBorder());
            setBorderPainted(false);
            //Making nice rollover effect
            //we use the same listener for all buttons
            addMouseListener(buttonMouseListener);
            setRolloverEnabled(true);
            //Close the proper tab by clicking the button
            addActionListener(this);
        }

        public void actionPerformed(ActionEvent e) {
            int i = pane.indexOfTabComponent(ButtonTabComponent.this);
            if (i != -1) {
                int option = 0;
                System.out.println(b.getFile().getOriginalText() + " " + editor.c);
                if(!b.getFile().getOriginalText().equals(b.getTextArea().getText()) && b.getFile().getId() == -1) {
                    if (file.getFile() == null) {
                        option = editor.saveFileClosing(new File(b.getFile().getTitle()), b.getFile().getText());
                    } else {
                        if (!b.getFile().getTitle().equals(b.getFile().getFile().getName())) {
                            option = editor.saveFileClosing(new File(b.getFile().getTitle()), b.getFile().getText());
                        } else {
                            option = editor.saveFileClosing(b.getFile().getFile(), b.getFile().getText());
                        }
                    }
                }
                if(editor.c != null && editor.c.connected) {
                    if (option == 0 || option == 1) {
                        editor.c.sendOnClose(file.getId(), b.getTextArea().getText());
                        pane.remove(i);
                        editor.tabs.remove(i);
                        editor.currentFiles.updateUI();
                    }
                } else {
                    if (option == 0 || option == 1) {
                        pane.remove(i);
                        editor.tabs.remove(i);
                        editor.currentFiles.updateUI();
                    }
                }
            }
        }

        //we don't want to update UI for this button
        public void updateUI() {
        }

        //paint the cross
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g.create();
            //shift the image for pressed buttons
            if (getModel().isPressed()) {
                g2.translate(1, 1);
            }
            g2.setStroke(new BasicStroke(2));
            g2.setColor(Color.BLACK);
            if (getModel().isRollover()) {
                g2.setColor(Color.MAGENTA);
            }
            int delta = 6;
            g2.drawLine(delta, delta, getWidth() - delta - 1, getHeight() - delta - 1);
            g2.drawLine(getWidth() - delta - 1, delta, delta, getHeight() - delta - 1);
            g2.dispose();
        }
    }

    private final static MouseListener buttonMouseListener = new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(true);
            }
        }

        public void mouseExited(MouseEvent e) {
            Component component = e.getComponent();
            if (component instanceof AbstractButton) {
                AbstractButton button = (AbstractButton) component;
                button.setBorderPainted(false);
            }
        }
    };
}

