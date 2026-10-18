    
package com.invpeajes.model;

/**
 * Representa una caseta física o carril de cobro específico dentro de una estación de peaje.
 * Cada caseta pertenece a un único peaje y maneja su propio estado y configuración.
 * 
 * @author Luis Guerrero
 * @version 1.0
 */
public class Caseta {
    private String idCaseta;
    private String numeroCaseta;
    private String tipoPago;     // Ej. "Efectivo", "Electrónico", "Mixto"
    private String sentido;      // Ej. "Norte-Sur", "Oriente-Occidente"
    private String estado;       // Ej. "Abierta", "Cerrada"
    private String idPeaje;      // Llave foránea a Peaje

    /**
     * Constructor para inicializar una nueva caseta con todos sus atributos base de datos.
     * 
     * @param idCaseta Identificador alfanumérico único de la caseta.
     * @param numeroCaseta Número o nombre de identificación en el peaje (ej. "3").
     * @param tipoPago Modalidad de pago aceptada en este carril.
     * @param sentido Dirección del tráfico que atiende la caseta.
     * @param estado Estado actual de operatividad de la caseta.
     * @param idPeaje ID de la estación de peaje a la que pertenece (Llave foránea).
     */
    public Caseta(String idCaseta, String numeroCaseta, String tipoPago, 
                  String sentido, String estado, String idPeaje) {
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
    public String getIdCaseta() { return idCaseta; }
    
    /**
     * Establece el identificador de la caseta.
     * @param idCaseta El nuevo ID a asignar.
     */
    public void setIdCaseta(String idCaseta) { this.idCaseta = idCaseta; }

    /**
     * Obtiene el número o denominación de la caseta.
     * @return El número de la caseta.
     */
    public String getNumeroCaseta() { return numeroCaseta; }
    
    /**
     * Establece el número de la caseta.
     * @param numeroCaseta El nuevo número a asignar.
     */
    public void setNumeroCaseta(String numeroCaseta) { this.numeroCaseta = numeroCaseta; }

    /**
     * Obtiene el tipo de pago aceptado en la caseta.
     * @return La modalidad de pago.
     */
    public String getTipoPago() { return tipoPago; }
    
    /**
     * Establece el tipo de pago de la caseta.
     * @param tipoPago La nueva modalidad de pago.
     */
    public void setTipoPago(String tipoPago) { this.tipoPago = tipoPago; }

    /**
     * Obtiene el sentido del tráfico que atiende la caseta.
     * @return La dirección del tráfico.
     */
    public String getSentido() { return sentido; }
    
    /**
     * Establece el sentido del tráfico de la caseta.
     * @param sentido La nueva dirección a asignar.
     */
    public void setSentido(String sentido) { this.sentido = sentido; }

    /**
     * Obtiene el estado operativo de la caseta.
     * @return El estado de la caseta.
     */
    public String getEstado() { return estado; }
    
    /**
     * Establece el estado operativo de la caseta.
     * @param estado El nuevo estado a asignar.
     */
    public void setEstado(String estado) { this.estado = estado; }

    /**
     * Obtiene el identificador del peaje al que pertenece.
     * @return El ID del peaje (Llave foránea).
     */
    public String getIdPeaje() { return idPeaje; }
    
    /**
     * Establece el identificador del peaje.
     * @param idPeaje El nuevo ID del peaje a asignar.
     */
    public void setIdPeaje(String idPeaje) { this.idPeaje = idPeaje; }
}