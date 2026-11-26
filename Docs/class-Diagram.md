# Full Class Diagram — INVPeajes

```mermaid
classDiagram
    direction TB

    namespace model {
        class Peaje {
            -int id
            -String name
            -String location
            +getId() int
            +getName() String
            +getLocation() String
        }

        class PeajeElectronico {
            -String tagReaderProtocol
            +getTagReaderProtocol() String
        }

        class PeajeManual {
            -boolean cashOnly
            +isCashOnly() boolean
        }

        class Caseta {
            -int id
            -int laneNumber
            +getId() int
            +getLaneNumber() int
        }

        class Ticket {
            -int id
            -LocalDateTime issuedAt
            -double amount
            +getId() int
            +getIssuedAt() LocalDateTime
            +getAmount() double
        }

        class TollRate {
            -int id
            -double rateAmount
            +getId() int
            +getRateAmount() double
        }

        class TollUser {
            -int id
            -String name
            -String document
            +getId() int
            +getName() String
            +getDocument() String
        }

        class Vehicle {
            -int id
            -String plate
            +getId() int
            +getPlate() String
        }

        class VehicleCategory {
            -int id
            -String code
            -String description
            +getId() int
            +getCode() String
            +getDescription() String
        }

        class Assistance {
            -int id
            -LocalDateTime requestedAt
            +getId() int
            +getRequestedAt() LocalDateTime
        }

        class AssistanceHistory {
            -int id
            -String notes
            -LocalDateTime updatedAt
            +getId() int
            +getNotes() String
            +getUpdatedAt() LocalDateTime
        }

        class AssistanceResource {
            -int id
            -String name
            +getId() int
            +getName() String
        }

        class AssistanceStatus {
            -int id
            -String statusName
            +getId() int
            +getStatusName() String
        }

        class AssistanceType {
            -int id
            -String typeName
            +getId() int
            +getTypeName() String
        }

        class ResourceAvailability {
            -int id
            -boolean available
            +isAvailable() boolean
        }

        class Ruta {
            -int id
            -String code
            -String name
            +getId() int
            +getCode() String
            +getName() String
        }

        class Sector {
            -int id
            -String name
            +getId() int
            +getName() String
        }

        class Departamento {
            -int id
            -String name
            +getId() int
            +getName() String
        }

        class Municipio {
            -int id
            -String name
            +getId() int
            +getName() String
        }

        class Concesionario {
            -int id
            -String name
            +getId() int
            +getName() String
        }

        class Intermediador {
            -int id
            -String name
            +getId() int
            +getName() String
        }
    }

    namespace persistence {
        class DatabaseConnection {
            -Connection connection
            +getConnection() Connection
        }

        class VehicleConnectionFactory {
            +createConnection() Connection$
        }

        class JdbcUtils {
            +closeQuietly(AutoCloseable resource) void$
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
        }

        class PeajeDAOImpl {
            +findById(Integer id) Peaje
            +findAll() List~Peaje~
            +save(Peaje entity) void
            +update(Peaje entity) void
            +delete(Integer id) void
        }

        class ICasetaDAO {
            <<interface>>
        }

        class CasetaDAOImpl {
            +findById(Integer id) Caseta
            +findAll() List~Caseta~
        }

        class IConcesionarioDAO {
            <<interface>>
        }

        class ConcesionarioDAOImpl {
            +findById(Integer id) Concesionario
            +findAll() List~Concesionario~
        }

        class IDepartamentoDAO {
            <<interface>>
        }

        class DepartamentoDAOImpl {
            +findById(Integer id) Departamento
            +findAll() List~Departamento~
        }

        class IIntermediadorDAO {
            <<interface>>
        }

        class IntermediadorDAOImpl {
            +findById(Integer id) Intermediador
            +findAll() List~Intermediador~
        }

        class IRutaDAO {
            <<interface>>
        }

        class RutaDAOImpl {
            +findById(Integer id) Ruta
            +findAll() List~Ruta~
        }

        class ISectorDAO {
            <<interface>>
        }

        class SectorDAOImpl {
            +findById(Integer id) Sector
            +findAll() List~Sector~
        }

        class IVehicleCategoryDAO {
            <<interface>>
        }

        class VehicleCategoryDAOImpl {
            +findById(Integer id) VehicleCategory
            +findAll() List~VehicleCategory~
        }

        class IVehicleDAO {
            <<interface>>
        }

        class VehicleDAOImpl {
            +findById(Integer id) Vehicle
            +findAll() List~Vehicle~
        }

        class IAssistanceDAO {
            <<interface>>
        }

        class AssistanceDAOImpl {
            +findById(Integer id) Assistance
            +findAll() List~Assistance~
        }

        class IAssistanceHistoryDAO {
            <<interface>>
        }

        class AssistanceHistoryDAOImpl {
            +findById(Integer id) AssistanceHistory
            +findAll() List~AssistanceHistory~
        }

        class IAssistanceResourceDAO {
            <<interface>>
        }

        class AssistanceResourceDAOImpl {
            +findById(Integer id) AssistanceResource
            +findAll() List~AssistanceResource~
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

    namespace service {
        class ValidationException {
            +ValidationException(String message)
        }

        class PeajeService {
            -IPeajeDAO peajeDAO
            +getAllPeajes() List~Peaje~
            +registerPeaje(Peaje peaje) void
        }

        class TicketService {
            -TicketDao ticketDao
            +issueTicket(Ticket ticket) Ticket
            +getAllTickets() List~Ticket~
        }

        class TollRatesService {
            -TollRateDao tollRateDao
            +getAllRates() List~TollRate~
        }

        class VehicleService {
            -IVehicleDAO vehicleDAO
            +getAllVehicles() List~Vehicle~
        }

        class VehicleCategoryService {
            -IVehicleCategoryDAO categoryDAO
            +getAllCategories() List~VehicleCategory~
        }

        class AssistanceService {
            -IAssistanceDAO assistanceDAO
            +getAllAssistances() List~Assistance~
        }

        class AssistanceResourceService {
            -IAssistanceResourceDAO resourceDAO
            +getAllResources() List~AssistanceResource~
        }
    }

    namespace ui {
        class FuenteDatos {
            <<interface>>
            +loadPeajes() List~Peaje~
        }

        class FuenteDatosOracle {
            +loadPeajes() List~Peaje~
        }

        class Tema {
            +applyTheme() void
        }

        class BarraLateral {
            +render() void
        }

        class PanelInicio {
            +render() void
        }

        class PanelInventario {
            +render() void
        }

        class PanelDetallePeaje {
            +showPeaje(Peaje peaje) void
        }

        class PanelEnConstruccion {
            +render() void
        }

        class MapaPeajes {
            +renderMap() void
        }

        class DatosPeajes {
            +loadData() void
        }

        class VentanaPrincipal {
            -PeajeService peajeService
            -TicketService ticketService
            -TollRatesService tollRatesService
            -VehicleService vehicleService
            -AssistanceService assistanceService
            -FuenteDatos fuenteDatos
            +start() void
        }
    }

    <<abstract>> Peaje

    Peaje <|-- PeajeElectronico
    Peaje <|-- PeajeManual
    Peaje "1" --> "0..*" Caseta : has
    Peaje "1" --> "0..*" Ticket : issues
    Peaje "1" --> "0..*" TollRate : defines
    Ruta "1" --> "0..*" Peaje : includes
    Sector "1" --> "0..*" Ruta : contains
    Departamento "1" --> "0..*" Municipio : contains
    Concesionario "1" --> "0..*" Peaje : operates
    Intermediador "1" --> "0..*" Ticket : processes
    TollUser "1" --> "0..*" Ticket : holds
    Vehicle "0..*" --> "1" VehicleCategory : belongsTo
    Vehicle "1" --> "0..*" Ticket : generates
    TollRate "0..*" --> "1" VehicleCategory : appliesTo
    Assistance "1" --> "0..*" AssistanceHistory : logs
    Assistance "0..*" --> "0..*" AssistanceResource : uses
    Assistance "0..*" --> "1" AssistanceStatus : hasStatus
    Assistance "0..*" --> "1" AssistanceType : hasType
    AssistanceResource "0..*" --> "1" ResourceAvailability : hasAvailability

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

    PeajeDAOImpl ..> Peaje
    CasetaDAOImpl ..> Caseta
    ConcesionarioDAOImpl ..> Concesionario
    DepartamentoDAOImpl ..> Departamento
    IntermediadorDAOImpl ..> Intermediador
    RutaDAOImpl ..> Ruta
    SectorDAOImpl ..> Sector
    VehicleCategoryDAOImpl ..> VehicleCategory
    VehicleDAOImpl ..> Vehicle
    AssistanceDAOImpl ..> Assistance
    AssistanceHistoryDAOImpl ..> AssistanceHistory
    AssistanceResourceDAOImpl ..> AssistanceResource
    TicketDao ..> Ticket
    TollRateDao ..> TollRate
    TollUserDao ..> TollUser
    DatabaseConnection ..> DataAccessException
    VehicleConnectionFactory ..> DataAccessException
    JdbcUtils ..> DataAccessException

    PeajeService ..> IPeajeDAO
    PeajeService ..> Peaje
    PeajeService ..> ValidationException
    TicketService ..> TicketDao
    TicketService ..> Ticket
    TicketService ..> ValidationException
    TollRatesService ..> TollRateDao
    TollRatesService ..> TollRate
    VehicleService ..> IVehicleDAO
    VehicleService ..> Vehicle
    VehicleCategoryService ..> IVehicleCategoryDAO
    VehicleCategoryService ..> VehicleCategory
    AssistanceService ..> IAssistanceDAO
    AssistanceService ..> Assistance
    AssistanceResourceService ..> IAssistanceResourceDAO
    AssistanceResourceService ..> AssistanceResource

    FuenteDatos <|.. FuenteDatosOracle
    FuenteDatosOracle ..> DatabaseConnection
    VentanaPrincipal --> BarraLateral
    VentanaPrincipal --> Tema
    VentanaPrincipal --> PanelInicio
    VentanaPrincipal --> PanelInventario
    VentanaPrincipal --> PanelDetallePeaje
    VentanaPrincipal --> PanelEnConstruccion
    VentanaPrincipal --> MapaPeajes
    VentanaPrincipal --> DatosPeajes
    VentanaPrincipal --> FuenteDatos
    VentanaPrincipal --> PeajeService
    VentanaPrincipal --> TicketService
    VentanaPrincipal --> TollRatesService
    VentanaPrincipal --> VehicleService
    VentanaPrincipal --> AssistanceService
```[cite: 2, 3, 4]