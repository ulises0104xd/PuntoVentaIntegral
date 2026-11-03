package com.puntoventa.ui;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.util.Collections;
import javax.swing.UIManager;

/** Sistema de diseño: paleta, tipografía y ajustes globales del aspecto (FlatLaf). */
final class Tema {

    static final Color PRIMARIO = new Color(0x2F6FED);
    static final Color PRIMARIO_OSCURO = new Color(0x2459C9);
    static final Color FONDO = new Color(0xF4F6FA);
    static final Color SUPERFICIE = Color.WHITE;
    static final Color BORDE = new Color(0xD0D5DD);
    static final Color LINEA = new Color(0xEAECF0);
    static final Color TEXTO = new Color(0x1F2933);
    static final Color SUAVE = new Color(0x667085);
    static final Color EXITO = new Color(0x1E8E3E);
    static final Color EXITO_FONDO = new Color(0xE6F4EA);
    static final Color AVISO = new Color(0xB26A00);
    static final Color AVISO_FONDO = new Color(0xFFF4E0);
    static final Color PELIGRO = new Color(0xC62828);
    static final Color PELIGRO_FONDO = new Color(0xFDECEA);
    static final Color INFO = new Color(0x1D4ED8);
    static final Color INFO_FONDO = new Color(0xE8F0FE);
    static final Color SELECCION = new Color(0xDCE8FD);
    static final Color ZEBRA = new Color(0xF9FAFC);

    private static boolean instalado;

    private Tema() {
    }

    /** Instala el aspecto una sola vez, antes de crear cualquier componente. */
    static synchronized void instalar() {
        if (instalado) {
            return;
        }
        FlatLaf.setGlobalExtraDefaults(Collections.singletonMap("@accentColor", "#2F6FED"));
        FlatLightLaf.setup();
        Font base = UIManager.getFont("defaultFont");
        UIManager.put("defaultFont", base.deriveFont(14f));
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("CheckBox.arc", 6);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("ScrollBar.thumbArc", 999);
        UIManager.put("ScrollBar.trackArc", 999);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("Table.rowHeight", 34);
        UIManager.put("Table.intercellSpacing", new Dimension(0, 1));
        UIManager.put("Table.selectionBackground", SELECCION);
        UIManager.put("Table.selectionForeground", TEXTO);
        UIManager.put("Table.selectionInactiveBackground", SELECCION);
        UIManager.put("Table.selectionInactiveForeground", TEXTO);
        UIManager.put("Table.showVerticalLines", false);
        UIManager.put("TableHeader.height", 36);
        UIManager.put("TabbedPane.tabHeight", 44);
        UIManager.put("TabbedPane.tabInsets", new java.awt.Insets(4, 22, 4, 22));
        UIManager.put("TabbedPane.selectedBackground", SUPERFICIE);
        UIManager.put("OptionPane.buttonMinimumWidth", 90);
        instalado = true;
    }

    static Font fuente(float tamano, int estilo) {
        return UIManager.getFont("defaultFont").deriveFont(estilo, tamano);
    }
}
