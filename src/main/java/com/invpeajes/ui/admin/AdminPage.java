package com.invpeajes.ui.admin;

import javax.swing.JComponent;

/**
 * Una pantalla del panel de Administración (Resumen, Mapa, Peajes, Empleados, Mi cuenta).
 *
 * @author Salomejimenez
 * @version 1.0
 */
public interface AdminPage {

    /**
     * Recibe los datos del administrador y del catálogo (al entrar y cada vez que cambian).
     * @param context Datos compartidos.
     */
    void setContext(AdminContext context);

    /** Vuelve a cargar lo que muestra la pantalla. */
    default void refresh() {
    }

    /**
     * @return Botón para la barra de arriba (ej. "+ Registrar peaje"), o null.
     */
    default JComponent topAction() {
        return null;
    }

    /**
     * @param context Datos compartidos.
     * @return Texto gris debajo del título.
     */
    String subtitle(AdminContext context);
}
