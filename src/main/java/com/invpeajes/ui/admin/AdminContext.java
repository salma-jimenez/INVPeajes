package com.invpeajes.ui.admin;

import com.invpeajes.model.Toll;
import com.invpeajes.model.TollUser;
import com.invpeajes.service.AdminService;
import com.invpeajes.service.TravelerService.Catalog;
import java.util.List;

/**
 * Lo que comparten las pantallas del administrador: quién es, su entidad, el catálogo (todos los
 * peajes, departamentos, rutas, concesionarios y categorías) y los peajes de su entidad.
 *
 * @author Salomejimenez
 * @version 1.0
 */
public final class AdminContext {

    private final TollUser admin;
    private final Catalog catalog;
    private final List<Toll> mine;

    /**
     * @param admin Administrador con su entidad.
     * @param catalog Catálogo completo.
     */
    public AdminContext(TollUser admin, Catalog catalog) {
        this.admin = admin;
        this.catalog = catalog;
        this.mine = AdminService.tollsOf(catalog, admin.getEntity());
    }

    /** @return Administrador. */
    public TollUser getAdmin() {
        return admin;
    }

    /** @return Entidad del administrador (PEAJE.NIVEL). */
    public String getLevel() {
        return admin.getEntity();
    }

    /** @return Catálogo completo. */
    public Catalog getCatalog() {
        return catalog;
    }

    /** @return Peajes de la entidad, por nombre. */
    public List<Toll> getMine() {
        return mine;
    }

    /**
     * @param tollId ID del peaje.
     * @return El peaje si es de la entidad, o null.
     */
    public Toll mineById(int tollId) {
        for (Toll t : mine) {
            if (t.getTollId() == tollId) {
                return t;
            }
        }
        return null;
    }
}
