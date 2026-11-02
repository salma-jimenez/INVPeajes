
package com.invpeajes.model;

/**
 * Representa una caseta física o carril de cobro específico dentro de una estación de peaje.
 * Cada caseta pertenece a un único peaje y maneja su propio estado y configuración.
 * 
 * @author Luis Guerrero
 * @version 1.1
 */
public class TollBooth {
    private Integer boothId;
    private int boothNumber;
    private String paymentType;
    private String direction;
    private String status;
    private int tollId;
    
    /**
     * Constructor para inicializar una nueva caseta con todos sus atributos base de datos.
     * 
     * @param boothId Identificador de la caseta. Usar null al crear una nueva: Oracle lo genera.
     * @param boothNumber Número de la caseta dentro del peaje (ej. 3).
     * @param paymentType Modalidad de pago: EFECTIVO, COLPASS o MIXTO.
     * @param direction Sentido del tráfico: AMBOS, NORTE_SUR, SUR_NORTE, 
     * ORIENTE_OCCIDENTE, OCCIDENTE_ORIENTE u OTRO (puede ser null).
     * @param status Estado de la caseta: ACTIVA, INACTIVA o MANTENIMIENTO.
     * @param tollId ID de la estación de peaje a la que pertenece (Llave foránea).
     */
    public TollBooth(Integer boothId, int boothNumber, String paymentType, 
                  String direction, String status, int tollId) {
        this.boothId = boothId;
        this.boothNumber = boothNumber;
        this.paymentType = paymentType;
        this.direction = direction;
        this.status = status;
        this.tollId = tollId;
    }

    /**
     * Obtiene el identificador de la caseta.
     * @return El ID de la caseta.
     */
    public Integer getBoothId() {
        return boothId; 
    }
    
    /**
     * Establece el identificador de la caseta.
     * @param boothId El nuevo ID a asignar.
     */
    public void setBoothId(Integer boothId) {
        this.boothId = boothId; 
    }

    /**
     * Obtiene el número o denominación de la caseta.
     * @return El número de la caseta.
     */
    public int getBoothNumber() { 
        return boothNumber; 
    }
    
    /**
     * Establece el número de la caseta.
     * @param boothNumber El nuevo número a asignar.
     */
    public void setBoothNumber(int boothNumber) { 
        this.boothNumber = boothNumber; 
    }

    /**
     * Obtiene el tipo de pago de la caseta.
     * @return EFECTIVO, COLPASS o MIXTO.
     */
    public String getPaymentType() { 
        return paymentType; 
    }
    
    /**
     * Establece el tipo de pago de la caseta.
     * @param paymentType EFECTIVO, COLPASS o MIXTO.
     */
    public void setPaymentType(String paymentType) { 
        this.paymentType = paymentType; 
    }

    /**
     * Obtiene el sentido del tráfico que atiende la caseta.
     * @return el sentido del tráfico.
     */
    public String getDirection() { 
        return direction; 
    }
    
    /**
     * Establece el sentido del tráfico de la caseta.
     * @param direction AMBOS, NORTE_SUR, SUR_NORTE, ORIENTE_OCCIDENTE, OCCIDENTE_ORIENTE u OTRO.
     */
    public void setDirection(String direction) { 
        this.direction = direction; 
    }

    /**
     * Obtiene el estado operativo de la caseta.
     * @return ACTIVA, INACTIVA o MANTENIMIENTO.
     */
    public String getStatus() { 
        return status; 
    }
    
    /**
     * Establece el estado operativo de la caseta.
     * @param status ACTIVA, INACTIVA o MANTENIMIENTO.
     */
    public void setStatus(String status) { 
        this.status = status; 
    }

    /**
     * Obtiene el identificador del peaje al que pertenece.
     * @return El ID del peaje (Llave foránea).
     */
    public int getTollId() { 
        return tollId; 
    }
    
    /**
     * Establece el identificador del peaje.
     * @param tollId El nuevo ID del peaje a asignar.
     */
    public void setTollId(int tollId) { 
        this.tollId = tollId; 
    }
}