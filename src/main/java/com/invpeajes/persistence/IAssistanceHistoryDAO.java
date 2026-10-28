
package com.invpeajes.persistence;

import com.invpeajes.model.AssistanceHistory;
import java.util.List;

/**
 * Contrato de consulta del historial de asistencias.
 * @author Salomejimenez
 */

    
    public interface IAssistanceHistoryDAO {

    /**
     * Lista los cambios de estado de una asistencia en orden cronológico.
     *
     * @param assistanceId asistencia a consultar
     * @return historial de la asistencia
     */
    List<AssistanceHistory> findByAssistance(int assistanceId);
}

