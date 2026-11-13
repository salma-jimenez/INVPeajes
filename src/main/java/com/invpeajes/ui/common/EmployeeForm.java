package com.invpeajes.ui.common;

import com.invpeajes.model.TollUser;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;

/**
 * Formulario con los datos de un empleado: cédula, nombres, apellidos, teléfono, correo y fecha
 * de ingreso. Lo usan el jefe de estación (operadores) y el administrador (jefes y sus propios
 * datos), para registrar y para editar.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class EmployeeForm extends JPanel {

    private final JTextField idNumber = Forms.text("");
    private final JTextField firstName = Forms.text("");
    private final JTextField middleName = Forms.text("");
    private final JTextField firstSurname = Forms.text("");
    private final JTextField secondSurname = Forms.text("");
    private final JTextField phone = Forms.text("");
    private final JTextField email = Forms.text("");
    private final JTextField hireDate = Forms.text("");
    private final boolean registering;

    /**
     * @param registering true para registrar (la cédula se puede escribir); false para editar.
     * @param showHireDate true para mostrar la fecha de ingreso.
     * @param extra Fila extra al final (ej. cargo y peaje), o null.
     */
    public EmployeeForm(boolean registering, boolean showHireDate, JComponent extra) {
        this.registering = registering;
        setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
        setOpaque(false);
        if (!registering) {
            idNumber.setEditable(false);
            idNumber.setBackground(Theme.DISABLED);
            idNumber.setForeground(Theme.MUTED);
        }
        if (registering && showHireDate) {
            add(Forms.row(Forms.field("Cédula *", idNumber), Forms.field("Fecha de ingreso", hireDate)));
        } else {
            add(Forms.field(registering ? "Cédula *" : "Cédula (no se puede cambiar)", idNumber));
        }
        add(Box.createVerticalStrut(10));
        add(Forms.row(Forms.field("Primer nombre *", firstName), Forms.field("Segundo nombre", middleName)));
        add(Box.createVerticalStrut(10));
        add(Forms.row(Forms.field("Primer apellido *", firstSurname), Forms.field("Segundo apellido", secondSurname)));
        add(Box.createVerticalStrut(10));
        if (!registering && showHireDate) {
            add(Forms.row(Forms.field("Teléfono", phone), Forms.field("Fecha de ingreso", hireDate)));
        } else {
            add(Forms.field("Teléfono", phone));
        }
        add(Box.createVerticalStrut(10));
        add(Forms.field("Correo *", email));
        if (extra != null) {
            add(Box.createVerticalStrut(10));
            extra.setAlignmentX(LEFT_ALIGNMENT);
            add(extra);
        }
        if (registering) {
            hireDate.setText(Forms.formatDate(java.time.LocalDate.now()));
        }
    }

    /**
     * Muestra los datos de un empleado.
     * @param user Empleado.
     */
    public void load(TollUser user) {
        idNumber.setText(user.getIdNumber());
        firstName.setText(orEmpty(user.getFirstName()));
        middleName.setText(orEmpty(user.getMiddleName()));
        firstSurname.setText(orEmpty(user.getFirstSurname()));
        secondSurname.setText(orEmpty(user.getSecondSurname()));
        phone.setText(orEmpty(user.getPhone()));
        email.setText(orEmpty(user.getEmail()));
        hireDate.setText(Forms.formatDate(user.getHireDate()));
    }

    /**
     * Lee lo escrito en el formulario.
     * @param base Empleado de donde se copian el cargo, el peaje, la entidad y el estado (o null
     *             si es nuevo).
     * @return Un empleado nuevo con los datos del formulario.
     */
    public TollUser read(TollUser base) {
        TollUser user = new TollUser(idNumber.getText().trim(), firstName.getText(), middleName.getText(),
                firstSurname.getText(), secondSurname.getText(), phone.getText(), email.getText(),
                base == null ? null : base.getPosition(), Forms.parseDate(hireDate.getText()),
                base == null ? null : base.getTollId());
        if (base != null) {
            user.setActive(base.isActive());
            user.setEntity(base.getEntity());
            user.setDefaultPassword(base.isDefaultPassword());
            if (user.getHireDate() == null && hireDate.getText().isBlank()) {
                user.setHireDate(base.getHireDate());
            }
        }
        if (!hireDate.getText().isBlank() && user.getHireDate() == null) {
            throw new IllegalArgumentException("Escribe la fecha de ingreso como dd/mm/aaaa");
        }
        return user;
    }

    /** @return true si el formulario es para registrar a alguien nuevo. */
    public boolean isRegistering() {
        return registering;
    }

    /**
     * Deja editar o no los campos (ej. para un operador que el administrador solo puede ver).
     * @param editable true para dejar editar.
     */
    public void setEditable(boolean editable) {
        for (JTextField f : new JTextField[]{firstName, middleName, firstSurname, secondSurname, phone, email,
            hireDate}) {
            f.setEditable(editable);
            f.setBackground(editable ? java.awt.Color.WHITE : Theme.DISABLED);
        }
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
