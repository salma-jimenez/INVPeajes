# INVPeajes - Full Class Diagram
This document presents the unified class diagram for **INVPeajes**, structured across its 4 architectural layers: **Model**, **Persistence**, **Service**, and **User Interface (UI)**.

## Unified Class Diagram (Mermaid)

```mermaid
classDiagram
    direction TB

    namespace Model {
        class Peaje {
            -int id
            -String name
            -String location
            -double latitude
            -double longitude
            +Peaje(int id, String name, String location, double latitude, double longitude)
            +getId() int
            +setId(int id) void
            +getName() String
            +setName(String name) void
            +getLocation() String
            +setLocation(String location) void
            +getLatitude() double
            +setLatitude(double latitude) void
            +getLongitude() double
            +setLongitude(double longitude) void
        }
        class PeajeElectronico {
            -String tagProtocol
            -boolean automaticLane
            +PeajeElectronico(int id, String name, String location, double latitude, double longitude, String tagProtocol, boolean automaticLane)
            +getTagProtocol() String
            +setTagProtocol(String tagProtocol) void
            +isAutomaticLane() boolean
            +setAutomaticLane(boolean automaticLane) void
        }
        class PeajeManual {
            -boolean acceptsCash
            -boolean acceptsCard
            +PeajeManual(int id, String name, String location, double latitude, double longitude, boolean acceptsCash, boolean acceptsCard)
            +isAcceptsCash() boolean
            +setAcceptsCash(boolean acceptsCash) void
            +isAcceptsCard() boolean
            +setAcceptsCard(boolean acceptsCard) void
        }
        class Caseta {
            -int id
            -int laneNumber
            -String laneType
            -boolean active
            +Caseta(int id, int laneNumber, String laneType, boolean active)
            +getId() int
            +setId(int id) void
            +getLaneNumber() int
            +setLaneNumber(int laneNumber) void
            +getLaneType() String
            +setLaneType(String laneType) void
            +isActive() boolean
            +setActive(boolean active) void
        }
        class Ticket {
            -int id
            -String code
            -double amount
            -String issuedDate
            +Ticket(int id, String code, double amount, String issuedDate)
            +getId() int
            +setId(int id) void
            +getCode() String
            +setCode(String code) void
            +getAmount() double
            +setAmount(double amount) void
            +getIssuedDate() String
            +setIssuedDate(String issuedDate) void
        }
        class TollRate {
            -int id
            -double rateValue
            -String effectiveDate
            +TollRate(int id, double rateValue, String effectiveDate)
            +getId() int
            +setId(int id) void
            +getRateValue() double
            +setRateValue(double rateValue) void
            +getEffectiveDate() String
            +setEffectiveDate(String effectiveDate) void
        }
        class TollUser {
            -int id
            -String fullName
            -String documentId
            -String email
            +TollUser(int id, String fullName, String documentId, String email)
            +getId() int
            +setId(int id) void
            +getFullName() String
            +setFullName(String fullName) void
            +getDocumentId() String
            +setDocumentId(String documentId) void
            +getEmail() String
            +setEmail(String email) void
        }
        class Vehicle {
            -int id
            -String plate
            -String brand
            -String model
            +Vehicle(int id, String plate, String brand, String model)
            +getId() int
            +setId(int id) void
            +getPlate() String
            +setPlate(String plate) void
            +getBrand() String
            +setBrand(String brand) void
            +getModel() String
            +setModel(String model) void
        }
        class VehicleCategory {
            -int id
            -String code
            -String name
            -int axleCount
            +VehicleCategory(int id, String code, String name, int axleCount)
            +getId() int
            +setId(int id) void
            +getCode() String
            +setCode(String code) void
            +getName() String
            +setName(String name) void
            +getAxleCount() int
            +setAxleCount(int axleCount) void
        }
        class Assistance {
            -int id
            -String description
            -String reportedAt
            +Assistance(int id, String description, String reportedAt)
            +getId() int
            +setId(int id) void
            +getDescription() String
            +setDescription(String description) void
            +getReportedAt() String
            +setReportedAt(String reportedAt) void
        }
        class AssistanceHistory {
            -int id
            -String actionTaken
            -String updatedAt
            +AssistanceHistory(int id, String actionTaken, String updatedAt)
            +getId() int
            +setId(int id) void
            +getActionTaken() String
            +setActionTaken(String actionTaken) void
            +getUpdatedAt() String
            +setUpdatedAt(String updatedAt) void
        }
        class AssistanceResource {
            -int id
            -String name
            -String resourceType
            +AssistanceResource(int id, String name, String resourceType)
            +getId() int
            +setId(int id) void
            +getName() String
            +setName(String name) void
            +getResourceType() String
            +setResourceType(String resourceType) void
        }
        class AssistanceStatus {
            -int id
            -String name
            +AssistanceStatus(int id, String name)
            +getId() int
            +setId(int id) void
            +getName() String
            +setName(String name) void
        }
        class AssistanceType {
            -int id
            -String name
            +AssistanceType(int id, String name)
            +getId() int
            +setId(int id) void
            +getName() String
            +setName(String name) void
        }
        class ResourceAvailability {
            -int id
            -String status
            -boolean available
            +ResourceAvailability(int id, String status, boolean available)
            +getId() int
            +setId(int id) void
            +getStatus() String
            +setStatus(String status) void
            +isAvailable() boolean
            +setAvailable(boolean available) void
        }
        class Ruta {
            -int id
            -String code
            -String name
            +Ruta(int id, String code, String name)
            +getId() int
            +setId(int id) void
            +getCode() String
            +setCode(String code) void
            +getName() String
            +setName(String name) void
        }
        class Sector {
            -int id
            -String name
            +Sector(int id, String name)
            +getId() int
            +setId(int id) void
            +getName() String
            +setName(String name) void
        }
        class Departamento {
            -int id
            -String code
            -String name
            +Departamento(int id, String code, String name)
            +getId() int
            +setId(int id) void
            +getCode() String
            +setCode(String code) void
            +getName() String
            +setName(String name) void
        }
        class Municipio {
            -int id
            -String code
            -String name
            +Municipio(int id, String code, String name)
            +getId() int
            +setId(int id) void
            +getCode() String
            +setCode(String code) void
            +getName() String
            +setName(String name) void
        }
        class Concesionario {
            -int id
            -String nit
            -String name
            +Concesionario(int id, String nit, String name)
            +getId() int
            +setId(int id) void
            +getNit() String
            +setNit(String nit) void
            +getName() String
            +setName(String name) void
        }
        class Intermediador {
            -int id
            -String name
            -String contactEmail
            +Intermediador(int id, String name, String contactEmail)
            +getId() int
            +setId(int id) void
            +getName() String
            +setName(String name) void
            +getContactEmail() String
            +setContactEmail(String contactEmail) void
        }
    }

    namespace Persistence {
        class DatabaseConnection {
            -String url
            -String username
            +getConnection() Connection
        }
        class VehicleConnectionFactory {
            +createConnection() Connection$
        }
        class JdbcUtils {
            +close(AutoCloseable resource) void$
        }
        class DataAccessException {
            +DataAccessException(String message)
        }
        class CrudDAO~T, ID~ {
            <<interface>>
            +findById(ID id) T
            +findAll() List~T~
            +save(T entity) void
            +update(T entity) void
            +delete(ID id) void
        }
        class IPeajeDAO {
            <<interface>>
            +findByRutaId(int rutaId) List~Peaje~
        }
        class PeajeDAOImpl {
            -DatabaseConnection dbConnection
            +PeajeDAOImpl(DatabaseConnection dbConnection)
            +findById(Integer id) Peaje
            +findAll() List~Peaje~
            +save(Peaje entity) void
            +update(Peaje entity) void
            +delete(Integer id) void
            +findByRutaId(int rutaId) List~Peaje~
        }
        class ICasetaDAO {
            <<interface>>
        }
        class CasetaDAOImpl {
            +findById(Integer id) Caseta
            +findAll() List~Caseta~
            +save(Caseta entity) void
            +update(Caseta entity) void
            +delete(Integer id) void
        }
        class IConcesionarioDAO {
            <<interface>>
        }
        class ConcesionarioDAOImpl {
            +findById(Integer id) Concesionario
            +findAll() List~Concesionario~
            +save(Concesionario entity) void
            +update(Concesionario entity) void
            +delete(Integer id) void
        }
        class IDepartamentoDAO {
            <<interface>>
        }
        class DepartamentoDAOImpl {
            +findById(Integer id) Departamento
            +findAll() List~Departamento~
            +save(Departamento entity) void
            +update(Departamento entity) void
            +delete(Integer id) void
        }
        class IIntermediadorDAO {
            <<interface>>
        }
        class IntermediadorDAOImpl {
            +findById(Integer id) Intermediador
            +findAll() List~Intermediador~
            +save(Intermediador entity) void
            +update(Intermediador entity) void
            +delete(Integer id) void
        }
        class IRutaDAO {
            <<interface>>
        }
        class RutaDAOImpl {
            +findById(Integer id) Ruta
            +findAll() List~Ruta~
            +save(Ruta entity) void
            +update(Ruta entity) void
            +delete(Integer id) void
        }
        class ISectorDAO {
            <<interface>>
        }
        class SectorDAOImpl {
            +findById(Integer id) Sector
            +findAll() List~Sector~
            +save(Sector entity) void
            +update(Sector entity) void
            +delete(Integer id) void
        }
        class IVehicleCategoryDAO {
            <<interface>>
        }
        class VehicleCategoryDAOImpl {
            +findById(Integer id) VehicleCategory
            +findAll() List~VehicleCategory~
            +save(VehicleCategory entity) void
            +update(VehicleCategory entity) void
            +delete(Integer id) void
        }
        class IVehicleDAO {
            <<interface>>
        }
        class VehicleDAOImpl {
            +findById(Integer id) Vehicle
            +findAll() List~Vehicle~
            +save(Vehicle entity) void
            +update(Vehicle entity) void
            +delete(Integer id) void
        }
        class IAssistanceDAO {
            <<interface>>
        }
        class AssistanceDAOImpl {
            +findById(Integer id) Assistance
            +findAll() List~Assistance~
            +save(Assistance entity) void
            +update(Assistance entity) void
            +delete(Integer id) void
        }
        class IAssistanceHistoryDAO {
            <<interface>>
        }
        class AssistanceHistoryDAOImpl {
            +findById(Integer id) AssistanceHistory
            +findAll() List~AssistanceHistory~
            +save(AssistanceHistory entity) void
            +update(AssistanceHistory entity) void
            +delete(Integer id) void
        }
        class IAssistanceResourceDAO {
            <<interface>>
        }
        class AssistanceResourceDAOImpl {
            +findById(Integer id) AssistanceResource
            +findAll() List~AssistanceResource~
            +save(AssistanceResource entity) void
            +update(AssistanceResource entity) void
            +delete(Integer id) void
        }
        class TicketDao {
            +findAll() List~Ticket~
            +save(Ticket ticket) void
        }
        class TollRateDao {
            +findAll() List~TollRate~
            +save(TollRate rate) void
        }
        class TollUserDao {
            +findAll() List~TollUser~
            +save(TollUser user) void
        }
    }

    namespace Service {
        class ValidationException {
            +ValidationException(String message)
        }
        class PeajeService {
            <<Service>>
            -IPeajeDAO peajeDAO
            +PeajeService(IPeajeDAO peajeDAO)
            +registerPeaje(Peaje peaje) void
            +listAllPeajes() List~Peaje~
            +findPeajeById(int id) Peaje
        }
        class TicketService {
            <<Service>>
            -TicketDao ticketDao
            +TicketService(TicketDao ticketDao)
            +registerTicket(Ticket ticket) void
            +listAllTickets() List~Ticket~
        }
        class TollRatesService {
            <<Service>>
            -TollRateDao tollRateDao
            +TollRatesService(TollRateDao tollRateDao)
            +registerRate(TollRate rate) void
            +listAllRates() List~TollRate~
        }
        class VehicleService {
            <<Service>>
            -IVehicleDAO vehicleDAO
            +VehicleService(IVehicleDAO vehicleDAO)
            +registerVehicle(Vehicle vehicle) void
            +listAllVehicles() List~Vehicle~
        }
        class VehicleCategoryService {
            <<Service>>
            -IVehicleCategoryDAO vehicleCategoryDAO
            +VehicleCategoryService(IVehicleCategoryDAO vehicleCategoryDAO)
            +registerCategory(VehicleCategory category) void
            +listAllCategories() List~VehicleCategory~
        }
        class AssistanceService {
            <<Service>>
            -IAssistanceDAO assistanceDAO
            +AssistanceService(IAssistanceDAO assistanceDAO)
            +registerAssistance(Assistance assistance) void
            +listAllAssistances() List~Assistance~
        }
        class AssistanceResourceService {
            <<Service>>
            -IAssistanceResourceDAO assistanceResourceDAO
            +AssistanceResourceService(IAssistanceResourceDAO assistanceResourceDAO)
            +registerResource(AssistanceResource resource) void
            +listAllResources() List~AssistanceResource~
        }
    }

    namespace UI {
        class FuenteDatos {
            <<interface>>
            +getPeajes() List~Peaje~
        }
        class FuenteDatosOracle {
            -DatabaseConnection dbConnection
            +FuenteDatosOracle(DatabaseConnection dbConnection)
            +getPeajes() List~Peaje~
        }
        class Tema {
            +applyTheme() void
        }
        class BarraLateral {
            +initComponents() void
        }
        class PanelInicio {
            +refreshSummary() void
        }
        class PanelInventario {
            +loadInventory() void
        }
        class PanelDetallePeaje {
            +displayPeaje(Peaje peaje) void
        }
        class PanelEnConstruccion {
            +initComponents() void
        }
        class MapaPeajes {
            +renderMap(List~Peaje~ peajes) void
        }
        class DatosPeajes {
            +loadTableData() void
        }
        class VentanaPrincipal {
            <<UI>>
            -PeajeService peajeService
            -TicketService ticketService
            -TollRatesService tollRatesService
            -VehicleService vehicleService
            -AssistanceService assistanceService
            -FuenteDatos fuenteDatos
            +VentanaPrincipal(...)
            +start() void
        }
    }

    <<abstract>> Peaje

    Peaje <|-- PeajeElectronico
    Peaje <|-- PeajeManual
    Peaje "1" o-- "0..*" Caseta : lanes
    Peaje "1" --> "0..*" Ticket : issues
    Peaje "1" --> "0..*" TollRate : defines
    Ruta "1" --> "0..*" Peaje : peajes
    Sector "1" --> "0..*" Ruta : rutas
    Departamento "1" --> "0..*" Municipio : municipios
    Concesionario "1" --> "0..*" Peaje : operates
    Intermediador "1" --> "0..*" Ticket : processes
    TollUser "1" --> "0..*" Ticket : user
    Vehicle "0..*" --> "1" VehicleCategory : category
    Vehicle "1" --> "0..*" Ticket : vehicle
    TollRate "0..*" --> "1" VehicleCategory : category
    Assistance "1" *-- "0..*" AssistanceHistory : history
    Assistance "1" o-- "0..*" AssistanceResource : resources
    Assistance "0..*" --> "1" AssistanceStatus : status
    Assistance "0..*" --> "1" AssistanceType : type
    AssistanceResource "0..*" --> "1" ResourceAvailability : availability

    CrudDAO <|-- IPeajeDAO
    CrudDAO <|-- ICasetaDAO
    CrudDAO <|-- IConcesionarioDAO
    CrudDAO <|-- IDepartamentoDAO
    CrudDAO <|-- IIntermediadorDAO
    CrudDAO <|-- IRutaDAO
    CrudDAO <|-- ISectorDAO
    CrudDAO <|-- IVehicleCategoryDAO
    CrudDAO <|-- IVehicleDAO
    CrudDAO <|-- IAssistanceDAO
    CrudDAO <|-- IAssistanceHistoryDAO
    CrudDAO <|-- IAssistanceResourceDAO

    IPeajeDAO <|.. PeajeDAOImpl
    ICasetaDAO <|.. CasetaDAOImpl
    IConcesionarioDAO <|.. ConcesionarioDAOImpl
    IDepartamentoDAO <|.. DepartamentoDAOImpl
    IIntermediadorDAO <|.. IntermediadorDAOImpl
    IRutaDAO <|.. RutaDAOImpl
    ISectorDAO <|.. SectorDAOImpl
    IVehicleCategoryDAO <|.. VehicleCategoryDAOImpl
    IVehicleDAO <|.. VehicleDAOImpl
    IAssistanceDAO <|.. AssistanceDAOImpl
    IAssistanceHistoryDAO <|.. AssistanceHistoryDAOImpl
    IAssistanceResourceDAO <|.. AssistanceResourceDAOImpl

    PeajeDAOImpl ..> Peaje : uses
    CasetaDAOImpl ..> Caseta : uses
    ConcesionarioDAOImpl ..> Concesionario : uses
    DepartamentoDAOImpl ..> Departamento : uses
    IntermediadorDAOImpl ..> Intermediador : uses
    RutaDAOImpl ..> Ruta : uses
    SectorDAOImpl ..> Sector : uses
    VehicleCategoryDAOImpl ..> VehicleCategory : uses
    VehicleDAOImpl ..> Vehicle : uses
    AssistanceDAOImpl ..> Assistance : uses
    AssistanceHistoryDAOImpl ..> AssistanceHistory : uses
    AssistanceResourceDAOImpl ..> AssistanceResource : uses
    TicketDao ..> Ticket : uses
    TollRateDao ..> TollRate : uses
    TollUserDao ..> TollUser : uses

    PeajeService --> IPeajeDAO : uses
    TicketService --> TicketDao : uses
    TollRatesService --> TollRateDao : uses
    VehicleService --> IVehicleDAO : uses
    VehicleCategoryService --> IVehicleCategoryDAO : uses
    AssistanceService --> IAssistanceDAO : uses
    AssistanceResourceService --> IAssistanceResourceDAO : uses

    FuenteDatos <|.. FuenteDatosOracle
    FuenteDatosOracle --> DatabaseConnection : uses
    VentanaPrincipal --> PeajeService : uses
    VentanaPrincipal --> TicketService : uses
    VentanaPrincipal --> TollRatesService : uses
    VentanaPrincipal --> VehicleService : uses
    VentanaPrincipal --> AssistanceService : uses
    VentanaPrincipal --> FuenteDatos : uses
    VentanaPrincipal --> BarraLateral : uses
    VentanaPrincipal --> Tema : uses
    VentanaPrincipal --> PanelInicio : uses
    VentanaPrincipal --> PanelInventario : uses
    VentanaPrincipal --> PanelDetallePeaje : uses
    VentanaPrincipal --> PanelEnConstruccion : uses
    VentanaPrincipal --> MapaPeajes : uses
    VentanaPrincipal --> DatosPeajes : uses