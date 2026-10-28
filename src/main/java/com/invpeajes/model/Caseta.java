
package com.invpeajes.model;

/**
 * Representa una caseta física o carril de cobro específico dentro de una estación de peaje.
 * Cada caseta pertenece a un único peaje y maneja su propio estado y configuración.
 * 
 * @author Luis Guerrero
 * @version 1.1
 */
public class Caseta {
    private Integer idCaseta;
    private int numeroCaseta;
    private String tipoPago;
    private String sentido;
    private String estado;
    private int idPeaje;
    
    /**
     * Constructor para inicializar una nueva caseta con todos sus atributos base de datos.
     * 
     * @param idCaseta Identificador de la caseta. Usar null al crear una nueva: Oracle lo genera.
     * @param numeroCaseta Número de la caseta dentro del peaje (ej. 3).
     * @param tipoPago Modalidad de pago: EFECTIVO, COLPASS o MIXTO.
     * @param sentido Sentido del tráfico: AMBOS, NORTE_SUR, SUR_NORTE, 
     * ORIENTE_OCCIDENTE, OCCIDENTE_ORIENTE u OTRO (puede ser null).
     * @param estado Estado de la caseta: ACTIVA, INACTIVA o MANTENIMIENTO.
     * @param idPeaje ID de la estación de peaje a la que pertenece (Llave foránea).
     */
    public Caseta(Integer idCaseta, int numeroCaseta, String tipoPago, 
                  String sentido, String estado, int idPeaje) {
        this.idCaseta = idCaseta;
        this.numeroCaseta = numeroCaseta;
        this.tipoPago = tipoPago;
        this.sentido = sentido;
        this.estado = estado;
        this.idPeaje = idPeaje;
    }

    /**
     * Obtiene el identificador de la caseta.
     * @return El ID de la caseta.
     */
    public Integer getIdCaseta() {
        return idCaseta; 
    }
    
    /**
     * Establece el identificador de la caseta.
     * @param idCaseta El nuevo ID a asignar.
     */
    public void setIdCaseta(Integer idCaseta) {
        this.idCaseta = idCaseta; 
    }

    /**
     * Obtiene el número o denominación de la caseta.
     * @return El número de la caseta.
     */
    public int getNumeroCaseta() { 
        return numeroCaseta; 
    }
    
    /**
     * Establece el número de la caseta.
     * @param numeroCaseta El nuevo número a asignar.
     */
    public void setNumeroCaseta(int numeroCaseta) { 
        this.numeroCaseta = numeroCaseta; 
    }

    /**
     * Obtiene el tipo de pago de la caseta.
     * @return EFECTIVO, COLPASS o MIXTO.
     */
    public String getTipoPago() { 
        return tipoPago; 
    }
    
    /**
     * Establece el tipo de pago de la caseta.
     * @param tipoPago EFECTIVO, COLPASS o MIXTO.
     */
    public void setTipoPago(String tipoPago) { 
        this.tipoPago = tipoPago; 
    }

    /**
     * Obtiene el sentido del tráfico que atiende la caseta.
     * @return el sentido del tráfico.
     */
    public String getSentido() { 
        return sentido; 
    }
    
    /**
     * Establece el sentido del tráfico de la caseta.
     * @param sentido AMBOS, NORTE_SUR, SUR_NORTE, ORIENTE_OCCIDENTE, OCCIDENTE_ORIENTE u OTRO.
     */
    public void setSentido(String sentido) { 
        this.sentido = sentido; 
    }

    /**
     * Obtiene el estado operativo de la caseta.
     * @return ACTIVA, INACTIVA o MANTENIMIENTO.
     */
    public String getEstado() { 
        return estado; 
    }
    
    /**
     * Establece el estado operativo de la caseta.
     * @param estado ACTIVA, INACTIVA o MANTENIMIENTO.
     */
    public void setEstado(String estado) { 
        this.estado = estado; 
    }

    /**
     * Obtiene el identificador del peaje al que pertenece.
     * @return El ID del peaje (Llave foránea).
     */
    public int getIdPeaje() { 
        return idPeaje; 
    }
    
    /**
     * Establece el identificador del peaje.
     * @param idPeaje El nuevo ID del peaje a asignar.
     */
    public void setIdPeaje(int idPeaje) { 
        this.idPeaje = idPeaje; 
    }
}