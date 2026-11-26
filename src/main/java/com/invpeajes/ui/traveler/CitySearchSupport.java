package com.invpeajes.ui.traveler;

import com.invpeajes.ui.common.Theme;
import com.invpeajes.ui.traveler.TravelerUi.City;
import java.awt.AWTEvent;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.AWTEventListener;
import java.awt.event.ActionListener;
import java.awt.event.HierarchyEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractListModel;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxEditor;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.plaf.basic.ComboPopup;

/**
 * Controlador de búsqueda para combos de municipios: alterna entre una tarjeta compacta
 * de dos líneas (municipio en negrita arriba y departamento abajo) cuando no tiene foco
 * y un campo de texto editable para filtrar en tiempo real al hacer clic.
 */
final class CitySearchSupport {

    private static final String CLIENT_KEY = "invpeajes.citySearchSupport";
    private static final String CARD_DISPLAY = "DISPLAY";
    private static final String CARD_EDIT = "EDIT";

    private final JComboBox<City> combo;
    private final CityListModel listModel;
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel editorContainer = new JPanel(cardLayout);
    private final JPanel displayPanel = new JPanel();
    private final JLabel displayCityLabel = new JLabel();
    private final JLabel displayDeptLabel = new JLabel();
    private final JTextField textField = new JTextField();

    private List<City> allCities = new ArrayList<>();
    private City committedCity;
    private boolean editing;
    private boolean updating;
    private boolean filtering;
    private boolean navigatingPopup;
    private AWTEventListener outsideClickListener;

    private CitySearchSupport(JComboBox<City> combo) {
        this.combo = combo;
        if (combo.getModel() instanceof DefaultComboBoxModel) {
            DefaultComboBoxModel<City> initial = (DefaultComboBoxModel<City>) combo.getModel();
            for (int i = 0; i < initial.getSize(); i++) {
                allCities.add(initial.getElementAt(i));
            }
        }
        Object current = combo.getSelectedItem();
        if (current instanceof City) {
            this.committedCity = (City) current;
        }

        this.listModel = new CityListModel(allCities, committedCity);
        combo.setModel(listModel);
        combo.setMaximumRowCount(6);
        combo.setRenderer(new CityCellRenderer());
        combo.setKeySelectionManager((aKey, aModel) -> -1);
        combo.setPreferredSize(new Dimension(140, 44));
        combo.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));

        buildEditorCards();
        combo.setEditor(new DualModeComboEditor());
        combo.setEditable(true);
        refreshDisplayLabels(committedCity);
        showDisplayCard();

        hookPopupSelection();
        hookDocumentFilter();
        hookKeyboard();
        installOutsideClickHandler();
    }

    /** Habilita el modo buscable de dos líneas en el combo indicado. */
    static void install(JComboBox<City> combo) {
        if (combo.getClientProperty(CLIENT_KEY) != null) {
            return;
        }
        combo.putClientProperty(CLIENT_KEY, new CitySearchSupport(combo));
    }

    /** Reemplaza los municipios del combo y selecciona el elemento inicial. */
    static void setItems(JComboBox<City> combo, List<City> items, City defaultItem) {
        Object prop = combo.getClientProperty(CLIENT_KEY);
        if (prop instanceof CitySearchSupport) {
            ((CitySearchSupport) prop).updateItems(items, defaultItem);
            return;
        }
        combo.removeAllItems();
        for (City item : items) {
            combo.addItem(item);
        }
        if (defaultItem != null) {
            combo.setSelectedItem(defaultItem);
        }
    }

    private void updateItems(List<City> items, City defaultItem) {
        updating = true;
        try {
            allCities = new ArrayList<>(items);
            committedCity = defaultItem;
            listModel.setVisibleCities(allCities);
            listModel.setSelectedItem(defaultItem);
            refreshDisplayLabels(defaultItem);
            showDisplayCard();
        } finally {
            updating = false;
        }
    }

    private void buildEditorCards() {
        displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS));
        displayPanel.setBackground(Color.WHITE);
        displayPanel.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 2));
        displayPanel.setCursor(Cursor.getPredefinedCursor(Cursor.TEXT_CURSOR));

        displayCityLabel.setFont(Theme.font(Font.BOLD, 13));
        displayCityLabel.setForeground(Theme.TEXT);
        displayDeptLabel.setFont(Theme.font(Font.PLAIN, 11));
        displayDeptLabel.setForeground(Theme.MUTED);

        displayPanel.add(displayCityLabel);
        displayPanel.add(displayDeptLabel);

        displayPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    enterEditMode(true);
                }
            }
        });

        textField.setFont(Theme.font(Font.PLAIN, 13));
        textField.setForeground(Theme.TEXT);
        textField.setBackground(Color.WHITE);
        textField.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));
        textField.setToolTipText("Escriba para buscar municipio o departamento");

        editorContainer.setOpaque(true);
        editorContainer.setBackground(Color.WHITE);
        editorContainer.add(displayPanel, CARD_DISPLAY);
        editorContainer.add(textField, CARD_EDIT);
    }

    private void enterEditMode(boolean openPopup) {
        if (editing) {
            textField.requestFocusInWindow();
            return;
        }
        editing = true;
        updating = true;
        try {
            String initialText = committedCity != null ? committedCity.getName() : "";
            textField.setText(initialText);
            listModel.setVisibleCities(allCities);
        } finally {
            updating = false;
        }
        cardLayout.show(editorContainer, CARD_EDIT);
        editorContainer.revalidate();
        editorContainer.repaint();

        SwingUtilities.invokeLater(() -> {
            textField.requestFocusInWindow();
            int len = textField.getText().length();
            textField.select(len, len);
            textField.setCaretPosition(len);
            if (openPopup && combo.isShowing() && !allCities.isEmpty() && !combo.isPopupVisible()) {
                combo.setPopupVisible(true);
            }
        });
    }

    private void exitEditMode(boolean applyTypedMatch) {
        if (!editing) {
            showDisplayCard();
            return;
        }
        City resolved = applyTypedMatch ? resolveFromText(textField.getText(), false) : null;
        editing = false;
        if (resolved != null) {
            commitCity(resolved, true);
        } else {
            restoreCommitted();
        }
        showDisplayCard();
    }

    private void showDisplayCard() {
        editing = false;
        cardLayout.show(editorContainer, CARD_DISPLAY);
        editorContainer.revalidate();
        editorContainer.repaint();
    }

    private void refreshDisplayLabels(City city) {
        if (city == null) {
            displayCityLabel.setText("Seleccionar...");
            displayDeptLabel.setText(" ");
            displayPanel.setToolTipText(null);
            combo.setToolTipText(null);
            return;
        }
        displayCityLabel.setText(city.getName());
        displayDeptLabel.setText(departmentSubtitle(city));
        String full = city.toString();
        displayPanel.setToolTipText(full);
        combo.setToolTipText(full);
    }

    private static String departmentSubtitle(City city) {
        String dept = city.getDepartmentName();
        if (dept == null || dept.isBlank()) {
            return " ";
        }
        if (TravelerUi.normalize(city.getName()).startsWith("bogota")
                && TravelerUi.normalize(dept).startsWith("bogota")) {
            return "Distrito Capital";
        }
        return dept;
    }

    private void hookPopupSelection() {
        combo.addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                if (!updating && !filtering && shouldShowAllOnPopup()) {
                    updating = true;
                    try {
                        listModel.setVisibleCities(allCities);
                    } finally {
                        updating = false;
                    }
                }
                resizePopup();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
            }
        });

        Object child = combo.getUI().getAccessibleChild(combo, 0);
        if (child instanceof ComboPopup) {
            JList<?> popupList = ((ComboPopup) child).getList();
            popupList.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseReleased(MouseEvent e) {
                    Object val = popupList.getSelectedValue();
                    if (val instanceof City) {
                        City chosen = (City) val;
                        SwingUtilities.invokeLater(() -> {
                            combo.setPopupVisible(false);
                            commitCity(chosen, true);
                            showDisplayCard();
                            clearFocus();
                        });
                    }
                }
            });
        }
    }

    private void hookDocumentFilter() {
        textField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                scheduleFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }

            private void scheduleFilter() {
                if (updating || filtering || navigatingPopup || !editing) {
                    return;
                }
                String typed = textField.getText();
                SwingUtilities.invokeLater(() -> filterFromEditor(typed));
            }
        });
    }

    private void hookKeyboard() {
        textField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                int code = e.getKeyCode();
                if (code == KeyEvent.VK_UP || code == KeyEvent.VK_DOWN) {
                    navigatingPopup = true;
                    if (!combo.isPopupVisible() && combo.isShowing() && listModel.getSize() > 0) {
                        combo.setPopupVisible(true);
                    }
                    SwingUtilities.invokeLater(() -> navigatingPopup = false);
                } else if (code == KeyEvent.VK_ENTER) {
                    e.consume();
                    confirmWithEnter();
                } else if (code == KeyEvent.VK_ESCAPE) {
                    e.consume();
                    combo.setPopupVisible(false);
                    exitEditMode(false);
                    clearFocus();
                } else if (code == KeyEvent.VK_TAB) {
                    combo.setPopupVisible(false);
                    exitEditMode(true);
                }
            }
        });
    }

    private void confirmWithEnter() {
        City chosen = null;
        Object child = combo.getUI().getAccessibleChild(combo, 0);
        if (child instanceof ComboPopup) {
            Object highlighted = ((ComboPopup) child).getList().getSelectedValue();
            if (highlighted instanceof City) {
                chosen = (City) highlighted;
            }
        }
        if (chosen == null) {
            chosen = resolveFromText(textField.getText(), true);
        }
        combo.setPopupVisible(false);
        if (chosen != null) {
            commitCity(chosen, true);
        } else {
            restoreCommitted();
        }
        showDisplayCard();
        clearFocus();
    }

    private void installOutsideClickHandler() {
        outsideClickListener = event -> {
            if (!(event instanceof MouseEvent) || event.getID() != MouseEvent.MOUSE_PRESSED) {
                return;
            }
            if (!combo.isShowing()) {
                return;
            }
            if (!editing && !textField.isFocusOwner() && !combo.isPopupVisible()) {
                return;
            }
            Object src = event.getSource();
            if (!(src instanceof Component)) {
                return;
            }
            Component clicked = (Component) src;
            if (isInsideComboOrPopup(clicked)) {
                return;
            }

            if (combo.isPopupVisible()) {
                combo.setPopupVisible(false);
            }
            exitEditMode(true);
            if (clicked.isFocusable()) {
                clicked.requestFocusInWindow();
            } else {
                clearFocus();
            }
        };

        combo.addHierarchyListener(e -> {
            if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                if (combo.isDisplayable()) {
                    Toolkit.getDefaultToolkit().addAWTEventListener(
                            outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
                } else {
                    Toolkit.getDefaultToolkit().removeAWTEventListener(outsideClickListener);
                }
            }
        });
        if (combo.isDisplayable()) {
            Toolkit.getDefaultToolkit().addAWTEventListener(
                    outsideClickListener, AWTEvent.MOUSE_EVENT_MASK);
        }
    }

    private static void clearFocus() {
        KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
    }

    private boolean isInsideComboOrPopup(Component target) {
        if (SwingUtilities.isDescendingFrom(target, combo)) {
            return true;
        }
        Object child = combo.getUI().getAccessibleChild(combo, 0);
        if (child instanceof Component && SwingUtilities.isDescendingFrom(target, (Component) child)) {
            return true;
        }
        Window comboWindow = SwingUtilities.getWindowAncestor(combo);
        Window targetWindow = SwingUtilities.getWindowAncestor(target);
        return targetWindow != null && targetWindow != comboWindow && targetWindow.getOwner() == comboWindow;
    }

    private void resizePopup() {
        Object child = combo.getUI().getAccessibleChild(combo, 0);
        if (child instanceof JPopupMenu) {
            JPopupMenu popup = (JPopupMenu) child;
            popup.setFocusable(false);
            popup.setBorder(BorderFactory.createLineBorder(Theme.BORDER));
            int width = combo.getWidth();
            if (width <= 0) {
                return;
            }
            int innerWidth = Math.max(0, width - 2);
            for (Component comp : popup.getComponents()) {
                if (comp instanceof JComponent) {
                    JComponent jc = (JComponent) comp;
                    jc.setAlignmentX(Component.LEFT_ALIGNMENT);
                    Dimension compPref = jc.getPreferredSize();
                    jc.setPreferredSize(new Dimension(innerWidth, compPref.height));
                    jc.setMaximumSize(new Dimension(innerWidth, Integer.MAX_VALUE));
                }
            }
            popup.setPreferredSize(null);
            Dimension pref = popup.getPreferredSize();
            popup.setPreferredSize(new Dimension(width, pref.height));
            popup.pack();
        }
    }

    private boolean shouldShowAllOnPopup() {
        if (listModel.getSize() >= allCities.size()) {
            return false;
        }
        if (!editing) {
            return true;
        }
        String text = TravelerUi.normalize(textField.getText());
        return text.isEmpty()
                || (committedCity != null && text.equals(TravelerUi.normalize(committedCity.getName())));
    }

    private void filterFromEditor(String typed) {
        if (updating || !editing || !typed.equals(textField.getText())) {
            return;
        }
        List<City> filtered = filter(typed);
        filtering = true;
        try {
            listModel.setVisibleCities(filtered);
            if (combo.isShowing()) {
                if (filtered.isEmpty()) {
                    if (combo.isPopupVisible()) {
                        combo.setPopupVisible(false);
                    }
                } else if (!combo.isPopupVisible()) {
                    combo.setPopupVisible(true);
                } else {
                    resizePopup();
                }
            }
        } finally {
            filtering = false;
        }
    }

    private void commitCity(City city, boolean fireSelectionEvent) {
        updating = true;
        try {
            committedCity = city;
            listModel.setVisibleCities(allCities);
            listModel.setSelectedItem(city);
            refreshDisplayLabels(city);
        } finally {
            updating = false;
        }
        if (fireSelectionEvent) {
            combo.setSelectedItem(city);
        }
    }

    private void restoreCommitted() {
        updating = true;
        try {
            listModel.setVisibleCities(allCities);
            listModel.setSelectedItem(committedCity);
            refreshDisplayLabels(committedCity);
        } finally {
            updating = false;
        }
    }

    private City resolveFromText(String text, boolean allowFirstFiltered) {
        String query = TravelerUi.normalize(text);
        if (query.isEmpty()) {
            return null;
        }
        City uniqueName = null;
        int nameMatches = 0;
        for (City c : allCities) {
            if (TravelerUi.normalize(c.toString()).equals(query)) {
                return c;
            }
            if (TravelerUi.normalize(c.getName()).equals(query)) {
                uniqueName = c;
                nameMatches++;
            }
        }
        if (nameMatches == 1) {
            return uniqueName;
        }
        if (allowFirstFiltered && listModel.getSize() > 0) {
            return listModel.getElementAt(0);
        }
        List<City> filtered = filter(query);
        return filtered.size() == 1 ? filtered.get(0) : null;
    }

    private List<City> filter(String query) {
        String q = TravelerUi.normalize(query);
        if (q.isEmpty()) {
            return allCities;
        }
        List<City> startsWithName = new ArrayList<>();
        List<City> containsName = new ArrayList<>();
        List<City> containsDept = new ArrayList<>();
        for (City c : allCities) {
            String nameNorm = TravelerUi.normalize(c.getName());
            if (nameNorm.startsWith(q)) {
                startsWithName.add(c);
            } else if (nameNorm.contains(q)) {
                containsName.add(c);
            } else if (TravelerUi.normalize(c.toString()).contains(q)) {
                containsDept.add(c);
            }
        }
        List<City> result = new ArrayList<>(
                startsWithName.size() + containsName.size() + containsDept.size());
        result.addAll(startsWithName);
        result.addAll(containsName);
        result.addAll(containsDept);
        return result;
    }

    /** Editor con doble tarjeta que impide que Swing sobrescriba el campo al escribir o borrar. */
    private final class DualModeComboEditor implements ComboBoxEditor {
        @Override
        public Component getEditorComponent() {
            return editorContainer;
        }

        @Override
        public void setItem(Object anObject) {
            if (!editing && anObject instanceof City) {
                committedCity = (City) anObject;
                refreshDisplayLabels(committedCity);
            }
        }

        @Override
        public Object getItem() {
            return committedCity;
        }

        @Override
        public void selectAll() {
        }

        @Override
        public void addActionListener(ActionListener l) {
            textField.addActionListener(l);
        }

        @Override
        public void removeActionListener(ActionListener l) {
            textField.removeActionListener(l);
        }
    }

    /** Modelo de lista que actualiza las sugerencias filtradas sin alterar el editor. */
    private static final class CityListModel extends AbstractListModel<City> implements ComboBoxModel<City> {
        private List<City> visible;
        private Object selectedItem;

        CityListModel(List<City> initial, City selectedItem) {
            this.visible = new ArrayList<>(initial);
            this.selectedItem = selectedItem;
        }

        void setVisibleCities(List<City> cities) {
            int oldSize = visible.size();
            this.visible = new ArrayList<>(cities);
            int max = Math.max(oldSize, visible.size());
            if (max > 0) {
                fireContentsChanged(this, 0, max - 1);
            }
        }

        @Override
        public int getSize() {
            return visible.size();
        }

        @Override
        public City getElementAt(int index) {
            return index >= 0 && index < visible.size() ? visible.get(index) : null;
        }

        @Override
        public void setSelectedItem(Object anItem) {
            if ((selectedItem != null && !selectedItem.equals(anItem))
                    || (selectedItem == null && anItem != null)) {
                selectedItem = anItem;
                fireContentsChanged(this, -1, -1);
            }
        }

        @Override
        public Object getSelectedItem() {
            return selectedItem;
        }
    }

    /** Renderizador de dos líneas para la lista desplegable: municipio arriba y departamento abajo. */
    private static final class CityCellRenderer extends JPanel implements ListCellRenderer<City> {
        private final JLabel cityLabel = new JLabel();
        private final JLabel deptLabel = new JLabel();

        CityCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setOpaque(true);
            setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(241, 245, 249)),
                    BorderFactory.createEmptyBorder(6, 10, 6, 10)));
            cityLabel.setFont(Theme.font(Font.BOLD, 13));
            deptLabel.setFont(Theme.font(Font.PLAIN, 11));
            add(cityLabel);
            add(deptLabel);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends City> list, City value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {
            if (value != null) {
                cityLabel.setText(value.getName());
                cityLabel.setToolTipText(value.toString());
                deptLabel.setText(departmentSubtitle(value));
                setToolTipText(value.toString());
            } else {
                cityLabel.setText(" ");
                deptLabel.setText(" ");
                setToolTipText(null);
            }

            if (isSelected) {
                setBackground(Theme.PRIMARY_SOFT);
                cityLabel.setForeground(Theme.PRIMARY);
                deptLabel.setForeground(Theme.TEXT);
            } else {
                setBackground(Color.WHITE);
                cityLabel.setForeground(Theme.TEXT);
                deptLabel.setForeground(Theme.MUTED);
            }
            return this;
        }
    }
}