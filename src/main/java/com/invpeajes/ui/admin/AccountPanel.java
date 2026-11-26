package com.invpeajes.ui.admin;

import com.invpeajes.model.TollUser;
import com.invpeajes.service.AdminService;
import com.invpeajes.service.AuthService;
import com.invpeajes.ui.common.Async;
import com.invpeajes.ui.common.EmployeeForm;
import com.invpeajes.ui.common.FlatButton;
import com.invpeajes.ui.common.Forms;
import com.invpeajes.ui.common.Labels;
import com.invpeajes.ui.common.Theme;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

/**
 * "Mi cuenta" del administrador: editar sus propios nombres, teléfono y correo (la cédula y la
 * entidad no se cambian) y cambiar su contraseña.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public class AccountPanel extends JPanel implements AdminPage {

    private final AdminService service;
    private final Consumer<TollUser> onSaved;
    private final JPanel dataCard = new JPanel(new BorderLayout(0, 14));
    private AdminContext context;

    /**
     * @param service Servicio del administrador.
     * @param onSaved Qué hacer después de guardar sus datos (actualizar las demás pantallas).
     */
    public AccountPanel(AdminService service, Consumer<TollUser> onSaved) {
        super(new FlowLayout(FlowLayout.LEFT, 0, 0));
        this.service = service;
        this.onSaved = onSaved;
        setBackground(Theme.BACKGROUND);
        setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        dataCard.setBackground(Color.WHITE);
        dataCard.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        add(dataCard);
        add(Box.createHorizontalStrut(14));
        add(passwordCard());
    }

    @Override
    public void setContext(AdminContext context) {
        this.context = context;
        fill(context.getAdmin());
    }

    @Override
    public String subtitle(AdminContext context) {
        return "Tus datos y tu contraseña";
    }

    private void fill(TollUser admin) {
        dataCard.removeAll();
        JPanel head = new JPanel(new BorderLayout(0, 2));
        head.setOpaque(false);
        JLabel name = new JLabel(admin.getFullName());
        name.setFont(Theme.font(Font.BOLD, 22));
        name.setForeground(Theme.TEXT);
        head.add(name, BorderLayout.NORTH);
        head.add(Theme.muted("Administrador · " + Labels.entity(admin.getEntity())
                + (admin.getHireDate() == null ? "" : " · desde " + Forms.formatDate(admin.getHireDate()))),
                BorderLayout.CENTER);
        JComponent extra = Forms.field("Entidad (no se puede cambiar)",
                Forms.readOnly(Labels.entityLong(admin.getEntity())));
        EmployeeForm form = new EmployeeForm(false, false, extra);
        form.load(admin);
        FlatButton save = Theme.primaryButton("Guardar cambios");
        save.addActionListener(e -> Async.run(() -> {
            TollUser changed = form.read(admin);
            service.updateOwnAccount(changed);
            return changed;
        }, changed -> {
            Forms.info(this, "Mi cuenta", "Tus datos quedaron guardados.");
            onSaved.accept(changed);
        }, error -> Forms.info(this, "Mi cuenta", error.getMessage())));
        JPanel south = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        south.setOpaque(false);
        south.add(save);
        dataCard.add(head, BorderLayout.NORTH);
        dataCard.add(form, BorderLayout.CENTER);
        dataCard.add(south, BorderLayout.SOUTH);
        dataCard.setPreferredSize(null);
        dataCard.setPreferredSize(new Dimension(560, dataCard.getPreferredSize().height));
        dataCard.revalidate();
        dataCard.repaint();
    }

    private JComponent passwordCard() {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Theme.BORDER),
                BorderFactory.createEmptyBorder(20, 22, 20, 22)));
        card.setPreferredSize(new Dimension(380, 400));
        card.add(Theme.title("Cambiar contraseña"), BorderLayout.NORTH);
        JPasswordField current = Forms.password();
        JPasswordField password = Forms.password();
        JPasswordField confirm = Forms.password();
        JPanel fields = new JPanel();
        fields.setLayout(new BoxLayout(fields, BoxLayout.Y_AXIS));
        fields.setOpaque(false);
        fields.add(Forms.field("Contraseña actual", current));
        fields.add(Box.createVerticalStrut(10));
        fields.add(Forms.field("Nueva contraseña", password));
        fields.add(Box.createVerticalStrut(10));
        fields.add(Forms.field("Confirmar nueva contraseña", confirm));
        fields.add(Box.createVerticalStrut(8));
        JLabel hint = Theme.muted("<html>Mínimo " + AuthService.MIN_PASSWORD_LENGTH + " caracteres. Si todavía usas "
                + "tu cédula como contraseña, cámbiala.</html>");
        hint.setFont(Theme.font(Font.PLAIN, 12));
        hint.setAlignmentX(LEFT_ALIGNMENT);
        fields.add(hint);
        card.add(fields, BorderLayout.CENTER);
        FlatButton change = Theme.primaryButton("Cambiar contraseña");
        change.addActionListener(e -> {
            String id = context == null ? null : context.getAdmin().getIdNumber();
            if (id == null) {
                return;
            }
            char[] a = current.getPassword();
            char[] b = password.getPassword();
            char[] c = confirm.getPassword();
            Async.run(() -> {
                service.staff().changePassword(id, a, b, c);
                return Boolean.TRUE;
            }, ok -> {
                current.setText("");
                password.setText("");
                confirm.setText("");
                Forms.info(this, "Mi cuenta", "Tu contraseña quedó cambiada.");
            }, error -> Forms.info(this, "Mi cuenta", error.getMessage()));
        });
        card.add(change, BorderLayout.SOUTH);
        return card;
    }
}
