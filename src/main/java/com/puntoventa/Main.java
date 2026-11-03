package com.puntoventa;

import com.puntoventa.service.AuthService;
import com.puntoventa.ui.LoginFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // se usa el aspecto por defecto de Swing
        }
        SwingUtilities.invokeLater(() -> {
            try {
                new AuthService().sembrarUsuariosIniciales();
                new LoginFrame().setVisible(true);
            } catch (RuntimeException e) {
                JOptionPane.showMessageDialog(null,
                        e.getMessage() + "\n\nVerifique que MySQL esté en ejecución y que db/schema.sql se haya aplicado.",
                        "No se pudo iniciar", JOptionPane.ERROR_MESSAGE);
                System.exit(1);
            }
        });
    }
}
