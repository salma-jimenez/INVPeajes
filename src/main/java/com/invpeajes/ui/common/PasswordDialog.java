package com.invpeajes.ui.common;

import com.invpeajes.service.AuthService;
import com.invpeajes.service.StaffService;
import java.awt.Component;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * Ventana "Cambiar contraseña", la misma para el operador, el jefe de estación y el
 * administrador: pide la contraseña actual, la nueva y su confirmación.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public final class PasswordDialog {

    private PasswordDialog() {
    }

    /**
     * Abre la ventana.
     * @param parent Componente de la ventana dueña.
     * @param staff Servicio de empleados.
     * @param idNumber Cédula de quien cambia su contraseña.
     */
    public static void open(Component parent, StaffService staff, String idNumber) {
        JPasswordField current = Forms.password();
        JPasswordField password = Forms.password();
        JPasswordField confirm = Forms.password();
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.add(Forms.field("Contraseña actual", current));
        body.add(Box.createVerticalStrut(10));
        body.add(Forms.field("Nueva contraseña", password));
        body.add(Box.createVerticalStrut(10));
        body.add(Forms.field("Confirmar nueva contraseña", confirm));
        Forms.dialog(parent, "Cambiar contraseña", "Mínimo " + AuthService.MIN_PASSWORD_LENGTH
                + " caracteres. Si todavía usas tu cédula como contraseña, cámbiala.", body, "Cambiar contraseña",
                false, () -> {
                    staff.changePassword(idNumber, current.getPassword(), password.getPassword(),
                            confirm.getPassword());
                    return Boolean.TRUE;
                }, ok -> Forms.info(parent, "Cambiar contraseña", "Tu contraseña quedó cambiada."));
    }
}
